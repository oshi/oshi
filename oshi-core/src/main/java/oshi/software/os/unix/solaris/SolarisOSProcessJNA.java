/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.solaris;

import java.util.List;
import java.util.Map;

import com.sun.jna.platform.unix.Resource;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.solaris.PsInfoJNA;
import oshi.jna.platform.unix.SolarisLibc;
import oshi.software.common.os.unix.solaris.SolarisOSProcess;
import oshi.software.os.OSThread;
import oshi.util.tuples.Pair;

/**
 * JNA-backed Solaris OSProcess.
 */
@ThreadSafe
public final class SolarisOSProcessJNA extends SolarisOSProcess {

    private final SolarisOperatingSystemJNA os;

    public SolarisOSProcessJNA(int pid, SolarisOperatingSystemJNA os) {
        super(pid);
        this.os = os;
        updateAttributes();
    }

    @Override
    protected Pair<List<String>, Map<String, String>> queryArgsEnv() {
        return PsInfoJNA.queryArgsEnv(getProcessID(), getPsInfo());
    }

    @Override
    protected OSThread createThread(int lwpid) {
        return new SolarisOSThreadJNA(getProcessID(), lwpid);
    }

    @Override
    public long getSoftOpenFileLimit() {
        if (getProcessID() == this.os.getProcessId()) {
            final Resource.Rlimit rlimit = new Resource.Rlimit();
            if (SolarisLibc.INSTANCE.getrlimit(SolarisLibc.RLIMIT_NOFILE, rlimit) == 0) {
                return rlimit.rlim_cur;
            }
            return -1L;
        } else {
            return getProcessOpenFileLimit(getProcessID(), 1);
        }
    }

    @Override
    public long getHardOpenFileLimit() {
        if (getProcessID() == this.os.getProcessId()) {
            final Resource.Rlimit rlimit = new Resource.Rlimit();
            if (SolarisLibc.INSTANCE.getrlimit(SolarisLibc.RLIMIT_NOFILE, rlimit) == 0) {
                return rlimit.rlim_max;
            }
            return -1L;
        } else {
            return getProcessOpenFileLimit(getProcessID(), 2);
        }
    }
}
