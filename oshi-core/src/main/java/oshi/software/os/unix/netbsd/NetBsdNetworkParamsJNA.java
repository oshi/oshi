/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.netbsd;

import com.sun.jna.Memory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.os.unix.netbsd.NetBsdNetworkParams;
import oshi.util.platform.unix.netbsd.NetBsdSysctlUtil;

/**
 * NetBSD network parameters, reading the routing table from the kernel where the base class runs a command.
 */
@ThreadSafe
public class NetBsdNetworkParamsJNA extends NetBsdNetworkParams {

    private static final int CTL_NET = 4;
    private static final int PF_ROUTE = 17;
    private static final int NET_RT_DUMP = 1;

    @Override
    protected byte[] queryRouteDump() {
        // Every native call on this platform has to ask first: the runner without java-jna installed still uses this
        // class, and reaching libc there fails while the library loads rather than by returning anything
        if (!NetBsdSysctlUtil.JNA_AVAILABLE) {
            return new byte[0];
        }
        int[] mib = { CTL_NET, PF_ROUTE, 0, 0, NET_RT_DUMP, 0 };
        try (Memory buf = NetBsdSysctlUtil.sysctl(mib)) {
            if (buf == null) {
                return new byte[0];
            }
            return buf.getByteArray(0, (int) buf.size());
        }
    }
}
