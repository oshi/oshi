/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.dragonflybsd;

import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.freebsd.Who;
import oshi.jna.platform.unix.DragonFlyBsdLibc;
import oshi.jna.platform.unix.FreeBsdLibc;
import oshi.software.common.os.unix.bsd.BsdPsKeyword;
import oshi.software.common.os.unix.dragonflybsd.DragonFlyBsdOperatingSystem;
import oshi.software.os.FileSystem;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.software.os.OSSession;
import oshi.software.os.unix.freebsd.FreeBsdFileSystemJNA;
import oshi.software.os.unix.freebsd.FreeBsdInternetProtocolStatsJNA;
import oshi.software.os.unix.freebsd.FreeBsdNetworkParamsJNA;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.freebsd.BsdSysctlUtil;
import oshi.util.tuples.Pair;

/**
 * JNA-backed DragonFly BSD operating system.
 */
@ThreadSafe
public class DragonFlyBsdOperatingSystemJNA extends DragonFlyBsdOperatingSystem {

    @Override
    public Pair<String, OSVersionInfo> queryFamilyVersionInfo() {
        String family = BsdSysctlUtil.sysctl("kern.ostype", "DragonFly");

        String version = BsdSysctlUtil.sysctl("kern.osrelease", "");
        String versionInfo = BsdSysctlUtil.sysctl("kern.version", "");
        String buildNumber = versionInfo.split(":")[0].replace(family, "").replace(version, "").trim();

        return new Pair<>(family, new OSVersionInfo(version, null, buildNumber));
    }

    @Override
    public FileSystem getFileSystem() {
        return new FreeBsdFileSystemJNA();
    }

    @Override
    public InternetProtocolStats getInternetProtocolStats() {
        return new FreeBsdInternetProtocolStatsJNA();
    }

    @Override
    public NetworkParams getNetworkParams() {
        return new FreeBsdNetworkParamsJNA();
    }

    @Override
    public List<OSSession> getSessions() {
        return USE_WHO_COMMAND ? super.getSessions() : Who.queryUtxent();
    }

    @Override
    public int getProcessId() {
        return DragonFlyBsdLibc.INSTANCE.getpid();
    }

    @Override
    public int getThreadId() {
        int tid = DragonFlyBsdLibc.INSTANCE.lwp_gettid();
        return tid < 0 ? 0 : tid;
    }

    @Override
    protected OSProcess createProcess(int pid, Map<BsdPsKeyword, String> psMap) {
        return new DragonFlyBsdOSProcessJNA(pid, psMap, this);
    }

    @Override
    protected long queryBootTime() {
        FreeBsdLibc.Timeval tv = new FreeBsdLibc.Timeval();
        if (!BsdSysctlUtil.sysctl("kern.boottime", tv) || tv.tv_sec == 0) {
            // Fall back to text parsing: "{ sec = 1779823319, nsec = 0 } ..."
            return ParseUtil.parseLongOrDefault(
                    ExecutingCommand.getFirstAnswer("sysctl -n kern.boottime").split(",")[0].replaceAll("\\D", ""),
                    System.currentTimeMillis() / 1000);
        }
        return tv.tv_sec;
    }
}
