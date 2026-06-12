/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.openbsd;

import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.CTL_KERN;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.KERN_OSRELEASE;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.KERN_OSTYPE;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.KERN_VERSION;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.ForeignFunctions;
import oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions;
import oshi.ffm.util.platform.unix.openbsd.OpenBsdSysctlUtilFFM;
import oshi.software.common.os.unix.bsd.BsdPsKeyword;
import oshi.software.common.os.unix.openbsd.OpenBsdOperatingSystem;
import oshi.software.os.FileSystem;
import oshi.software.os.OSProcess;
import oshi.util.tuples.Pair;

/**
 * FFM-backed OpenBSD operating system.
 */
@ThreadSafe
public class OpenBsdOperatingSystemFFM extends OpenBsdOperatingSystem {

    private static final Logger LOG = LoggerFactory.getLogger(OpenBsdOperatingSystemFFM.class);

    @Override
    public Pair<String, OSVersionInfo> queryFamilyVersionInfo() {
        int[] mibType = { CTL_KERN, KERN_OSTYPE };
        String family = OpenBsdSysctlUtilFFM.sysctl(mibType, "OpenBSD");

        int[] mibRelease = { CTL_KERN, KERN_OSRELEASE };
        String version = OpenBsdSysctlUtilFFM.sysctl(mibRelease, "");

        int[] mibVersion = { CTL_KERN, KERN_VERSION };
        String versionInfo = OpenBsdSysctlUtilFFM.sysctl(mibVersion, "");
        String buildNumber = versionInfo.split(":")[0].replace(family, "").replace(version, "").trim();

        return new Pair<>(family, new OSVersionInfo(version, null, buildNumber));
    }

    @Override
    public FileSystem getFileSystem() {
        return new OpenBsdFileSystemFFM();
    }

    @Override
    public int getProcessId() {
        return ForeignFunctions.callInArenaIntOrDefault(arena -> OpenBsdLibcFunctions.getpid(), LOG, Level.WARN,
                "getpid failed", 0);
    }

    @Override
    public int getThreadId() {
        return ForeignFunctions.callInArenaIntOrDefault(arena -> OpenBsdLibcFunctions.getthrid(), LOG, Level.WARN,
                "getthrid failed", 0);
    }

    @Override
    protected OSProcess createProcess(int pid, Map<BsdPsKeyword, String> psMap) {
        return new OpenBsdOSProcessFFM(pid, psMap, this);
    }
}
