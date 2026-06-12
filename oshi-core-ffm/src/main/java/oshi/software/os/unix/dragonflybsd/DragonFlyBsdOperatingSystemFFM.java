/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.dragonflybsd;

import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static oshi.ffm.ForeignFunctions.callInArenaIntOrDefault;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.freebsd.WhoFFM;
import oshi.ffm.platform.unix.dragonflybsd.DragonFlyBsdLibcFunctions;
import oshi.ffm.platform.unix.freebsd.FreeBsdLibcFunctions;
import oshi.ffm.util.platform.unix.freebsd.BsdSysctlUtilFFM;
import oshi.software.common.os.unix.bsd.BsdPsKeyword;
import oshi.software.common.os.unix.dragonflybsd.DragonFlyBsdOperatingSystem;
import oshi.software.os.FileSystem;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.software.os.OSSession;
import oshi.software.os.unix.freebsd.FreeBsdFileSystemFFM;
import oshi.software.os.unix.freebsd.FreeBsdInternetProtocolStatsFFM;
import oshi.software.os.unix.freebsd.FreeBsdNetworkParamsFFM;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;

/**
 * FFM-backed DragonFly BSD operating system.
 */
@ThreadSafe
public class DragonFlyBsdOperatingSystemFFM extends DragonFlyBsdOperatingSystem {

    private static final Logger LOG = LoggerFactory.getLogger(DragonFlyBsdOperatingSystemFFM.class);

    @Override
    public Pair<String, OSVersionInfo> queryFamilyVersionInfo() {
        String family = BsdSysctlUtilFFM.sysctl("kern.ostype", "DragonFly");

        String version = BsdSysctlUtilFFM.sysctl("kern.osrelease", "");
        String versionInfo = BsdSysctlUtilFFM.sysctl("kern.version", "");
        String buildNumber = versionInfo.split(":")[0].replace(family, "").replace(version, "").trim();

        return new Pair<>(family, new OSVersionInfo(version, null, buildNumber));
    }

    @Override
    public FileSystem getFileSystem() {
        return new FreeBsdFileSystemFFM();
    }

    @Override
    public InternetProtocolStats getInternetProtocolStats() {
        return new FreeBsdInternetProtocolStatsFFM();
    }

    @Override
    public NetworkParams getNetworkParams() {
        return new FreeBsdNetworkParamsFFM();
    }

    @Override
    public List<OSSession> getSessions() {
        return USE_WHO_COMMAND ? super.getSessions() : WhoFFM.queryUtxent();
    }

    @Override
    public int getProcessId() {
        return callInArenaIntOrDefault(arena -> FreeBsdLibcFunctions.getpid(), LOG, Level.WARN, "getpid failed", 0);
    }

    @Override
    public int getThreadId() {
        int tid = callInArenaIntOrDefault(arena -> DragonFlyBsdLibcFunctions.lwp_gettid(), LOG, Level.WARN,
                "lwp_gettid failed", -1);
        return tid < 0 ? 0 : tid;
    }

    @Override
    protected OSProcess createProcess(int pid, Map<BsdPsKeyword, String> psMap) {
        return new DragonFlyBsdOSProcessFFM(pid, psMap, this);
    }

    @Override
    protected long queryBootTime() {
        try (Arena arena = Arena.ofConfined()) {
            // struct timeval: two longs (tv_sec, tv_usec) = 16 bytes on LP64 BSD.
            MemorySegment tv = arena.allocate(JAVA_LONG.byteSize() * 2);
            if (BsdSysctlUtilFFM.sysctl("kern.boottime", tv) && tv.get(JAVA_LONG, 0) != 0L) {
                return tv.get(JAVA_LONG, 0);
            }
        }
        // Fall back to text parsing: "{ sec = 1779823319, nsec = 0 } ..."
        return ParseUtil.parseLongOrDefault(
                ExecutingCommand.getFirstAnswer("sysctl -n kern.boottime").split(",")[0].replaceAll("\\D", ""),
                System.currentTimeMillis() / 1000);
    }
}
