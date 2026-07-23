/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.dragonflybsd;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static oshi.ffm.ForeignFunctions.CAPTURED_STATE_LAYOUT;
import static oshi.ffm.ForeignFunctions.callInArenaIntOrDefault;
import static oshi.ffm.ForeignFunctions.callInArenaOrDefault;

import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.platform.unix.freebsd.FreeBsdLibcFunctions;
import oshi.software.common.os.unix.bsd.BsdPsKeyword;
import oshi.software.common.os.unix.dragonflybsd.DragonFlyBsdOSProcess;
import oshi.util.FileUtil;
import oshi.util.LogLevel;
import oshi.util.ParseUtil;

/**
 * FFM-backed DragonFly BSD OS process.
 */
@ThreadSafe
public class DragonFlyBsdOSProcessFFM extends DragonFlyBsdOSProcess {

    private static final Logger LOG = LoggerFactory.getLogger(DragonFlyBsdOSProcessFFM.class);

    private final DragonFlyBsdOperatingSystemFFM os;

    public DragonFlyBsdOSProcessFFM(int pid, Map<BsdPsKeyword, String> psMap, DragonFlyBsdOperatingSystemFFM os) {
        super(pid);
        this.os = os;
        updateAttributes(psMap);
    }

    @Override
    protected List<String> queryArguments() {
        // DragonFlyBSD provides command line via /proc filesystem
        byte[] cmdBytes = FileUtil.readAllBytes("/proc/" + getProcessID() + "/cmdline", false);
        if (cmdBytes != null && cmdBytes.length > 0) {
            return Collections.unmodifiableList(ParseUtil.parseByteArrayToStrings(cmdBytes));
        }
        return Collections.emptyList();
    }

    @Override
    protected Map<String, String> queryEnvironmentVariables() {
        // DragonFlyBSD's /proc does not expose environ for other processes.
        // For the current process, use Java's System.getenv().
        int self = callInArenaIntOrDefault(arena -> FreeBsdLibcFunctions.getpid(), LOG, LogLevel.WARN, "getpid failed",
                -1);
        if (getProcessID() == self) {
            return System.getenv();
        }
        return Collections.emptyMap();
    }

    @Override
    protected int queryOwnProcessId() {
        return this.os.getProcessId();
    }

    @Override
    protected long queryRlimitNofile(boolean soft) {
        return callInArenaOrDefault(arena -> {
            MemorySegment rlim = arena.allocate(FreeBsdLibcFunctions.RLIMIT_LAYOUT);
            if (FreeBsdLibcFunctions.getrlimit(FreeBsdLibcFunctions.RLIMIT_NOFILE, rlim) != 0) {
                return -1L;
            }
            return rlim.get(JAVA_LONG, soft ? 0 : Long.BYTES);
        }, LOG, LogLevel.WARN, "getrlimit(RLIMIT_NOFILE) failed", -1L);
    }

    @Override
    protected int queryBitness() {
        return callInArenaIntOrDefault(arena -> {
            // CTL_KERN.KERN_PROC.KERN_PROC_SV_NAME.<pid>
            MemorySegment mib = arena.allocateFrom(JAVA_INT, 1, 14, 9, getProcessID());
            MemorySegment buf = arena.allocate(32);
            MemorySegment size = arena.allocate(JAVA_LONG);
            size.set(JAVA_LONG, 0, 32L);
            MemorySegment callState = arena.allocate(CAPTURED_STATE_LAYOUT);
            int rc = FreeBsdLibcFunctions.sysctl(callState, mib, 4, buf, size, MemorySegment.NULL, 0L);
            if (rc != 0) {
                return 0;
            }
            byte[] bytes = buf.asSlice(0, Math.min(size.get(JAVA_LONG, 0), 32L)).toArray(JAVA_BYTE);
            return elfBitness(new String(bytes, StandardCharsets.UTF_8).trim());
        }, LOG, LogLevel.WARN, "queryBitness failed", 0);
    }

}
