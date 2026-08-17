/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.openbsd;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractNetworkParams;
import oshi.software.os.NetworkParams;
import oshi.util.ExecutingCommand;
import oshi.util.driver.unix.NetstatRoute;

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
        Map<String, Integer> ifIndexByName = queryInterfaceIndexByName();
        List<NetworkParams.IPRoute> routes = new ArrayList<>(
                NetstatRoute.queryRoutes("netstat -rn -f inet", false, IF_NAME_INDEX, ifIndexByName));
        routes.addAll(NetstatRoute.queryRoutes("netstat -rn -f inet6", true, IF_NAME_INDEX, ifIndexByName));
        return routes;
    }

    /**
     * Reads the host name from libc, avoiding the name resolution that the {@link AbstractNetworkParams} fallback
     * performs.
     *
     * @return the native host name, or {@code null} to fall back to the InetAddress lookup
     */
    protected abstract @Nullable String queryHostName();
}
