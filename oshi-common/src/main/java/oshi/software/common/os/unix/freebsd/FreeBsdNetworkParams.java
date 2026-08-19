/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.freebsd;

import java.util.List;

import org.jspecify.annotations.Nullable;

import oshi.software.common.AbstractNetworkParams;
import oshi.software.os.NetworkParams;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.driver.unix.NetstatRoute;
import oshi.util.driver.unix.RouteTableDump;

/**
 * Abstract base for the FreeBSD NetworkParams. Resolves the host and domain names and the default gateways from command
 * output; the JNA and FFM subclasses supply the resolver calls.
 * <p>
 * DragonFly BSD uses this class too. Its {@code netstat} prints Refs and Use columns FreeBSD's does not, which the
 * routing table parser absorbs by reading the interface column from the header rather than a fixed index.
 */
public abstract class FreeBsdNetworkParams extends AbstractNetworkParams {

    /** FreeBSD prints the interface in the fourth column: Destination Gateway Flags Netif Expire. */
    private static final int IF_NAME_INDEX = 3;

    @Override
    public String getDomainName() {
        return ParseUtil.getStringValueOrEmpty(queryDomainName());
    }

    @Override
    public String getHostName() {
        String hn = queryHostName();
        return hn != null ? hn : super.getHostName();
    }

    @Override
    public String getIpv4DefaultGateway() {
        return searchGateway(ExecutingCommand.runNative("route -4 get default"));
    }

    @Override
    public String getIpv6DefaultGateway() {
        return searchGateway(ExecutingCommand.runNative("route -6 get default"));
    }

    @Override
    public List<NetworkParams.IPRoute> getRoutes() {
        byte[] dump = queryRouteDump();
        if (dump.length > 0) {
            List<IPRoute> routes = RouteTableDump.parse(dump, RouteTableDump.Layout.FREEBSD,
                    queryInterfaceNameByIndex());
            if (!routes.isEmpty()) {
                return routes;
            }
        }
        return NetstatRoute.queryRoutes("netstat -rn -f inet", "netstat -rn -f inet6", IF_NAME_INDEX,
                queryInterfaceIndexByName());
    }

    /**
     * Resolves the domain name via the subclass's getaddrinfo binding.
     *
     * @return the resolved canonical domain name, or {@code null} if the native call failed. {@link #getDomainName()}
     *         converts that to the empty string its own contract promises.
     */
    protected abstract @Nullable String queryDomainName();

    /**
     * Returns the hostname via the subclass's gethostname binding, or {@code null} to fall back to the InetAddress
     * lookup inherited from {@link AbstractNetworkParams#getHostName()}.
     *
     * @return the native hostname, or {@code null} to fall back to the InetAddress lookup
     */
    protected abstract @Nullable String queryHostName();

    /**
     * Fetches the kernel's routing table dump.
     *
     * @return The bytes of a {@code NET_RT_DUMP} sysctl, or an empty array if this build cannot make the call, in which
     *         case the routing table is read by running a command instead
     */
    protected byte[] queryRouteDump() {
        return new byte[0];
    }
}
