/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.netbsd;

import static oshi.util.LogLevel.WARN;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.ForeignFunctions;
import oshi.ffm.platform.unix.netbsd.NetBsdLibcFunctions;
import oshi.ffm.util.platform.unix.netbsd.NetBsdSysctlUtilFFM;
import oshi.software.common.os.unix.bsd.BsdPsKeyword;
import oshi.software.common.os.unix.netbsd.NetBsdOperatingSystem;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;

/**
 * An OperatingSystem for NetBSD that uses FFM native calls for process/thread identity when FFM native access is
 * available, and constructs FFM-capable {@link NetBsdOSProcessFFM} processes. Falls back to the command-line
 * {@link NetBsdOperatingSystem} implementation when it is not.
 */
@ThreadSafe
public class NetBsdOperatingSystemFFM extends NetBsdOperatingSystem {

    private static final Logger LOG = LoggerFactory.getLogger(NetBsdOperatingSystemFFM.class);

    @Override
    public int getProcessId() {
        if (!NetBsdSysctlUtilFFM.FFM_AVAILABLE) {
            return super.getProcessId();
        }
        return ForeignFunctions.callInArenaIntOrDefault(arena -> NetBsdLibcFunctions.getpid(), LOG, WARN,
                "Failed getpid", -1);
    }

    @Override
    public int getThreadId() {
        if (!NetBsdSysctlUtilFFM.FFM_AVAILABLE) {
            return super.getThreadId();
        }
        return ForeignFunctions.callInArenaIntOrDefault(arena -> NetBsdLibcFunctions.lwpSelf(), LOG, WARN,
                "Failed _lwp_self", -1);
    }

    @Override
    protected OSProcess createProcess(int pid, Map<BsdPsKeyword, String> psMap) {
        return new NetBsdOSProcessFFM(pid, psMap, this);
    }

    @Override
    public NetworkParams getNetworkParams() {
        return new NetBsdNetworkParamsFFM();
    }
}
