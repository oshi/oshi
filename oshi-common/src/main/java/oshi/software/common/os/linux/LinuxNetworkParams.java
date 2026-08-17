/*
 * Copyright 2017-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.linux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractNetworkParams;
import oshi.software.os.NetworkParams;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.driver.linux.proc.RouteTable;

/**
 * Linux network parameters. Provides default gateway implementations via command line. Subclasses provide
 * {@link #getDomainName()} and {@link #getHostName()} via native calls.
 */
@ThreadSafe
public abstract class LinuxNetworkParams extends AbstractNetworkParams {

    /**
     * Default constructor.
     */
    protected LinuxNetworkParams() {
    }

    private static final String IPV4_DEFAULT_DEST = "0.0.0.0"; // NOSONAR java:S1313 - the kernel route table's literal
                                                               // wildcard destination, not a configurable address
    private static final String IPV6_DEFAULT_DEST = "::/0";

    @Override
    public String getIpv4DefaultGateway() {
        List<String> routes = ExecutingCommand.runNative("route -A inet -n");
        if (routes.size() <= 2) {
            return "";
        }

        String gateway = "";
        int minMetric = Integer.MAX_VALUE;

        for (int i = 2; i < routes.size(); i++) {
            String[] fields = ParseUtil.whitespaces.split(routes.get(i), -1);
            if (fields.length > 4 && fields[0].equals(IPV4_DEFAULT_DEST)) {
                boolean isGateway = fields[3].indexOf('G') != -1;
                int metric = ParseUtil.parseIntOrDefault(fields[4], Integer.MAX_VALUE);
                if (isGateway && metric < minMetric) {
                    minMetric = metric;
                    gateway = fields[1];
                }
            }
        }
        return gateway;
    }

    @Override
    public String getIpv6DefaultGateway() {
        List<String> routes = ExecutingCommand.runNative("route -A inet6 -n");
        if (routes.size() <= 2) {
            return "";
        }

        String gateway = "";
        int minMetric = Integer.MAX_VALUE;

        for (int i = 2; i < routes.size(); i++) {
            String[] fields = ParseUtil.whitespaces.split(routes.get(i), -1);
            if (fields.length > 3 && fields[0].equals(IPV6_DEFAULT_DEST)) {
                boolean isGateway = fields[2].indexOf('G') != -1;
                int metric = ParseUtil.parseIntOrDefault(fields[3], Integer.MAX_VALUE);
                if (isGateway && metric < minMetric) {
                    minMetric = metric;
                    gateway = fields[1];
                }
            }
        }
        return gateway;
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
