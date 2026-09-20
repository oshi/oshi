/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.netbsd;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.bsd.BsdRouteDumpFFM;
import oshi.ffm.util.platform.unix.netbsd.NetBsdSysctlUtilFFM;
import oshi.software.common.os.unix.netbsd.NetBsdNetworkParams;

/**
 * NetBSD network parameters, reading the routing table from the kernel where the base class runs a command.
 */
@ThreadSafe
public class NetBsdNetworkParamsFFM extends NetBsdNetworkParams {

    @Override
    protected byte[] queryRouteDump() {
        if (!NetBsdSysctlUtilFFM.FFM_AVAILABLE) {
            return super.queryRouteDump();
        }
        return BsdRouteDumpFFM.queryRouteDump(NetBsdSysctlUtilFFM::sysctl);
    }
}
