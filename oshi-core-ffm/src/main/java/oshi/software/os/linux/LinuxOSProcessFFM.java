/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.linux;

import static org.slf4j.event.Level.DEBUG;
import static oshi.ffm.ForeignFunctions.runInArenaCatchingThrowable;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.linux.LinuxLibcFunctions;
import oshi.software.common.os.linux.LinuxOSProcess;
import oshi.software.common.os.linux.LinuxOperatingSystem;

/**
 * FFM-based Linux OS process. Implements {@code getrlimit} and {@code getrusage} via FFM.
 */
@ThreadSafe
public class LinuxOSProcessFFM extends LinuxOSProcess {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxOSProcessFFM.class);

    private boolean rusagePopulated;
    private long cachedVoluntaryContextSwitches;
    private long cachedInvoluntaryContextSwitches;

    public LinuxOSProcessFFM(int pid, LinuxOperatingSystem os) {
        super(pid, os);
    }

    @Override
    public boolean updateAttributes() {
        boolean result = super.updateAttributes();
        if (getProcessID() == getOs().getProcessId()) {
            this.rusagePopulated = false;
            runInArenaCatchingThrowable(arena -> {
                MemorySegment rusage = arena.allocate(LinuxLibcFunctions.RUSAGE_SIZE);
                if (0 == LinuxLibcFunctions.getrusage(LinuxLibcFunctions.RUSAGE_SELF, rusage)) {
                    this.cachedVoluntaryContextSwitches = rusage.get(ValueLayout.JAVA_LONG,
                            LinuxLibcFunctions.RUSAGE_NVCSW_OFFSET);
                    this.cachedInvoluntaryContextSwitches = rusage.get(ValueLayout.JAVA_LONG,
                            LinuxLibcFunctions.RUSAGE_NIVCSW_OFFSET);
                    this.rusagePopulated = true;
                }
            }, LOG, DEBUG, "FFM getrusage failed");
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
