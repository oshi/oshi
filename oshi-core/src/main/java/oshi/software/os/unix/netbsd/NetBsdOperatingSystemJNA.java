/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.netbsd;

import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.platform.unix.NetBsdLibc;
import oshi.software.common.os.unix.bsd.BsdPsKeyword;
import oshi.software.common.os.unix.netbsd.NetBsdOperatingSystem;
import oshi.software.os.OSProcess;
import oshi.util.platform.unix.netbsd.NetBsdSysctlUtil;

/**
 * An OperatingSystem for NetBSD that uses JNA native calls for process/thread identity when the JNA native library is
 * available, and constructs JNA-capable {@link NetBsdOSProcessJNA} processes. Falls back to the command-line
 * {@link NetBsdOperatingSystem} implementation when JNA is unavailable.
 */
@ThreadSafe
public class NetBsdOperatingSystemJNA extends NetBsdOperatingSystem {

    @Override
    public int getProcessId() {
        if (!NetBsdSysctlUtil.JNA_AVAILABLE) {
            return super.getProcessId();
        }
        return NetBsdLibc.INSTANCE.getpid();
    }

    @Override
    public int getThreadId() {
        if (!NetBsdSysctlUtil.JNA_AVAILABLE) {
            return super.getThreadId();
        }
        return NetBsdLibc.INSTANCE._lwp_self();
    }

    @Override
    protected OSProcess createProcess(int pid, Map<BsdPsKeyword, String> psMap) {
        return new NetBsdOSProcessJNA(pid, psMap, this);
    }
}
