/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.linux;

import static com.sun.jna.platform.unix.Resource.RLIMIT_NOFILE;
import static oshi.jna.platform.unix.CLibrary.RUSAGE_SELF;

import com.sun.jna.platform.unix.Resource;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.platform.linux.LinuxLibc;
import oshi.software.common.os.linux.LinuxOSProcess;
import oshi.software.common.os.linux.LinuxOperatingSystem;

/**
 * JNA-based Linux OS process. Implements {@code getrlimit} and {@code getrusage} via JNA.
 */
@ThreadSafe
public class LinuxOSProcessJNA extends LinuxOSProcess {

    private boolean rusagePopulated;
    private long cachedVoluntaryContextSwitches;
    private long cachedInvoluntaryContextSwitches;

    public LinuxOSProcessJNA(int pid, LinuxOperatingSystem os) {
        super(pid, os);
    }

    @Override
    public boolean updateAttributes() {
        boolean result = super.updateAttributes();
        if (getProcessID() == getOs().getProcessId()) {
            this.rusagePopulated = false;
            LinuxLibc.Rusage rusage = new LinuxLibc.Rusage();
            if (0 == LinuxLibc.INSTANCE.getrusage(RUSAGE_SELF, rusage)) {
                this.cachedVoluntaryContextSwitches = rusage.ru_nvcsw.longValue();
                this.cachedInvoluntaryContextSwitches = rusage.ru_nivcsw.longValue();
                this.rusagePopulated = true;
            }
        }
        return result;
    }

    @Override
    public long getVoluntaryContextSwitches() {
        if (rusagePopulated) {
            return cachedVoluntaryContextSwitches;
        }
        return super.getVoluntaryContextSwitches();
    }

    @Override
    public long getInvoluntaryContextSwitches() {
        if (rusagePopulated) {
            return cachedInvoluntaryContextSwitches;
        }
        return super.getInvoluntaryContextSwitches();
    }

    @Override
    protected long queryRlimitSoft() {
        final Resource.Rlimit rlimit = new Resource.Rlimit();
        if (0 == LinuxLibc.INSTANCE.getrlimit(RLIMIT_NOFILE, rlimit)) {
            return rlimit.rlim_cur;
        }
        return getProcessOpenFileLimit(getProcessID(), 1);
    }

    @Override
    protected long queryRlimitHard() {
        final Resource.Rlimit rlimit = new Resource.Rlimit();
        if (0 == LinuxLibc.INSTANCE.getrlimit(RLIMIT_NOFILE, rlimit)) {
            return rlimit.rlim_max;
        }
        return getProcessOpenFileLimit(getProcessID(), 2);
    }
}
