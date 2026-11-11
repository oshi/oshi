/*
 * Copyright 2017-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.windows;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractNetworkParams;
import oshi.software.os.NetworkParams;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Common Windows NetworkParams logic shared between the JNA and FFM implementations. The default-gateway lookups parse
 * {@code route print} output and live here, as does all of the routing table logic; domain/host name, DNS-server
 * resolution and the IP Helper API routing table read are native and provided by the subclasses.
 */
@ThreadSafe
public abstract class WindowsNetworkParams extends AbstractNetworkParams {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsNetworkParams.class);

    // Microsoft's recommended starting size for GetAdaptersAddresses, which avoids a second call on most systems
    private static final int ADAPTER_BUFFER_SIZE = 15_000;
    private static final int MAX_ADAPTER_ATTEMPTS = 3;
    private static final int ERROR_SUCCESS = 0;
    private static final int ERROR_BUFFER_OVERFLOW = 111;
    private static final int ERROR_NO_DATA = 232;

    /** Default constructor. */
    protected WindowsNetworkParams() {
    }

    /**
     * One row of the routing table as read from the IP Helper API by a backend.
     * <p>
     * This is a plain carrier so that the interpretation of the rows can live here rather than being written twice,
     * once against JNA structures and once against FFM memory segments.
     */
    public static final class RouteRow {
        /** The destination prefix address bytes, four for IPv4 or sixteen for IPv6. */
        public byte[] destination = new byte[0];
        /** The destination prefix length. */
        public int prefixLength = -1;
        /** The next hop address bytes, all zero for a directly attached route. */
        public byte[] nextHop = new byte[0];
        /** The index of the outgoing interface. */
        public int interfaceIndex = -1;
        /** The route metric. */
        public long metric = -1L;
    }

    /**
     * Queries the routing table from the native IP Helper API.
     *
     * @return the rows, or an empty list if the query failed
     */
    protected abstract List<RouteRow> queryRouteRows();

    /**
     * Makes one {@code GetAdaptersAddresses} call with a buffer of the given size and, if it succeeds, collects the DNS
     * server addresses of every adapter that is up.
     *
     * @param bufferSize   the size of the buffer to allocate and pass
     * @param requiredSize receives, in its only element, the size the call reports when the buffer was too small
     * @param addresses    receives the address bytes in network order, four for IPv4 or sixteen for IPv6
     * @return the call's return code, or -1 if the call itself could not be made
     */
    protected abstract int queryAdapterDnsServers(int bufferSize, int[] requiredSize, List<byte[]> addresses);

    /**
     * Queries the DNS server addresses of every adapter that is up, growing the buffer when the adapter list outgrows
     * it between the call that reports the size and the one that fills it.
     *
     * @return the address bytes in network order, or an empty list if the query failed
     */
    List<byte[]> queryDnsServerAddresses() {
        int size = ADAPTER_BUFFER_SIZE;
        int[] requiredSize = new int[1];
        for (int attempt = 0; attempt < MAX_ADAPTER_ATTEMPTS; attempt++) {
            List<byte[]> addresses = new ArrayList<>();
            int ret = queryAdapterDnsServers(size, requiredSize, addresses);
            if (ret == ERROR_BUFFER_OVERFLOW) {
                size = requiredSize[0];
                continue;
            }
            if (ret == ERROR_SUCCESS) {
                return addresses;
            }
            if (ret != ERROR_NO_DATA) {
                LOG.error("Failed to get adapter addresses. Error code: {}", ret);
            }
            return new ArrayList<>();
        }
        LOG.warn("The adapter list outgrew the buffer {} times in a row.", MAX_ADAPTER_ATTEMPTS);
        return new ArrayList<>();
    }

    @Override
    public String[] getDnsServers() {
        Set<String> servers = new LinkedHashSet<>();
        for (byte[] address : queryDnsServerAddresses()) {
            if ((address.length == 4 || address.length == 16) && !isUnconfiguredIpv6Dns(address)) {
                servers.add(ParseUtil.formatIpAddress(address));
            }
        }
        return servers.toArray(new String[0]);
    }

    /**
     * Whether an address is one of the deprecated site-local DNS addresses {@code fec0:0:0:ffff::1} to {@code ::3},
     * which Windows reports for an adapter with no IPv6 DNS server configured.
     *
     * @param address the address bytes
     * @return true for one of the three placeholder addresses
     */
    static boolean isUnconfiguredIpv6Dns(byte[] address) {
        if (address.length != 16 || address[15] < 1 || address[15] > 3) {
            return false;
        }
        byte[] prefix = { (byte) 0xfe, (byte) 0xc0, 0, 0, 0, 0, (byte) 0xff, (byte) 0xff };
        for (int i = 0; i < 15; i++) {
            if (address[i] != (i < prefix.length ? prefix[i] : 0)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public List<NetworkParams.IPRoute> getRoutes() {
        List<RouteRow> rows = queryRouteRows();
        List<NetworkParams.IPRoute> routes = new ArrayList<>(rows.size());
        if (rows.isEmpty()) {
            // Skip the interface enumeration, which is the expensive part, when there is nothing to name
            return routes;
        }
        // Windows reports only an interface index, so the name has to be looked up. Do it once for the whole table.
        Map<Integer, String> namesByIndex = queryInterfaceNameByIndex();
        for (RouteRow row : rows) {
            int addressBits = row.destination.length * 8;
            if (addressBits != 32 && addressBits != 128) {
                // A family we did not recognize. Dropping the row keeps the promise that a destination's length
                // always identifies the address family.
                continue;
            }
            // Windows publishes no flags, so a gateway route is one whose next hop is not the unspecified address.
            boolean isGateway = !isUnspecified(row.nextHop);
            routes.add(
                    new NetworkParams.IPRoute(row.destination, row.prefixLength, isGateway ? row.nextHop : new byte[0],
                            ParseUtil.getStringValueOrEmpty(namesByIndex.get(row.interfaceIndex)), row.interfaceIndex,
                            row.metric, isGateway, row.prefixLength == addressBits));
        }
        return routes;
    }

    private static boolean isUnspecified(byte[] address) {
        for (byte b : address) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String getIpv4DefaultGateway() {
        return parseIpv4Route();
    }

    @Override
    public String getIpv6DefaultGateway() {
        return parseIpv6Route();
    }

    private static String parseIpv4Route() {
        List<String> lines = ExecutingCommand.runNative("route print -4 0.0.0.0");
        for (String line : lines) {
            String[] fields = ParseUtil.whitespaces.split(line.trim(), -1);
            if (fields.length > 2 && "0.0.0.0".equals(fields[0])) {
                return fields[2];
            }
        }
        return "";
    }

    private static String parseIpv6Route() {
        List<String> lines = ExecutingCommand.runNative("route print -6 ::/0");
        for (String line : lines) {
            String[] fields = ParseUtil.whitespaces.split(line.trim(), -1);
            if (fields.length > 3 && "::/0".equals(fields[2])) {
                return fields[3];
            }
        }
        return "";
    }
}
