/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.netbsd;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.os.unix.bsd.BsdOperatingSystem;
import oshi.software.common.os.unix.bsd.BsdPsKeyword;
import oshi.software.os.FileSystem;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.software.os.OSThread;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.common.platform.unix.netbsd.NetBsdSysctlUtil;
import oshi.util.tuples.Pair;

/**
 * NetBsd is a free and open-source Unix-like operating system descended from the Berkeley Software Distribution (BSD),
 * which was based on Research Unix.
 * <p>
 * The cross-BSD-common pieces live in {@link BsdOperatingSystem}; NetBSD has no native-access split, so this single
 * class also provides the {@code ps} process enumeration, the text-parsed boot time, the current-thread factory, and
 * the sysctl version / file-system / network queries.
 */
@ThreadSafe
public class NetBsdOperatingSystem extends BsdOperatingSystem {

    @Override
    public Pair<String, OSVersionInfo> queryFamilyVersionInfo() {
        String family = NetBsdSysctlUtil.sysctl("kern.ostype", "NetBSD");
        String version = NetBsdSysctlUtil.sysctl("kern.osrelease", "");
        String versionInfo = NetBsdSysctlUtil.sysctl("kern.version", "");
        String buildNumber = versionInfo.split(":")[0].replace(family, "").replace(version, "").trim();

        return new Pair<>(family, new OSVersionInfo(version, null, buildNumber));
    }

    @Override
    public FileSystem getFileSystem() {
        return new NetBsdFileSystem();
    }

    @Override
    public InternetProtocolStats getInternetProtocolStats() {
        return new NetBsdInternetProtocolStats();
    }

    @Override
    public NetworkParams getNetworkParams() {
        return new NetBsdNetworkParams();
    }

    @Override
    protected List<OSProcess> getProcessListFromPS(int pid) {
        List<OSProcess> procs = new ArrayList<>();
        // https://man.netbsd.org/ps#KEYWORDS
        // missing are threadCount and kernelTime which is included in cputime
        String psCommand = "ps -awwxo " + NetBsdOSProcess.PS_COMMAND_ARGS;
        if (pid >= 0) {
            psCommand += " -p " + pid;
        }
        List<String> procList = ExecutingCommand.runNative(psCommand);
        if (procList.isEmpty() || procList.size() < 2) {
            return procs;
        }

        // remove header row
        procList.remove(0);
        // Fill list
        for (String proc : procList) {
            Map<BsdPsKeyword, String> psMap = ParseUtil.stringToEnumMap(BsdPsKeyword.class, NetBsdOSProcess.PS_KEYWORDS,
                    proc.trim(), ' ');
            // Check if last (thus all) value populated
            if (psMap.containsKey(BsdPsKeyword.ARGS)) {
                procs.add(new NetBsdOSProcess(
                        pid < 0 ? ParseUtil.parseIntOrDefault(psMap.get(BsdPsKeyword.PID), 0) : pid, psMap, this));
            }
        }
        return procs;
    }

    @Override
    public int getProcessId() {
        // RuntimeMXBean name format is "pid@hostname"
        String name = ManagementFactory.getRuntimeMXBean().getName();
        return ParseUtil.parseIntOrDefault(name.split("@")[0], -1);
    }

    @Override
    // Thread.threadId() (the non-deprecated replacement) requires Java 19; this module compiles to Java 8.
    @SuppressWarnings({ "deprecation", "java:S1874" })
    public int getThreadId() {
        return (int) Thread.currentThread().getId();
    }

    @Override
    public OSThread getCurrentThread() {
        OSProcess proc = getCurrentProcess();
        final int tid = getThreadId();
        return proc.getThreadDetails().stream().filter(t -> t.getThreadId() == tid).findFirst()
                .orElse(new NetBsdOSThread(proc.getProcessID(), tid));
    }

    @Override
    protected long queryBootTime() {
        // Boot time will be the first consecutive string of digits.
        return ParseUtil.parseLongOrDefault(
                ExecutingCommand.getFirstAnswer("sysctl -n kern.boottime").split(",")[0].replaceAll("\\D", ""),
                System.currentTimeMillis() / 1000);
    }
}
