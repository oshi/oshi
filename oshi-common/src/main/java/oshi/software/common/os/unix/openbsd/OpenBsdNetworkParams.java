/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.openbsd;

import java.util.List;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractNetworkParams;
import oshi.software.os.NetworkParams;
import oshi.util.ExecutingCommand;
import oshi.util.driver.unix.NetstatRoute;
import oshi.util.driver.unix.RouteTableDump;

/**
 * Abstract base for the OpenBSD NetworkParams. Holds the command-line gateway lookup and the host name fallback; the
 * JNA and FFM subclasses supply the {@code gethostname} binding.
 */
@ThreadSafe
public abstract class OpenBsdNetworkParams extends AbstractNetworkParams {

    /**
     * OpenBSD prints the interface in the eighth column: Destination Gateway Flags Refs Use Mtu Prio Iface. The header
     * scan corrects this if the layout differs on another release.
     */
    private static final int IF_NAME_INDEX = 7;

    @Override
    public String getHostName() {
        String hn = queryHostName();
        return hn != null ? hn : super.getHostName();
    }

    @Override
    public String getIpv4DefaultGateway() {
        return searchGateway(ExecutingCommand.runNative("route -n get default"));
    }

    @Override
    public String getIpv6DefaultGateway() {
        return searchGateway(ExecutingCommand.runNative("route -n get -inet6 default"));
    }

    @Override
    public List<NetworkParams.IPRoute> getRoutes() {
        byte[] dump = queryRouteDump();
        if (dump.length > 0) {
            List<IPRoute> routes = RouteTableDump.parse(dump, RouteTableDump.OPENBSD, queryInterfaceNameByIndex());
            if (!routes.isEmpty()) {
                return routes;
            }
        }
        return NetstatRoute.queryRoutes("netstat -rn -f inet", "netstat -rn -f inet6", IF_NAME_INDEX,
                queryInterfaceIndexByName());
    }

    /**
     * Reads the host name from libc, avoiding the name resolution that the {@link AbstractNetworkParams} fallback
     * performs.
     *
     * @return the native host name, or {@code null} to fall back to the InetAddress lookup
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
