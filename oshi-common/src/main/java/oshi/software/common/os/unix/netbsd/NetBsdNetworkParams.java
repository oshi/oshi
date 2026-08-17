/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.netbsd;

import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractNetworkParams;
import oshi.software.os.NetworkParams;
import oshi.util.ExecutingCommand;
import oshi.util.driver.unix.NetstatRoute;

/**
 * NetBsdNetworkParams class.
 */
@ThreadSafe
public class NetBsdNetworkParams extends AbstractNetworkParams {

    /** NetBSD prints the interface in the seventh column: Destination Gateway Flags Refs Use Mtu Interface. */
    private static final int IF_NAME_INDEX = 6;

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
        return NetstatRoute.queryRoutes("netstat -rn -f inet", "netstat -rn -f inet6", IF_NAME_INDEX,
                queryInterfaceIndexByName());
    }
}
