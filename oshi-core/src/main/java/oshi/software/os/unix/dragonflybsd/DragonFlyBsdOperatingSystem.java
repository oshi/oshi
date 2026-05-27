/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.dragonflybsd;

import static oshi.software.os.OSService.State.RUNNING;
import static oshi.software.os.OSService.State.STOPPED;
import static oshi.software.os.OperatingSystem.ProcessFiltering.VALID_PROCESS;

import java.io.File;
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

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.freebsd.Who;
import oshi.jna.platform.unix.DragonFlyBsdLibc;
import oshi.jna.platform.unix.FreeBsdLibc;
import oshi.software.common.AbstractOperatingSystem;
import oshi.software.os.FileSystem;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.software.os.OSService;
import oshi.software.os.OSSession;
import oshi.software.os.OSThread;
import oshi.software.os.unix.freebsd.FreeBsdFileSystem;
import oshi.software.os.unix.freebsd.FreeBsdInternetProtocolStats;
import oshi.software.os.unix.freebsd.FreeBsdNetworkParams;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.freebsd.BsdSysctlUtil;
import oshi.util.tuples.Pair;

/**
 * DragonFly BSD is a free and open-source Unix-like operating system forked from FreeBSD 4.8 in 2003. It features the
 * HAMMER2 filesystem and a unique approach to SMP with its lightweight kernel threads (LWKT) subsystem.
 */
@ThreadSafe
public class DragonFlyBsdOperatingSystem extends AbstractOperatingSystem {

    private static final Logger LOG = LoggerFactory.getLogger(DragonFlyBsdOperatingSystem.class);

    private static final long BOOTTIME = querySystemBootTime();

    /*
     * Package-private for use by DragonFlyBsdOSProcess
     */
    enum PsKeywords {
        STATE, PID, PPID, USER, UID, RGID, NLWP, PRI, VSZ, RSS, TIME, MAJFLT, MINFLT, NVCSW, NIVCSW, UCOMM, COMMAND; // COMMAND
                                                                                                                     // must
                                                                                                                     // always
                                                                                                                     // be
                                                                                                                     // last
    }

    static final String PS_COMMAND_ARGS = Arrays.stream(PsKeywords.values()).map(Enum::name)
            .map(name -> name.toLowerCase(Locale.ROOT)).collect(Collectors.joining(","));

    @Override
    public String queryManufacturer() {
        return "Unix/BSD";
    }

    @Override
    public Pair<String, OSVersionInfo> queryFamilyVersionInfo() {
        String family = BsdSysctlUtil.sysctl("kern.ostype", "DragonFly");

        String version = BsdSysctlUtil.sysctl("kern.osrelease", "");
        String versionInfo = BsdSysctlUtil.sysctl("kern.version", "");
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
        return new FreeBsdFileSystem();
    }

    @Override
    public InternetProtocolStats getInternetProtocolStats() {
        return new FreeBsdInternetProtocolStats();
    }

    @Override
    public List<OSSession> getSessions() {
        return USE_WHO_COMMAND ? super.getSessions() : Who.queryUtxent();
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

        Predicate<Map<PsKeywords, String>> hasKeywordArgs = psMap -> psMap.containsKey(PsKeywords.COMMAND);
        return ExecutingCommand.runNative(psCommand).stream().skip(1).parallel()
                .map(proc -> ParseUtil.stringToEnumMap(PsKeywords.class, proc.trim(), ' ')).filter(hasKeywordArgs)
                .map(psMap -> new DragonFlyBsdOSProcess(
                        pid < 0 ? ParseUtil.parseIntOrDefault(psMap.get(PsKeywords.PID), 0) : pid, psMap, this))
                // DragonFlyBSD kernel threads report PID -1; filter them out
                .filter(proc -> proc.getProcessID() > 0).filter(VALID_PROCESS).collect(Collectors.toList());
    }

    @Override
    public int getProcessId() {
        return DragonFlyBsdLibc.INSTANCE.getpid();
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
        int tid = DragonFlyBsdLibc.INSTANCE.lwp_gettid();
        return tid < 0 ? 0 : tid;
    }

    @Override
    public OSThread getCurrentThread() {
        OSProcess proc = getCurrentProcess();
        final int tid = getThreadId();
        return proc.getThreadDetails().stream().filter(t -> t.getThreadId() == tid).findFirst()
                .orElse(new DragonFlyBsdOSThread(proc.getProcessID(), tid));
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
        FreeBsdLibc.Timeval tv = new FreeBsdLibc.Timeval();
        if (!BsdSysctlUtil.sysctl("kern.boottime", tv) || tv.tv_sec == 0) {
            // Fall back to text parsing: "{ sec = 1779823319, nsec = 0 } ..."
            return ParseUtil.parseLongOrDefault(
                    ExecutingCommand.getFirstAnswer("sysctl -n kern.boottime").split(",")[0].replaceAll("\\D", ""),
                    System.currentTimeMillis() / 1000);
        }
        return tv.tv_sec;
    }

    @Override
    public NetworkParams getNetworkParams() {
        return new FreeBsdNetworkParams();
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
