/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.openbsd;

import static oshi.jna.platform.unix.OpenBsdLibc.CTL_KERN;
import static oshi.jna.platform.unix.OpenBsdLibc.KERN_OSRELEASE;
import static oshi.jna.platform.unix.OpenBsdLibc.KERN_OSTYPE;
import static oshi.jna.platform.unix.OpenBsdLibc.KERN_VERSION;

import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.platform.unix.OpenBsdLibc;
import oshi.software.common.os.unix.bsd.BsdPsKeyword;
import oshi.software.common.os.unix.openbsd.OpenBsdOperatingSystem;
import oshi.software.os.FileSystem;
import oshi.software.os.OSProcess;
import oshi.util.platform.unix.openbsd.OpenBsdSysctlUtil;
import oshi.util.tuples.Pair;

/**
 * JNA-backed OpenBSD operating system.
 */
@ThreadSafe
public class OpenBsdOperatingSystemJNA extends OpenBsdOperatingSystem {

    @Override
    public Pair<String, OSVersionInfo> queryFamilyVersionInfo() {
        int[] mib = new int[2];
        mib[0] = CTL_KERN;
        mib[1] = KERN_OSTYPE;
        String family = OpenBsdSysctlUtil.sysctl(mib, "OpenBSD");
        mib[1] = KERN_OSRELEASE;
        String version = OpenBsdSysctlUtil.sysctl(mib, "");
        mib[1] = KERN_VERSION;
        String versionInfo = OpenBsdSysctlUtil.sysctl(mib, "");
        String buildNumber = versionInfo.split(":")[0].replace(family, "").replace(version, "").trim();

        return new Pair<>(family, new OSVersionInfo(version, null, buildNumber));
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
