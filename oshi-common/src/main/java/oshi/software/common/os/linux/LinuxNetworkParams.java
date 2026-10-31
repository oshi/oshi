/*
 * Copyright 2017-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.linux;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractNetworkParams;
import oshi.software.os.NetworkParams;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.driver.linux.proc.RouteTable;
import oshi.util.linux.ProcPath;

/**
 * Linux network parameters. Provides the host name and default gateway implementations shared by all backends. The JNA
 * and FFM subclasses override {@link #getDomainName()} with a native call; the native-free subclass inherits the
 * Java-based resolution from {@link AbstractNetworkParams}.
 */
@ThreadSafe
public abstract class LinuxNetworkParams extends AbstractNetworkParams {

    /**
     * Default constructor.
     */
    protected LinuxNetworkParams() {
    }

    @Override
    public String getHostName() {
        // The kernel.hostname sysctl is the same utsname.nodename that gethostname(2) returns, per UTS namespace, so
        // this stays correct inside a container. Reading it avoids the name resolution the AbstractNetworkParams
        // fallback performs, which truncates an FQDN at the first dot and reports "localhost" when the host name does
        // not resolve.
        String hostname = FileUtil.getStringFromFile(ProcPath.SYS_KERNEL_HOSTNAME);
        return hostname.isEmpty() ? super.getHostName() : hostname;
    }

    @Override
    public String getIpv4DefaultGateway() {
        return defaultGateway(RouteTable.queryIpv4Routes(Collections.emptyMap()));
    }

    @Override
    public String getIpv6DefaultGateway() {
        return defaultGateway(RouteTable.queryIpv6Routes(Collections.emptyMap()));
    }

    /**
     * Selects the default gateway from a routing table: the next hop of the default route with the lowest metric, the
     * one the kernel uses.
     *
     * @param routes the routes of one address family
     * @return the gateway address text, or an empty string if no default route has a gateway
     */
    static String defaultGateway(List<NetworkParams.IPRoute> routes) {
        NetworkParams.IPRoute best = null;
        for (NetworkParams.IPRoute route : routes) {
            if (route.getPrefixLength() == 0 && route.isGateway() && route.getGateway().length > 0
                    && (best == null || route.getMetric() < best.getMetric())) {
                best = route;
            }
        }
        return best == null ? "" : ParseUtil.formatIpAddress(best.getGateway());
    }

    @Override
    public List<NetworkParams.IPRoute> getRoutes() {
        // /proc and /sys are kernel interfaces rather than real disk, so reading them is cheaper than the process
        // spawn the route command costs, and is not affected by net-tools being absent.
        Map<String, Integer> ifIndexByName = queryInterfaceIndexByName();
        List<NetworkParams.IPRoute> routes = new ArrayList<>(RouteTable.queryIpv4Routes(ifIndexByName));
        routes.addAll(RouteTable.queryIpv6Routes(ifIndexByName));
        return routes;
    }
}
