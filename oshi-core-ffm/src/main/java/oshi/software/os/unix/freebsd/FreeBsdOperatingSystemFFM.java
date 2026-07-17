/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.freebsd;

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
import oshi.ffm.platform.unix.freebsd.FreeBsdLibcFunctions;
import oshi.ffm.util.platform.unix.freebsd.BsdSysctlUtilFFM;
import oshi.software.common.os.unix.bsd.BsdPsKeyword;
import oshi.software.common.os.unix.freebsd.FreeBsdOperatingSystem;
import oshi.software.os.FileSystem;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.software.os.OSSession;

/**
 * FFM-backed FreeBSD operating system.
 */
@ThreadSafe
public class FreeBsdOperatingSystemFFM extends FreeBsdOperatingSystem {

    private static final Logger LOG = LoggerFactory.getLogger(FreeBsdOperatingSystemFFM.class);

    @Override
    protected String querySysctl(String name, String def) {
        return BsdSysctlUtilFFM.sysctl(name, def);
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
    protected List<OSSession> queryWhoSessions() {
        return WhoFFM.queryUtxent();
    }

    @Override
    public int getProcessId() {
        return callInArenaIntOrDefault(arena -> FreeBsdLibcFunctions.getpid(), LOG, Level.WARN, "getpid failed", 0);
    }

    @Override
    public int getThreadId() {
        return callInArenaIntOrDefault(arena -> {
            MemorySegment id = arena.allocate(JAVA_LONG);
            if (FreeBsdLibcFunctions.thr_self(id) < 0) {
                return 0;
            }
            return (int) id.get(JAVA_LONG, 0);
        }, LOG, Level.WARN, "thr_self failed", 0);
    }

    @Override
    protected OSProcess createProcess(int pid, Map<BsdPsKeyword, String> psMap) {
        return new FreeBsdOSProcessFFM(pid, psMap, this);
    }

    @Override
    protected long queryKernBoottimeSeconds() {
        try (Arena arena = Arena.ofConfined()) {
            // struct timeval: two longs (tv_sec, tv_usec) = 16 bytes on LP64 FreeBSD.
            MemorySegment tv = arena.allocate(JAVA_LONG.byteSize() * 2);
            if (BsdSysctlUtilFFM.sysctl("kern.boottime", tv) && tv.get(JAVA_LONG, 0) != 0L) {
                return tv.get(JAVA_LONG, 0);
            }
        }
        return 0L;
    }
}
