/*
 * Copyright 2017-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.linux;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

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

    /** The resolv.conf systemd-resolved writes with the upstream servers it forwards to. */
    private static final String RESOLVED_UPSTREAM_CONF = "/run/systemd/resolve/resolv.conf";
    /** The addresses of systemd-resolved's stub listeners. */
    private static final Set<String> RESOLVED_STUBS = new HashSet<>(Arrays.asList("127.0.0.53", "127.0.0.54"));

    /**
     * Default constructor.
     */
    protected LinuxNetworkParams() {
    }

    /**
     * {@inheritDoc}
     * <p>
     * Where {@code /etc/resolv.conf} points only at the local systemd-resolved stub, returns the upstream servers
     * resolved forwards to instead, from the file it maintains for that purpose.
     */
    @Override
    public String[] getDnsServers() {
        return getDnsServers(FileUtil.readFile("/etc/resolv.conf"),
                () -> FileUtil.readFile(RESOLVED_UPSTREAM_CONF, false));
    }

    /**
     * Chooses the name servers from {@code /etc/resolv.conf}, or from resolved's upstream list when the former names
     * only its stub listener. Package-private for testing.
     *
     * @param resolv   the lines of {@code /etc/resolv.conf}
     * @param upstream supplies the lines of {@code /run/systemd/resolve/resolv.conf}, read only when needed
     * @return the name server addresses
     */
    static String[] getDnsServers(List<String> resolv, Supplier<List<String>> upstream) {
        String[] servers = parseDnsServers(resolv);
        if (servers.length > 0 && Arrays.stream(servers).allMatch(RESOLVED_STUBS::contains)) {
            // resolved forwards to every server it lists, not just the three the libc resolver would read
            String[] upstreamServers = parseDnsServers(upstream.get(), Integer.MAX_VALUE);
            if (upstreamServers.length > 0) {
                return upstreamServers;
            }
        }
        return servers;
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
