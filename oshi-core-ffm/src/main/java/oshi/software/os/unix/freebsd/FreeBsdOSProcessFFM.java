/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.freebsd;

import static java.lang.foreign.ValueLayout.JAVA_INT;
import static oshi.ffm.ForeignFunctions.CAPTURED_STATE_LAYOUT;
import static oshi.ffm.ForeignFunctions.callInArenaOrDefault;
import static oshi.ffm.ForeignFunctions.getByteArrayFromNativePointer;
import static oshi.ffm.ForeignFunctions.getErrno;
import static oshi.ffm.platform.unix.freebsd.FreeBsdLibcFunctions.RLIMIT_LAYOUT;
import static oshi.ffm.platform.unix.freebsd.FreeBsdLibcFunctions.RLIMIT_NOFILE;
import static oshi.ffm.platform.unix.freebsd.FreeBsdLibcFunctions.SIZE_T;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.platform.unix.freebsd.FreeBsdLibcFunctions;
import oshi.ffm.util.platform.unix.freebsd.BsdSysctlUtilFFM;
import oshi.software.common.os.unix.bsd.BsdPsKeyword;
import oshi.software.common.os.unix.freebsd.FreeBsdOSProcess;
import oshi.util.ParseUtil;
import oshi.util.common.platform.unix.freebsd.ProcstatUtil;

/**
 * FFM-backed FreeBSD OSProcess.
 */
@ThreadSafe
public class FreeBsdOSProcessFFM extends FreeBsdOSProcess {

    private static final Logger LOG = LoggerFactory.getLogger(FreeBsdOSProcessFFM.class);

    private static final int ARGMAX = BsdSysctlUtilFFM.sysctl("kern.argmax", 0);

    // CTL_KERN.KERN_PROC.<index>.<pid> sysctl MIB constants
    private static final int CTL_KERN = 1;
    private static final int KERN_PROC = 14;
    private static final int KERN_PROC_ARGS = 7;
    private static final int KERN_PROC_SV_NAME = 9;
    private static final int KERN_PROC_ENV = 35;

    private final FreeBsdOperatingSystemFFM os;

    public FreeBsdOSProcessFFM(int pid, Map<BsdPsKeyword, String> psMap, FreeBsdOperatingSystemFFM os) {
        super(pid);
        this.os = os;
        updateAttributes(psMap);
    }

    @Override
    protected List<String> queryArguments() {
        if (ARGMAX <= 0) {
            return Collections.emptyList();
        }
        return callInArenaOrDefault(arena -> {
            MemorySegment mib = arena.allocateFrom(JAVA_INT, CTL_KERN, KERN_PROC, KERN_PROC_ARGS, getProcessID());
            MemorySegment buf = arena.allocate(ARGMAX);
            MemorySegment size = arena.allocateFrom(SIZE_T, ARGMAX);
            MemorySegment callState = arena.allocate(CAPTURED_STATE_LAYOUT);
            int rc = FreeBsdLibcFunctions.sysctl(callState, mib, 4, buf, size, MemorySegment.NULL, 0L);
            if (rc != 0) {
                LOG.warn(
                        "Failed sysctl call for process arguments (kern.proc.args), process {} may not exist. Error code: {}",
                        getProcessID(), getErrno(callState));
                return Collections.<String>emptyList();
            }
            long written = size.get(SIZE_T, 0);
            byte[] bytes = getByteArrayFromNativePointer(buf, written, arena);
            return Collections.unmodifiableList(ParseUtil.parseByteArrayToStrings(bytes));
        }, LOG, org.slf4j.event.Level.WARN, "queryArguments failed", Collections.<String>emptyList());
    }

    @Override
    protected Map<String, String> queryEnvironmentVariables() {
        if (ARGMAX <= 0) {
            return Collections.emptyMap();
        }
        return callInArenaOrDefault(arena -> {
            MemorySegment mib = arena.allocateFrom(JAVA_INT, CTL_KERN, KERN_PROC, KERN_PROC_ENV, getProcessID());
            MemorySegment buf = arena.allocate(ARGMAX);
            MemorySegment size = arena.allocateFrom(SIZE_T, ARGMAX);
            MemorySegment callState = arena.allocate(CAPTURED_STATE_LAYOUT);
            int rc = FreeBsdLibcFunctions.sysctl(callState, mib, 4, buf, size, MemorySegment.NULL, 0L);
            if (rc != 0) {
                LOG.warn(
                        "Failed sysctl call for process environment variables (kern.proc.env), process {} may not exist. Error code: {}",
                        getProcessID(), getErrno(callState));
                return Collections.<String, String>emptyMap();
            }
            long written = size.get(SIZE_T, 0);
            byte[] bytes = getByteArrayFromNativePointer(buf, written, arena);
            return Collections.unmodifiableMap(ParseUtil.parseByteArrayToStringMap(bytes));
        }, LOG, org.slf4j.event.Level.WARN, "queryEnvironmentVariables failed", Collections.<String, String>emptyMap());
    }

    @Override
    public String getCurrentWorkingDirectory() {
        return ProcstatUtil.getCwd(getProcessID());
    }

    @Override
    public long getOpenFiles() {
        return ProcstatUtil.getOpenFiles(getProcessID());
    }

    @Override
    public long getSoftOpenFileLimit() {
        if (getProcessID() == this.os.getProcessId()) {
            return rlimitNofile(true);
        }
        return getProcessOpenFileLimit(getProcessID(), 1);
    }

    @Override
    public long getHardOpenFileLimit() {
        if (getProcessID() == this.os.getProcessId()) {
            return rlimitNofile(false);
        }
        return getProcessOpenFileLimit(getProcessID(), 2);
    }

    private static long rlimitNofile(boolean soft) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment rlim = arena.allocate(RLIMIT_LAYOUT);
            if (FreeBsdLibcFunctions.getrlimit(RLIMIT_NOFILE, rlim) != 0) {
                return -1L;
            }
            return soft ? FreeBsdLibcFunctions.rlimitCur(rlim) : FreeBsdLibcFunctions.rlimitMax(rlim);
        } catch (Throwable t) {
            LOG.warn("getrlimit(RLIMIT_NOFILE) failed", t);
            return -1L;
        }
    }

    @Override
    protected int queryBitness() {
        return callInArenaOrDefault(arena -> {
            MemorySegment mib = arena.allocateFrom(JAVA_INT, CTL_KERN, KERN_PROC, KERN_PROC_SV_NAME, getProcessID());
            MemorySegment buf = arena.allocate(32L);
            MemorySegment size = arena.allocateFrom(SIZE_T, 32L);
            MemorySegment callState = arena.allocate(CAPTURED_STATE_LAYOUT);
            if (0 != FreeBsdLibcFunctions.sysctl(callState, mib, 4, buf, size, MemorySegment.NULL, 0L)) {
                return 0;
            }
            String elf = buf.getString(0);
            if (elf.contains("ELF32")) {
                return 32;
            } else if (elf.contains("ELF64")) {
                return 64;
            }
            return 0;
        }, LOG, org.slf4j.event.Level.WARN, "queryBitness failed", 0);
    }
}
