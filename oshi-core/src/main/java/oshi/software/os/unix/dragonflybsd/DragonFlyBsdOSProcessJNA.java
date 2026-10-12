/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.dragonflybsd;

import static oshi.jna.platform.unix.FreeBsdLibc.RLIMIT_NOFILE;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.sun.jna.platform.unix.Resource;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.platform.unix.DragonFlyBsdLibc;
import oshi.software.common.os.unix.bsd.BsdPsKeyword;
import oshi.software.common.os.unix.dragonflybsd.DragonFlyBsdOSProcess;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;

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
    protected List<String> queryArguments() {
        // DragonFlyBSD provides command line via /proc filesystem
        byte[] cmdBytes = FileUtil.readAllBytes("/proc/" + getProcessID() + "/cmdline", false);
        if (cmdBytes.length > 0) {
            return Collections.unmodifiableList(ParseUtil.parseByteArrayToStrings(cmdBytes));
        }
        return Collections.emptyList();
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

}
