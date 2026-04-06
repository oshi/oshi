/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.linux;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.linux.LinuxLibcFunctions;

/**
 * FFM-based Linux OS process. Implements {@code getrlimit} via FFM.
 */
@ThreadSafe
public class LinuxOSProcessFFM extends LinuxOSProcess {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxOSProcessFFM.class);

    public LinuxOSProcessFFM(int pid, LinuxOperatingSystem os) {
        super(pid, os);
    }

    @Override
    protected long queryRlimitSoft() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment rlim = arena.allocate(LinuxLibcFunctions.RLIMIT_LAYOUT);
            if (0 == LinuxLibcFunctions.getrlimit(LinuxLibcFunctions.RLIMIT_NOFILE, rlim)) {
                return LinuxLibcFunctions.rlimitCur(rlim);
            }
            return getProcessOpenFileLimit(getProcessID(), 1);
        } catch (Throwable e) {
            LOG.warn("FFM getrlimit failed: {}", e.toString());
            return getProcessOpenFileLimit(getProcessID(), 1);
        }
    }

    @Override
    protected long queryRlimitHard() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment rlim = arena.allocate(LinuxLibcFunctions.RLIMIT_LAYOUT);
            if (0 == LinuxLibcFunctions.getrlimit(LinuxLibcFunctions.RLIMIT_NOFILE, rlim)) {
                return LinuxLibcFunctions.rlimitMax(rlim);
            }
            return getProcessOpenFileLimit(getProcessID(), 2);
        } catch (Throwable e) {
            LOG.warn("FFM getrlimit failed: {}", e.toString());
            return getProcessOpenFileLimit(getProcessID(), 2);
        }
    }
}
