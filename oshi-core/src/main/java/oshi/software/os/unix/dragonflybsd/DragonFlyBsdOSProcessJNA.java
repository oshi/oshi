/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.dragonflybsd;

import static oshi.jna.platform.unix.FreeBsdLibc.RLIMIT_NOFILE;

import java.util.Collections;
import java.util.Map;

import com.sun.jna.Memory;
import com.sun.jna.platform.unix.LibCAPI.size_t;
import com.sun.jna.platform.unix.Resource;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.ByRef.CloseableSizeTByReference;
import oshi.jna.platform.unix.DragonFlyBsdLibc;
import oshi.software.common.os.unix.bsd.BsdPsKeyword;
import oshi.software.common.os.unix.dragonflybsd.DragonFlyBsdOSProcess;

/**
 * OSProcess implementation
 */
@ThreadSafe
public class DragonFlyBsdOSProcessJNA extends DragonFlyBsdOSProcess {

    public DragonFlyBsdOSProcessJNA(int pid, Map<BsdPsKeyword, String> psMap, DragonFlyBsdOperatingSystemJNA os) {
        super(pid, os);
        updateAttributes(psMap);
    }

    @Override
    protected Map<String, String> queryEnvironmentVariables() {
        // DragonFlyBSD's /proc does not expose environ for other processes.
        // For the current process, use Java's System.getenv().
        if (getProcessID() == DragonFlyBsdLibc.INSTANCE.getpid()) {
            return System.getenv();
        }
        return Collections.emptyMap();
    }

    @Override
    protected long queryRlimitNofile(boolean soft) {
        Resource.Rlimit rlimit = new Resource.Rlimit();
        DragonFlyBsdLibc.INSTANCE.getrlimit(RLIMIT_NOFILE, rlimit);
        return soft ? rlimit.rlim_cur : rlimit.rlim_max;
    }

    @Override
    protected long queryAffinityMask() {
        try (Memory mask = new Memory(DragonFlyBsdLibc.CPU_SET_SIZE)) {
            mask.clear();
            if (DragonFlyBsdLibc.INSTANCE.sched_getaffinity(getProcessID(), new size_t(DragonFlyBsdLibc.CPU_SET_SIZE),
                    mask) == 0) {
                return mask.getLong(0);
            }
        }
        return 0L;
    }

    @Override
    protected String queryOwnExecutablePath() {
        int[] mib = { 1, 14, KERN_PROC_PATHNAME, getProcessID() }; // CTL_KERN, KERN_PROC
        try (Memory buf = new Memory(PATH_MAX);
                CloseableSizeTByReference size = new CloseableSizeTByReference(PATH_MAX)) {
            if (DragonFlyBsdLibc.INSTANCE.sysctl(mib, mib.length, buf, size, null, size_t.ZERO) == 0) {
                return buf.getString(0);
            }
        }
        return "";
    }
}
