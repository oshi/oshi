/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.openbsd;

import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.platform.unix.OpenBsdLibc;
import oshi.software.common.os.unix.bsd.BsdPsKeyword;
import oshi.software.common.os.unix.openbsd.OpenBsdOperatingSystem;
import oshi.software.os.FileSystem;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.util.platform.unix.openbsd.OpenBsdSysctlUtil;

/**
 * JNA-backed OpenBSD operating system.
 */
@ThreadSafe
public class OpenBsdOperatingSystemJNA extends OpenBsdOperatingSystem {

    @Override
    public NetworkParams getNetworkParams() {
        return new OpenBsdNetworkParamsJNA();
    }

    @Override
    protected String querySysctl(int[] mib, String def) {
        return OpenBsdSysctlUtil.sysctl(mib, def);
    }

    @Override
    public FileSystem getFileSystem() {
        return new OpenBsdFileSystemJNA();
    }

    @Override
    public int getProcessId() {
        return OpenBsdLibc.INSTANCE.getpid();
    }

    @Override
    public int getThreadId() {
        return OpenBsdLibc.INSTANCE.getthrid();
    }

    @Override
    protected OSProcess createProcess(int pid, Map<BsdPsKeyword, String> psMap) {
        return new OpenBsdOSProcessJNA(pid, psMap, this);
    }
}
