/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.linux;

import static oshi.ffm.ForeignFunctions.callInArenaLongOrDefault;
import static oshi.ffm.ForeignFunctions.callInArenaOrDefault;
import static oshi.util.LogLevel.DEBUG;
import static oshi.util.LogLevel.WARN;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.platform.linux.LinuxLibcFunctions;
import oshi.software.common.os.linux.LinuxOSProcess;
import oshi.software.common.os.linux.LinuxOperatingSystem;

/**
 * FFM-based Linux OS process. Implements {@code getrlimit} and {@code getrusage} via FFM.
 */
@ThreadSafe
public class LinuxOSProcessFFM extends LinuxOSProcess {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxOSProcessFFM.class);

    public LinuxOSProcessFFM(int pid, LinuxOperatingSystem os) {
        super(pid, os);
    }

    @Override
    protected long[] queryContextSwitches() {
        return callInArenaOrDefault(arena -> {
            MemorySegment rusage = arena.allocate(LinuxLibcFunctions.RUSAGE_SIZE);
            if (0 == LinuxLibcFunctions.getrusage(LinuxLibcFunctions.RUSAGE_SELF, rusage)) {
                return new long[] { rusage.get(ValueLayout.JAVA_LONG, LinuxLibcFunctions.RUSAGE_NVCSW_OFFSET),
                        rusage.get(ValueLayout.JAVA_LONG, LinuxLibcFunctions.RUSAGE_NIVCSW_OFFSET) };
            }
            return null;
        }, LOG, DEBUG, "FFM getrusage failed", null);
    }

    @Override
    protected long queryRlimitSoft() {
        long limit = callInArenaLongOrDefault(arena -> {
            MemorySegment rlim = arena.allocate(LinuxLibcFunctions.RLIMIT_LAYOUT);
            if (0 == LinuxLibcFunctions.getrlimit(LinuxLibcFunctions.RLIMIT_NOFILE, rlim)) {
                return LinuxLibcFunctions.rlimitCur(rlim);
            }
            return -1L;
        }, LOG, WARN, "FFM getrlimit failed", -1L);
        return limit < 0L ? getProcessOpenFileLimit(getProcessID(), 1) : limit;
    }

    @Override
    protected long queryRlimitHard() {
        long limit = callInArenaLongOrDefault(arena -> {
            MemorySegment rlim = arena.allocate(LinuxLibcFunctions.RLIMIT_LAYOUT);
            if (0 == LinuxLibcFunctions.getrlimit(LinuxLibcFunctions.RLIMIT_NOFILE, rlim)) {
                return LinuxLibcFunctions.rlimitMax(rlim);
            }
            return -1L;
        }, LOG, WARN, "FFM getrlimit failed", -1L);
        return limit < 0L ? getProcessOpenFileLimit(getProcessID(), 2) : limit;
    }
}
