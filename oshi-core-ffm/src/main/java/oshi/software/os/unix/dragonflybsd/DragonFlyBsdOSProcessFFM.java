/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.dragonflybsd;

import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static oshi.ffm.ForeignFunctions.callInArenaIntOrDefault;
import static oshi.ffm.ForeignFunctions.callInArenaOrDefault;

import java.lang.foreign.MemorySegment;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.platform.unix.PosixLibcFunctions;
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

    public DragonFlyBsdOSProcessFFM(int pid, Map<BsdPsKeyword, String> psMap, DragonFlyBsdOperatingSystemFFM os) {
        super(pid, os);
        updateAttributes(psMap);
    }

    @Override
    protected List<String> queryArguments() {
        // DragonFlyBSD provides command line via /proc filesystem
        byte[] cmdBytes = FileUtil.readAllBytes("/proc/" + getProcessID() + "/cmdline", false);
        if (cmdBytes.length > 0) {
            return Collections.unmodifiableList(ParseUtil.parseByteArrayToStrings(cmdBytes));
        }
        return Collections.emptyList();
    }

    @Override
    protected Map<String, String> queryEnvironmentVariables() {
        // DragonFlyBSD's /proc does not expose environ for other processes.
        // For the current process, use Java's System.getenv().
        int self = callInArenaIntOrDefault(arena -> PosixLibcFunctions.getpid(), LOG, LogLevel.WARN, "getpid failed",
                -1);
        if (getProcessID() == self) {
            return System.getenv();
        }
        return Collections.emptyMap();
    }

    @Override
    protected long queryRlimitNofile(boolean soft) {
        return callInArenaOrDefault(arena -> {
            MemorySegment rlim = arena.allocate(PosixLibcFunctions.RLIMIT_LAYOUT);
            if (PosixLibcFunctions.getrlimit(FreeBsdLibcFunctions.RLIMIT_NOFILE, rlim) != 0) {
                return -1L;
            }
            return rlim.get(JAVA_LONG, soft ? 0 : Long.BYTES);
        }, LOG, LogLevel.WARN, "getrlimit(RLIMIT_NOFILE) failed", -1L);
    }

}
