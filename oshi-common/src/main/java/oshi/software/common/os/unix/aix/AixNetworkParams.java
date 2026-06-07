/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.aix;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractNetworkParams;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Abstract base for AIX NetworkParams. {@code netstat -rnf inet[6]} parsing is shared; only {@link #getHostName()}
 * differs between JNA (gethostname via JNA) and FFM (gethostname via FFM).
 */
@ThreadSafe
public abstract class AixNetworkParams extends AbstractNetworkParams {

    /** AIX hostname maximum length, including the trailing NUL. {@code HOST_NAME_MAX = 255}. */
    protected static final int HOST_NAME_BUF_SIZE = 256;

    @Override
    public String getIpv4DefaultGateway() {
        return getDefaultGateway("netstat -rnf inet");
    }

    @Override
    public String getIpv6DefaultGateway() {
        return getDefaultGateway("netstat -rnf inet6");
    }

    private static String getDefaultGateway(String netstat) {
        for (String line : ExecutingCommand.runNative(netstat)) {
            String[] split = ParseUtil.whitespaces.split(line);
            if (split.length > 7 && "default".equals(split[0])) {
                return split[1];
            }
        }
        return Constants.UNKNOWN;
    }
}
