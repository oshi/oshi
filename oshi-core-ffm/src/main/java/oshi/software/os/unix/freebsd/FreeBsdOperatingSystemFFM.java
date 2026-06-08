/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.freebsd;

import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static oshi.ffm.ForeignFunctions.callInArenaIntOrDefault;
import static oshi.software.os.OSService.State.RUNNING;
import static oshi.software.os.OSService.State.STOPPED;
import static oshi.software.os.OperatingSystem.ProcessFiltering.VALID_PROCESS;

import java.io.File;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.freebsd.WhoFFM;
import oshi.ffm.platform.unix.freebsd.FreeBsdLibcFunctions;
import oshi.ffm.util.platform.unix.freebsd.BsdSysctlUtilFFM;
import oshi.software.common.AbstractOperatingSystem;
import oshi.software.common.os.unix.freebsd.FreeBsdOSThread;
import oshi.software.os.FileSystem;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.software.os.OSService;
import oshi.software.os.OSSession;
import oshi.software.os.OSThread;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;

/**
 * FFM-backed FreeBSD operating system.
 */
@ThreadSafe
public class FreeBsdOperatingSystemFFM extends AbstractOperatingSystem {

    private static final Logger LOG = LoggerFactory.getLogger(FreeBsdOperatingSystemFFM.class);

    private static final long BOOTTIME = querySystemBootTime();

    /*
     * Package-private for use by FreeBsdOSProcessFFM
     */
    enum PsKeywords {
        STATE, PID, PPID, USER, UID, GROUP, GID, NLWP, PRI, VSZ, RSS, ETIMES, SYSTIME, TIME, COMM, MAJFLT, MINFLT,
        NVCSW, NIVCSW, ARGS; // ARGS must always be last
    }

    static final String PS_COMMAND_ARGS = Arrays.stream(PsKeywords.values()).map(Enum::name)
            .map(name -> name.toLowerCase(Locale.ROOT)).collect(Collectors.joining(","));

    @Override
    public String queryManufacturer() {
        return "Unix/BSD";
    }

    @Override
    public Pair<String, OSVersionInfo> queryFamilyVersionInfo() {
        String family = BsdSysctlUtilFFM.sysctl("kern.ostype", "FreeBSD");

        String version = BsdSysctlUtilFFM.sysctl("kern.osrelease", "");
        String versionInfo = BsdSysctlUtilFFM.sysctl("kern.version", "");
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
        return new FreeBsdFileSystemFFM();
    }

    @Override
    public InternetProtocolStats getInternetProtocolStats() {
        return new FreeBsdInternetProtocolStatsFFM();
    }

    @Override
    public List<OSSession> getSessions() {
        return USE_WHO_COMMAND ? super.getSessions() : WhoFFM.queryUtxent();
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
        String psCommand = "ps -awwxo " + PS_COMMAND_ARGS;
        if (pid >= 0) {
            psCommand += " -p " + pid;
        }

        Predicate<Map<PsKeywords, String>> hasKeywordArgs = psMap -> psMap.containsKey(PsKeywords.ARGS);
        return ExecutingCommand.runNative(psCommand).stream().skip(1).parallel()
                .map(proc -> ParseUtil.stringToEnumMap(PsKeywords.class, proc.trim(), ' ')).filter(hasKeywordArgs)
                .map(psMap -> new FreeBsdOSProcessFFM(
                        pid < 0 ? ParseUtil.parseIntOrDefault(psMap.get(PsKeywords.PID), 0) : pid, psMap, this))
                .filter(VALID_PROCESS).collect(Collectors.toList());
    }

    @Override
    public int getProcessId() {
        return callInArenaIntOrDefault(arena -> FreeBsdLibcFunctions.getpid(), LOG, Level.WARN, "getpid failed", 0);
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
        return callInArenaIntOrDefault(arena -> {
            MemorySegment id = arena.allocate(JAVA_LONG);
            if (FreeBsdLibcFunctions.thr_self(id) < 0) {
                return 0;
            }
            return (int) id.get(JAVA_LONG, 0);
        }, LOG, Level.WARN, "thr_self failed", 0);
    }

    @Override
    public OSThread getCurrentThread() {
        OSProcess proc = getCurrentProcess();
        final int tid = getThreadId();
        return proc.getThreadDetails().stream().filter(t -> t.getThreadId() == tid).findFirst()
                .orElse(new FreeBsdOSThread(proc.getProcessID(), tid));
    }

    @Override
    public int getThreadCount() {
        int threads = 0;
        for (String proc : ExecutingCommand.runNative("ps -axo nlwp")) {
            threads += ParseUtil.parseIntOrDefault(proc.trim(), 0);
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
        try (Arena arena = Arena.ofConfined()) {
            // struct timeval: two longs (tv_sec, tv_usec) = 16 bytes on LP64 FreeBSD.
            MemorySegment tv = arena.allocate(JAVA_LONG.byteSize() * 2);
            if (BsdSysctlUtilFFM.sysctl("kern.boottime", tv) && tv.get(JAVA_LONG, 0) != 0L) {
                return tv.get(JAVA_LONG, 0);
            }
        }
        // Fall back to text parsing if sysctl fails or returns zero.
        return ParseUtil.parseLongOrDefault(
                ExecutingCommand.getFirstAnswer("sysctl -n kern.boottime").split(",")[0].replaceAll("\\D", ""),
                System.currentTimeMillis() / 1000);
    }

    @Override
    public NetworkParams getNetworkParams() {
        return new FreeBsdNetworkParamsFFM();
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
