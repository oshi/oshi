/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.aix;

import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractNetworkParams;
import oshi.software.os.NetworkParams;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.driver.unix.NetstatRoute;

/**
 * Abstract base for AIX NetworkParams. {@code netstat -rnf inet[6]} parsing is shared; only {@link #getHostName()}
 * differs between JNA (gethostname via JNA) and FFM (gethostname via FFM).
 */
@ThreadSafe
public abstract class AixNetworkParams extends AbstractNetworkParams {

    /** AIX hostname maximum length, including the trailing NUL. {@code HOST_NAME_MAX = 255}. */
    protected static final int HOST_NAME_BUF_SIZE = 256;

    /** AIX prints the interface in the sixth column: Destination Gateway Flags Refs Use If Exp Groups. */
    private static final int IF_NAME_INDEX = 5;

    @Override
    public String getIpv4DefaultGateway() {
        return getDefaultGateway("netstat -rnf inet");
    }

    @Override
    public String getIpv6DefaultGateway() {
        return getDefaultGateway("netstat -rnf inet6");
    }

    private static String getDefaultGateway(String netstat) {
        return parseDefaultGateway(ExecutingCommand.runNative(netstat));
    }

    /**
     * Parses {@code netstat -rnf inet[6]} output to find the gateway of the {@code default} route.
     *
     * @param netstat the lines of {@code netstat -rnf inet[6]} output
     * @return the default gateway address, or an empty string if no default route is present
     */
    static String parseDefaultGateway(List<String> netstat) {
        for (String line : netstat) {
            String[] split = ParseUtil.whitespaces.split(line, -1);
            if (split.length > 7 && "default".equals(split[0])) {
                return split[1];
            }
        }
        return "";
    }

    @Override
    public List<NetworkParams.IPRoute> getRoutes() {
        return NetstatRoute.queryRoutes("netstat -rnf inet", "netstat -rnf inet6", IF_NAME_INDEX,
                queryInterfaceIndexByName());
    }
}
