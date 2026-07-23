/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.solaris;

import java.lang.foreign.MemorySegment;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.solaris.WhoFFM;
import oshi.ffm.ForeignFunctions;
import oshi.ffm.platform.unix.solaris.LibKstatFunctions;
import oshi.ffm.platform.unix.solaris.SolarisLibcFunctions;
import oshi.ffm.util.platform.unix.solaris.KstatUtilFFM;
import oshi.ffm.util.platform.unix.solaris.KstatUtilFFM.KstatChain;
import oshi.software.common.os.unix.solaris.SolarisOperatingSystem;
import oshi.software.os.FileSystem;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.software.os.OSSession;
import oshi.software.os.OSThread;
import oshi.util.LogLevel;

/**
 * FFM-backed Solaris OperatingSystem. Uses the legacy {@code kstat} chain only; Kstat2 exists only on the JDK 17-capped
 * latest Solaris, so FFM (JDK 25) never needs it.
 */
@ThreadSafe
public class SolarisOperatingSystemFFM extends SolarisOperatingSystem {

    private static final Logger LOG = LoggerFactory.getLogger(SolarisOperatingSystemFFM.class);

    private static final long BOOTTIME = querySystemBootTime();

    @Override
    public FileSystem getFileSystem() {
        return new SolarisFileSystemFFM();
    }

    @Override
    protected OSProcess createProcess(int pid) {
        return new SolarisOSProcessFFM(pid, this);
    }

    @Override
    public int getProcessId() {
        return ForeignFunctions.callInArenaIntOrDefault(arena -> SolarisLibcFunctions.getpid(), LOG, LogLevel.WARN,
                "getpid failed", 0);
    }

    @Override
    public int getThreadId() {
        return ForeignFunctions.callInArenaIntOrDefault(arena -> SolarisLibcFunctions.thr_self(), LOG, LogLevel.WARN,
                "thr_self failed", 0);
    }

    @Override
    public OSThread getCurrentThread() {
        return new SolarisOSThreadFFM(getProcessId(), getThreadId());
    }

    @Override
    public long getSystemUptime() {
        try (KstatChain kc = KstatUtilFFM.openChain()) {
            MemorySegment ksp = kc.lookup("unix", 0, "system_misc");
            if (ksp.address() != 0L && kc.read(ksp)) {
                return LibKstatFunctions.kstatSnaptime(ksp) / 1_000_000_000L;
            }
        }
        return 0L;
    }

    @Override
    public long getSystemBootTime() {
        return BOOTTIME;
    }

    private static long querySystemBootTime() {
        try (KstatChain kc = KstatUtilFFM.openChain()) {
            MemorySegment ksp = kc.lookup("unix", 0, "system_misc");
            if (ksp.address() != 0L && kc.read(ksp)) {
                return KstatUtilFFM.dataLookupLong(ksp, "boot_time");
            }
        }
        return System.currentTimeMillis() / 1000L;
    }

    @Override
    public NetworkParams getNetworkParams() {
        return new SolarisNetworkParamsFFM();
    }

    @Override
    public List<OSSession> getSessions() {
        return USE_WHO_COMMAND ? super.getSessions() : WhoFFM.queryUtxent();
    }
}
