/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.netbsd;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.bsd.BsdRouteDumpJNA;
import oshi.jna.platform.unix.NetBsdLibc;
import oshi.software.common.os.unix.netbsd.NetBsdNetworkParams;
import oshi.util.platform.unix.netbsd.NetBsdSysctlUtil;

/**
 * NetBSD network parameters, reading the routing table from the kernel where the base class runs a command.
 */
@ThreadSafe
public class NetBsdNetworkParamsJNA extends NetBsdNetworkParams {

    @Override
    protected byte[] queryRouteDump() {
        // Every native call on this platform has to ask first: the runner without java-jna installed still uses this
        // class, and reaching libc there fails while the library loads rather than by returning anything
        if (!NetBsdSysctlUtil.JNA_AVAILABLE) {
            return super.queryRouteDump();
        }
        return BsdRouteDumpJNA.queryRouteDump(NetBsdLibc.INSTANCE);
    }
}
