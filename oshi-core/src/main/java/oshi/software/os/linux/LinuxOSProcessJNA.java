/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.linux;

import com.sun.jna.platform.unix.Resource;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.platform.linux.LinuxLibc;

/**
 * JNA-based Linux OS process. Implements {@code getrlimit} via JNA.
 */
@ThreadSafe
public class LinuxOSProcessJNA extends LinuxOSProcess {

    public LinuxOSProcessJNA(int pid, LinuxOperatingSystem os) {
        super(pid, os);
    }

    @Override
    protected long queryRlimitSoft() {
        final Resource.Rlimit rlimit = new Resource.Rlimit();
        if (0 == LinuxLibc.INSTANCE.getrlimit(LinuxLibc.RLIMIT_NOFILE, rlimit)) {
            return rlimit.rlim_cur;
        }
        return getProcessOpenFileLimit(getProcessID(), 1);
    }

    @Override
    protected long queryRlimitHard() {
        final Resource.Rlimit rlimit = new Resource.Rlimit();
        if (0 == LinuxLibc.INSTANCE.getrlimit(LinuxLibc.RLIMIT_NOFILE, rlimit)) {
            return rlimit.rlim_max;
        }
        return getProcessOpenFileLimit(getProcessID(), 2);
    }
}
