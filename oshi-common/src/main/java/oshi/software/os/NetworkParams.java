/*
 * Copyright 2017-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import oshi.annotation.PublicApi;
import oshi.annotation.concurrent.Immutable;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.NetworkIF;

/**
 * Provides network parameters of the running operating system, including the hostname, domain name, DNS server
 * addresses, default gateways for IPv4 and IPv6, and the routing table.
 * <p>
 * The hostname ({@link #getHostName()}) is the local machine name. The domain name ({@link #getDomainName()}) is the
 * DNS domain suffix. DNS servers ({@link #getDnsServers()}) are the configured name resolution servers. The default
 * gateways ({@link #getIpv4DefaultGateway()}, {@link #getIpv6DefaultGateway()}) are the routing destinations for
 * {@code 0.0.0.0/0} and {@code ::/0} respectively, and return an empty string if not defined.
 * <p>
 * {@link #getRoutes()} returns the whole routing table those gateways are drawn from:
 *
 * <pre>{@code
 * NetworkParams params = new SystemInfo().getOperatingSystem().getNetworkParams();
 * for (NetworkParams.IPRoute route : params.getRoutes()) {
 *     if (route.getPrefixLength() == 0) {
 *         System.out.println("Default route via " + route.getInterfaceName());
 *     }
 * }
 * }</pre>
 *
 * @see IPRoute
 */
@PublicApi
@ThreadSafe
public interface NetworkParams {

    /**
     * Gets the HostName of the machine executing OSHI.
     *
     * @return the hostname
     */
    String getHostName();

    /**
     * Gets the Domain Name of the machine executing OSHI.
     *
     * @return the domain name
     */
    String getDomainName();

    /**
     * Gets the DNS Servers configured for this machine.
     *
     * @return the DNS servers
     */
    String[] getDnsServers();

    /**
     * Gets the default gateway(routing destination for 0.0.0.0/0) for IPv4 connections.
     *
     * @return default gateway for IPv4, or empty string if not defined.
     */
    String getIpv4DefaultGateway();

    /**
     * Gets default gateway(routing destination for ::/0) for IPv6 connections.
     *
     * @return default gateway for IPv6, or empty string if not defined.
     */
    String getIpv6DefaultGateway();

    /**
     * Gets the operating system's routing table, containing both IPv4 and IPv6 routes. Each {@link IPRoute} carries its
     * own address family in the length of its destination address.
     * <p>
     * The routing table is read fresh on each call and is not cached.
     * <p>
     * <b>Platform notes:</b> Windows reads the table from the IP Helper API and Linux from {@code /proc/net/route} and
     * {@code /proc/net/ipv6_route}; the remaining platforms parse {@code netstat}. No platform requires elevated
     * privileges. Not every platform publishes every field; see {@link IPRoute} for the sentinel each unavailable field
     * reports.
     *
     * @return A list of {@link IPRoute} objects, or an empty list if the routing table could not be read.
     */
    default List<IPRoute> getRoutes() {
        return Collections.emptyList();
    }

    /**
     * A single entry in the operating system's routing table.
     * <p>
     * The address family of the entry is given by the length of {@link #getDestination()}: four bytes for IPv4, sixteen
     * for IPv6. A route whose destination cannot be interpreted is omitted from {@link #getRoutes()}, so that length is
     * always 4 or 16.
     * <p>
     * <b>Platform notes:</b> {@link #getMetric()} is only reported on Linux, Windows and OpenBSD; macOS, AIX, Solaris,
     * FreeBSD, DragonFly BSD and NetBSD do not publish a route metric. {@link #getPrefixLength()} is {@code -1} on the
     * rare route whose netmask the operating system does not publish. {@link #getInterfaceIndex()} is {@code -1} when
     * the interface name cannot be resolved to an index.
     */
    @PublicApi
    @Immutable
    final class IPRoute {
        private final byte[] destination;
        private final int prefixLength;
        private final byte[] gateway;
        private final String interfaceName;
        private final int interfaceIndex;
        private final long metric;
        private final boolean isGateway;
        private final boolean isHost;

        /**
         * Constructs a new IPRoute instance.
         *
         * @param destination    the destination network address bytes, four for IPv4 or sixteen for IPv6
         * @param prefixLength   the number of leading bits in the destination network mask, or -1 if not published
         * @param gateway        the next hop address bytes, or an empty array for a directly attached route
         * @param interfaceName  the name of the outgoing interface, or an empty string if not published
         * @param interfaceIndex the index of the outgoing interface, or -1 if not published
         * @param metric         the route metric or priority, or -1 if not published
         * @param isGateway      whether the route forwards through the gateway rather than being directly attached
         * @param isHost         whether the route is to a single host rather than to a network
         */
        public IPRoute(byte[] destination, int prefixLength, byte[] gateway, String interfaceName, int interfaceIndex,
                long metric, boolean isGateway, boolean isHost) {
            this.destination = Arrays.copyOf(destination, destination.length);
            this.prefixLength = prefixLength;
            this.gateway = Arrays.copyOf(gateway, gateway.length);
            this.interfaceName = interfaceName;
            this.interfaceIndex = interfaceIndex;
            this.metric = metric;
            this.isGateway = isGateway;
            this.isHost = isHost;
        }

        /**
         * Gets the destination network address. For IPv4 routes this is a 4-byte array, for IPv6 routes a 16-byte
         * array, so the length identifies the address family. A default route has an all-zero destination and a
         * {@link #getPrefixLength()} of 0.
         *
         * @return The destination address, never empty.
         */
        public byte[] getDestination() {
            return Arrays.copyOf(destination, destination.length);
        }

        /**
         * Gets the number of leading bits in the destination network mask, as in CIDR notation.
         *
         * @return The prefix length, from 0 to 32 for IPv4 or 0 to 128 for IPv6, or -1 if the operating system does not
         *         publish the mask for this route.
         */
        public int getPrefixLength() {
            return prefixLength;
        }

        /**
         * Gets the next hop address that traffic matching this route is forwarded to. Only gateway routes have one. For
         * a directly attached route the operating system may report the outgoing interface's own address, its
         * link-layer address, or a symbolic name, none of which is a next hop, so all of them are reported as empty.
         *
         * @return The gateway address, 4 bytes for IPv4 or 16 for IPv6, or an empty array when {@link #isGateway()} is
         *         {@code false}.
         */
        public byte[] getGateway() {
            return Arrays.copyOf(gateway, gateway.length);
        }

        /**
         * Gets the name of the interface traffic matching this route leaves by.
         *
         * @return The interface name, or an empty string if the operating system does not publish one.
         */
        public String getInterfaceName() {
            return interfaceName;
        }

        /**
         * Gets the index of the interface traffic matching this route leaves by, matching {@link NetworkIF#getIndex()}.
         *
         * @return The interface index, or -1 if it could not be determined.
         */
        public int getInterfaceIndex() {
            return interfaceIndex;
        }

        /**
         * Gets the cost of this route, used to select between routes to the same destination. Lower is preferred.
         *
         * @return The metric, or -1 on the platforms which do not publish one: macOS, AIX, Solaris, FreeBSD, DragonFly
         *         BSD and NetBSD.
         */
        public long getMetric() {
            return metric;
        }

        /**
         * Tests whether traffic matching this route is forwarded through {@link #getGateway()} rather than delivered
         * directly on the attached link.
         *
         * @return {@code true} for a gateway route.
         */
        public boolean isGateway() {
            return isGateway;
        }

        /**
         * Tests whether this route is to a single host rather than to a network, that is, whether
         * {@link #getPrefixLength()} covers the whole address.
         *
         * @return {@code true} for a host route.
         */
        public boolean isHost() {
            return isHost;
        }

        @Override
        public String toString() {
            return "IPRoute [destination=" + addressToString(destination) + "/" + prefixLength + ", gateway="
                    + addressToString(gateway) + ", interfaceName=" + interfaceName + ", interfaceIndex="
                    + interfaceIndex + ", metric=" + metric + ", isGateway=" + isGateway + ", isHost=" + isHost + "]";
        }

        private static String addressToString(byte[] address) {
            if (address.length > 0) {
                try {
                    return InetAddress.getByAddress(address).getHostAddress();
                } catch (UnknownHostException e) { // NOSONAR java:S108
                    // Cannot happen for a length of 4 or 16; fall through to the placeholder
                }
            }
            return "*";
        }
    }
}
