/*
 * Copyright 2017-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.mac;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractNetworkParams;
import oshi.software.os.NetworkParams;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.driver.unix.NetstatRoute;

/**
 * Common Mac NetworkParams logic shared between the JNA and FFM implementations. The default-gateway lookups are
 * command-line based and live here; hostname/domain resolution is native and provided by the subclasses.
 */
@ThreadSafe
public abstract class MacNetworkParams extends AbstractNetworkParams {

    private static final String IPV6_ROUTE_HEADER = "Internet6:";

    private static final String DEFAULT_GATEWAY = "default";

    /** macOS prints the interface in the fourth column: Destination Gateway Flags Netif Expire. */
    private static final int IF_NAME_INDEX = 3;

    /** Default constructor. */
    protected MacNetworkParams() {
    }

    @Override
    public String getIpv4DefaultGateway() {
        return searchGateway(ExecutingCommand.runNative("route -n get default"));
    }

    @Override
    public String getIpv6DefaultGateway() {
        List<String> lines = ExecutingCommand.runNative("netstat -nr");
        boolean v6Table = false;
        for (String line : lines) {
            if (v6Table && line.startsWith(DEFAULT_GATEWAY)) {
                String[] fields = ParseUtil.whitespaces.split(line, -1);
                if (fields.length > 2 && fields[2].contains("G")) {
                    return fields[1].split("%", -1)[0];
                }
            } else if (line.startsWith(IPV6_ROUTE_HEADER)) {
                v6Table = true;
            }
        }
        return "";
    }

    @Override
    public List<NetworkParams.IPRoute> getRoutes() {
        // Selecting one family per invocation avoids having to detect the "Internet:"/"Internet6:" section banners
        // that the combined table separates its two halves with.
        Map<String, Integer> ifIndexByName = queryInterfaceIndexByName();
        List<NetworkParams.IPRoute> routes = new ArrayList<>(
                NetstatRoute.queryRoutes("netstat -rn -f inet", false, IF_NAME_INDEX, ifIndexByName));
        routes.addAll(NetstatRoute.queryRoutes("netstat -rn -f inet6", true, IF_NAME_INDEX, ifIndexByName));
        return routes;
    }
}
