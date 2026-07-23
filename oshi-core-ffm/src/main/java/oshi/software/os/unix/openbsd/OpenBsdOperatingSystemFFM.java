/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.openbsd;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.ForeignFunctions;
import oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions;
import oshi.ffm.util.platform.unix.openbsd.OpenBsdSysctlUtilFFM;
import oshi.software.common.os.unix.bsd.BsdPsKeyword;
import oshi.software.common.os.unix.openbsd.OpenBsdOperatingSystem;
import oshi.software.os.FileSystem;
import oshi.software.os.OSProcess;
import oshi.util.LogLevel;

/**
 * FFM-backed OpenBSD operating system.
 */
@ThreadSafe
public class OpenBsdOperatingSystemFFM extends OpenBsdOperatingSystem {

    private static final Logger LOG = LoggerFactory.getLogger(OpenBsdOperatingSystemFFM.class);

    @Override
    protected String querySysctl(int[] mib, String def) {
        return OpenBsdSysctlUtilFFM.sysctl(mib, def);
    }

    @Override
    public FileSystem getFileSystem() {
        return new OpenBsdFileSystemFFM();
    }

    @Override
    public int getProcessId() {
        return ForeignFunctions.callInArenaIntOrDefault(arena -> OpenBsdLibcFunctions.getpid(), LOG, LogLevel.WARN,
                "getpid failed", 0);
    }

    @Override
    public int getThreadId() {
        return ForeignFunctions.callInArenaIntOrDefault(arena -> OpenBsdLibcFunctions.getthrid(), LOG, LogLevel.WARN,
                "getthrid failed", 0);
    }

    @Override
    protected OSProcess createProcess(int pid, Map<BsdPsKeyword, String> psMap) {
        return new OpenBsdOSProcessFFM(pid, psMap, this);
    }
}
