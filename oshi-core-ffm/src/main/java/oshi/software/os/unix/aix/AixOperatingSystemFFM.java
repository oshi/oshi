/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.aix;

import static oshi.software.os.OSProcess.State.INVALID;
import static oshi.software.os.OSService.State.RUNNING;
import static oshi.software.os.OSService.State.STOPPED;
import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.installedAppsExpiration;
import static oshi.util.Memoizer.memoize;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.unix.aix.Uptime;
import oshi.driver.common.unix.aix.Who;
import oshi.driver.unix.aix.perfstat.PerfstatConfigFFM;
import oshi.driver.unix.aix.perfstat.PerfstatProcessFFM;
import oshi.ffm.platform.unix.aix.AixLibcFunctions;
import oshi.software.common.AbstractOperatingSystem;
import oshi.software.common.os.unix.aix.AixFileSystem;
import oshi.software.common.os.unix.aix.AixInstalledApps;
import oshi.software.common.os.unix.aix.AixOSThread;
import oshi.software.os.ApplicationInfo;
import oshi.software.os.FileSystem;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.software.os.OSService;
import oshi.software.os.OSThread;
import oshi.util.ExecutingCommand;
import oshi.util.Memoizer;
import oshi.util.ParseUtil;
import oshi.util.Util;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Quartet;

/**
 * FFM-backed AIX OperatingSystem.
 */
@ThreadSafe
public final class AixOperatingSystemFFM extends AbstractOperatingSystem {

    private final Supplier<PerfstatConfigFFM.PartitionConfig> config = memoize(PerfstatConfigFFM::queryConfig);
    private final Supplier<PerfstatProcessFFM.ProcessInfo[]> procCpu = memoize(PerfstatProcessFFM::queryProcesses,
            defaultExpiration());
    private final Supplier<List<ApplicationInfo>> installedAppsSupplier = Memoizer
            .memoize(AixInstalledApps::queryInstalledApps, installedAppsExpiration());

    private static final long BOOTTIME = querySystemBootTimeMillis() / 1000L;

    @Override
    public String queryManufacturer() {
        return "IBM";
    }

    @Override
    public Pair<String, OSVersionInfo> queryFamilyVersionInfo() {
        PerfstatConfigFFM.PartitionConfig cfg = config.get();
        String systemName = System.getProperty("os.name");
        String archName = System.getProperty("os.arch");
        String versionNumber = System.getProperty("os.version");
        if (Util.isBlank(versionNumber)) {
            versionNumber = ExecutingCommand.getFirstAnswer("oslevel");
        }
        String releaseNumber = cfg.OSBuild;
        if (Util.isBlank(releaseNumber)) {
            releaseNumber = ExecutingCommand.getFirstAnswer("oslevel -s");
        } else {
            int idx = releaseNumber.lastIndexOf(' ');
            if (idx > 0 && idx < releaseNumber.length()) {
                releaseNumber = releaseNumber.substring(idx + 1);
            }
        }
        return new Pair<>(systemName, new OSVersionInfo(versionNumber, archName, releaseNumber));
    }

    @Override
    protected int queryBitness(int jvmBitness) {
        if (jvmBitness == 64) {
            return 64;
        }
        return (config.get().conf & 0x0080_0000) > 0 ? 64 : 32;
    }

    @Override
    public FileSystem getFileSystem() {
        return new AixFileSystem();
    }

    @Override
    public InternetProtocolStats getInternetProtocolStats() {
        return new AixInternetProtocolStatsFFM();
    }

    @Override
    public List<OSProcess> queryAllProcesses() {
        return getProcessListFromProcfs(-1);
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
        List<OSProcess> procs = getProcessListFromProcfs(pid);
        return procs.isEmpty() ? null : procs.get(0);
    }

    private List<OSProcess> getProcessListFromProcfs(int pid) {
        List<OSProcess> procs = new ArrayList<>();
        PerfstatProcessFFM.ProcessInfo[] perfstat = procCpu.get();
        Map<Integer, Quartet<Long, Long, Long, Long>> cpuMemMap = new HashMap<>();
        for (PerfstatProcessFFM.ProcessInfo stat : perfstat) {
            int statpid = (int) stat.pid;
            if (pid < 0 || statpid == pid) {
                cpuMemMap.put(statpid, new Quartet<>((long) stat.ucpu_time, (long) stat.scpu_time,
                        stat.real_inuse * 1024L, (stat.proc_real_mem_data + stat.proc_real_mem_text) * 1024L));
            }
        }
        for (Entry<Integer, Quartet<Long, Long, Long, Long>> entry : cpuMemMap.entrySet()) {
            OSProcess proc = new AixOSProcessFFM(entry.getKey(), entry.getValue(), procCpu, this);
            if (proc.getState() != INVALID) {
                procs.add(proc);
            }
        }
        return procs;
    }

    @Override
    public int getProcessId() {
        try {
            return AixLibcFunctions.getpid();
        } catch (Throwable _) {
            return 0;
        }
    }

    @Override
    public int getProcessCount() {
        return procCpu.get().length;
    }

    @Override
    public int getThreadId() {
        try {
            return AixLibcFunctions.thread_self();
        } catch (Throwable _) {
            return 0;
        }
    }

    @Override
    public OSThread getCurrentThread() {
        OSProcess proc = getCurrentProcess();
        final int tid = getThreadId();
        return proc.getThreadDetails().stream().filter(t -> t.getThreadId() == tid).findFirst()
                .orElse(new AixOSThread(proc.getProcessID(), tid));
    }

    @Override
    public int getThreadCount() {
        long tc = 0L;
        for (PerfstatProcessFFM.ProcessInfo proc : procCpu.get()) {
            tc += proc.num_threads;
        }
        return (int) tc;
    }

    @Override
    public long getSystemUptime() {
        return System.currentTimeMillis() / 1000L - BOOTTIME;
    }

    @Override
    public long getSystemBootTime() {
        return BOOTTIME;
    }

    private static long querySystemBootTimeMillis() {
        long bootTime = Who.queryBootTime();
        if (bootTime >= 1000L) {
            return bootTime;
        }
        return System.currentTimeMillis() - Uptime.queryUpTime();
    }

    @Override
    public NetworkParams getNetworkParams() {
        return new AixNetworkParamsFFM();
    }

    @Override
    public List<OSService> getServices() {
        List<OSService> services = new ArrayList<>();
        List<String> systemServicesInfoList = ExecutingCommand.runNative("lssrc -a");
        if (systemServicesInfoList.size() > 1) {
            systemServicesInfoList.remove(0);
            for (String systemService : systemServicesInfoList) {
                String[] serviceSplit = ParseUtil.whitespaces.split(systemService.trim());
                if (systemService.contains("active")) {
                    if (serviceSplit.length == 4) {
                        services.add(new OSService(serviceSplit[0], ParseUtil.parseIntOrDefault(serviceSplit[2], 0),
                                RUNNING));
                    } else if (serviceSplit.length == 3) {
                        services.add(new OSService(serviceSplit[0], ParseUtil.parseIntOrDefault(serviceSplit[1], 0),
                                RUNNING));
                    }
                } else if (systemService.contains("inoperative")) {
                    services.add(new OSService(serviceSplit[0], 0, STOPPED));
                }
            }
        }
        File dir = new File("/etc/rc.d/init.d");
        File[] listFiles;
        if (dir.exists() && dir.isDirectory() && (listFiles = dir.listFiles()) != null) {
            for (File file : listFiles) {
                String installedService = ExecutingCommand.getFirstAnswer(file.getAbsolutePath() + " status");
                if (installedService.contains("running")) {
                    services.add(new OSService(file.getName(), ParseUtil.parseLastInt(installedService, 0), RUNNING));
                } else {
                    services.add(new OSService(file.getName(), 0, STOPPED));
                }
            }
        }
        return services;
    }

    @Override
    public List<ApplicationInfo> getInstalledApplications() {
        return installedAppsSupplier.get();
    }
}
