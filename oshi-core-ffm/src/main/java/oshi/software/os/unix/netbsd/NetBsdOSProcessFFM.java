/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.netbsd;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static oshi.ffm.ForeignFunctions.CAPTURED_STATE_LAYOUT;
import static oshi.ffm.ForeignFunctions.callInArenaLongOrDefault;
import static oshi.ffm.ForeignFunctions.callInArenaOrDefault;
import static oshi.ffm.platform.unix.netbsd.NetBsdLibcFunctions.CTL_KERN;
import static oshi.ffm.platform.unix.netbsd.NetBsdLibcFunctions.KERN_ARGMAX;
import static oshi.ffm.platform.unix.netbsd.NetBsdLibcFunctions.KERN_PROC_ARGS;
import static oshi.ffm.platform.unix.netbsd.NetBsdLibcFunctions.KERN_PROC_ENV;
import static oshi.ffm.platform.unix.netbsd.NetBsdLibcFunctions.RLIMIT_NOFILE;
import static oshi.ffm.platform.unix.netbsd.NetBsdLibcFunctions.SIZE_T;
import static oshi.util.LogLevel.WARN;

import java.lang.foreign.MemorySegment;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.platform.unix.PosixLibcFunctions;
import oshi.ffm.util.platform.unix.netbsd.NetBsdSysctlUtilFFM;
import oshi.software.common.os.unix.bsd.BsdPsKeyword;
import oshi.software.common.os.unix.netbsd.NetBsdOSProcess;
import oshi.software.common.os.unix.netbsd.NetBsdOperatingSystem;

/**
 * An OSProcess for NetBSD that uses FFM native calls for the process open-file limits ({@code getrlimit}) and for
 * reading the environment of other processes ({@code kern.proc_args}) when FFM native access is available, and falls
 * back to the command-line {@link NetBsdOSProcess} implementation otherwise.
 */
@ThreadSafe
public class NetBsdOSProcessFFM extends NetBsdOSProcess {

    private static final Logger LOG = LoggerFactory.getLogger(NetBsdOSProcessFFM.class);

    // Maximum size of process argument/environment data (kern.argmax); 0 if FFM or the call is unavailable.
    private static final int ARGMAX = NetBsdSysctlUtilFFM.FFM_AVAILABLE
            ? NetBsdSysctlUtilFFM.sysctl(new int[] { CTL_KERN, KERN_ARGMAX }, 0)
            : 0;

    /**
     * Constructs a new {@code NetBsdOSProcessFFM}.
     *
     * @param pid   the process ID
     * @param psMap the parsed {@code ps} columns for this process
     * @param os    the owning operating system
     */
    public NetBsdOSProcessFFM(int pid, Map<BsdPsKeyword, String> psMap, NetBsdOperatingSystem os) {
        super(pid, psMap, os);
    }

    @Override
    protected long queryRlimitNofile(boolean soft) {
        if (!NetBsdSysctlUtilFFM.FFM_AVAILABLE) {
            return super.queryRlimitNofile(soft);
        }
        return callInArenaLongOrDefault(arena -> {
            MemorySegment rlim = arena.allocate(PosixLibcFunctions.RLIMIT_LAYOUT);
            if (PosixLibcFunctions.getrlimit(RLIMIT_NOFILE, rlim) != 0) {
                return -1L;
            }
            return soft ? PosixLibcFunctions.rlimitCur(rlim) : PosixLibcFunctions.rlimitMax(rlim);
        }, LOG, WARN, "Failed getrlimit", -1L);
    }

    @Override
    protected byte[] queryEnvironmentBytes() {
        // Other processes' environment is only accessible natively
        if (!NetBsdSysctlUtilFFM.FFM_AVAILABLE || ARGMAX <= 0) {
            return super.queryEnvironmentBytes();
        }
        int[] mib = { CTL_KERN, KERN_PROC_ARGS, getProcessID(), KERN_PROC_ENV };
        return callInArenaOrDefault(arena -> {
            MemorySegment mibSeg = arena.allocateFrom(JAVA_INT, mib);
            MemorySegment buf = arena.allocate(ARGMAX);
            MemorySegment size = arena.allocateFrom(SIZE_T, ARGMAX);
            MemorySegment callState = arena.allocate(CAPTURED_STATE_LAYOUT);
            if (NetBsdSysctlUtilFFM.sysctl(callState, mibSeg, mib.length, buf, size) != 0) {
                return new byte[0];
            }
            return buf.asSlice(0, Math.min(ARGMAX, size.get(SIZE_T, 0))).toArray(JAVA_BYTE);
        }, new byte[0], LOG, WARN, "Failed to get environment variables for pid {}", getProcessID());
    }
}
