/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.solaris;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.solaris.PsInfoFFM;
import oshi.ffm.platform.unix.solaris.SolarisLibcFunctions;
import oshi.software.common.os.unix.solaris.SolarisOSProcess;
import oshi.software.os.OSThread;
import oshi.util.tuples.Pair;

/**
 * FFM-backed Solaris OSProcess.
 */
@ThreadSafe
public final class SolarisOSProcessFFM extends SolarisOSProcess {

    private static final Logger LOG = LoggerFactory.getLogger(SolarisOSProcessFFM.class);

    private final SolarisOperatingSystemFFM os;

    public SolarisOSProcessFFM(int pid, SolarisOperatingSystemFFM os) {
        super(pid);
        this.os = os;
        updateAttributes();
    }

    @Override
    protected Pair<List<String>, Map<String, String>> queryArgsEnv() {
        return PsInfoFFM.queryArgsEnv(getProcessID());
    }

    @Override
    protected OSThread createThread(int lwpid) {
        return new SolarisOSThreadFFM(getProcessID(), lwpid);
    }

    @Override
    public long getSoftOpenFileLimit() {
        if (getProcessID() == this.os.getProcessId()) {
            return rlimitNofile(true);
        } else {
            return getProcessOpenFileLimit(getProcessID(), 1);
        }
    }

    @Override
    public long getHardOpenFileLimit() {
        if (getProcessID() == this.os.getProcessId()) {
            return rlimitNofile(false);
        } else {
            return getProcessOpenFileLimit(getProcessID(), 2);
        }
    }

    private static long rlimitNofile(boolean soft) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment rlim = arena.allocate(SolarisLibcFunctions.RLIMIT_LAYOUT);
            if (SolarisLibcFunctions.getrlimit(SolarisLibcFunctions.RLIMIT_NOFILE, rlim) != 0) {
                return -1L;
            }
            return soft ? SolarisLibcFunctions.rlimitCur(rlim) : SolarisLibcFunctions.rlimitMax(rlim);
        } catch (Throwable t) {
            LOG.warn("getrlimit failed", t);
            return -1L;
        }
    }
}
