/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.netbsd;

import static oshi.software.os.OSService.State.RUNNING;
import static oshi.software.os.OSService.State.STOPPED;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractOperatingSystem;
import oshi.software.common.os.unix.bsd.BsdPsKeyword;
import oshi.software.os.FileSystem;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.software.os.OSService;
import oshi.software.os.OSThread;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.common.platform.unix.netbsd.NetBsdSysctlUtil;
import oshi.util.tuples.Pair;

/**
 * NetBsd is a free and open-source Unix-like operating system descended from the Berkeley Software Distribution (BSD),
 * which was based on Research Unix.
 */
@ThreadSafe
public class NetBsdOperatingSystem extends AbstractOperatingSystem {

    private static final Logger LOG = LoggerFactory.getLogger(NetBsdOperatingSystem.class);

    private static final long BOOTTIME = querySystemBootTime();

    @Override
    public String queryManufacturer() {
        return "Unix/BSD";
    }

    @Override
    public Pair<String, OSVersionInfo> queryFamilyVersionInfo() {
        String family = NetBsdSysctlUtil.sysctl("kern.ostype", "NetBSD");
        String version = NetBsdSysctlUtil.sysctl("kern.osrelease", "");
        String versionInfo = NetBsdSysctlUtil.sysctl("kern.version", "");
        String buildNumber = versionInfo.split(":")[0].replace(family, "").replace(version, "").trim();

        return new Pair<>(family, new OSVersionInfo(version, null, buildNumber));
    }

    @Override
    protected int queryBitness(int jvmBitness) {
        if (jvmBitness < 64 && ExecutingCommand.getFirstAnswer("uname -m").indexOf("64") == -1) {
            return jvmBitness;
        }
        return 64;
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
    public List<OSProcess> queryAllProcesses() {
        return getProcessListFromPS(-1);
    }

    @Override
    public List<OSProcess> queryChildProcesses(int parentPid) {
        List<OSProcess> allProcs = queryAllProcesses();
        Set<Integer> descendantPids = getChildrenOrDescendants(allProcs, parentPid, false);
        return allProcs.stream().filter(p -> descendantPids.contains(p.getProcessID())).collect(Collectors.toList());
    }

    @Override
    public List<OSProcess> queryDescendantProcesses(int parentPid) {
        List<OSProcess> allProcs = queryAllProcesses();
        Set<Integer> descendantPids = getChildrenOrDescendants(allProcs, parentPid, true);
        return allProcs.stream().filter(p -> descendantPids.contains(p.getProcessID())).collect(Collectors.toList());
    }

    @Override
    public OSProcess getProcess(int pid) {
        List<OSProcess> procs = getProcessListFromPS(pid);
        if (procs.isEmpty()) {
            return null;
        }
        return procs.get(0);
    }

    private List<OSProcess> getProcessListFromPS(int pid) {
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
        String name = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
        return ParseUtil.parseIntOrDefault(name.split("@")[0], -1);
    }

    @Override
    public int getProcessCount() {
        List<String> procList = ExecutingCommand.runNative("ps -axo pid");
        if (!procList.isEmpty()) {
            // Subtract 1 for header
            return procList.size() - 1;
        }
        return 0;
    }

    @Override
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
    public int getThreadCount() {
        int threads = 0;
        // Sum nlwp (number of LWPs) across all processes
        List<String> nlwpList = ExecutingCommand.runNative("ps -axo nlwp");
        for (String line : nlwpList) {
            threads += ParseUtil.parseIntOrDefault(line.trim(), 0);
        }
        return threads;
    }

    @Override
    public long getSystemUptime() {
        return System.currentTimeMillis() / 1000 - BOOTTIME;
    }

    @Override
    public long getSystemBootTime() {
        return BOOTTIME;
    }

    private static long querySystemBootTime() {
        // Boot time will be the first consecutive string of digits.
        return ParseUtil.parseLongOrDefault(
                ExecutingCommand.getFirstAnswer("sysctl -n kern.boottime").split(",")[0].replaceAll("\\D", ""),
                System.currentTimeMillis() / 1000);
    }

    @Override
    public NetworkParams getNetworkParams() {
        return new NetBsdNetworkParams();
    }

    @Override
    public List<OSService> getServices() {
        // Get running services
        List<OSService> services = new ArrayList<>();
        Set<String> running = new HashSet<>();
        for (OSProcess p : getChildProcesses(1, ProcessFiltering.ALL_PROCESSES, ProcessSorting.PID_ASC, 0)) {
            OSService s = new OSService(p.getName(), p.getProcessID(), RUNNING);
            services.add(s);
            running.add(p.getName());
        }
        // Get Directories for stopped services
        File dir = new File("/etc/rc.d");
        File[] listFiles;
        if (dir.exists() && dir.isDirectory() && (listFiles = dir.listFiles()) != null) {
            for (File f : listFiles) {
                String name = f.getName();
                if (!running.contains(name)) {
                    OSService s = new OSService(name, 0, STOPPED);
                    services.add(s);
                }
            }
        } else {
            LOG.error("Directory: /etc/rc.d does not exist");
        }
        return services;
    }
}
