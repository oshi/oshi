/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.aix;

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
import oshi.driver.common.unix.aix.AixPerfstatProcess;
import oshi.driver.common.unix.aix.Uptime;
import oshi.driver.common.unix.aix.Who;
import oshi.software.common.AbstractOperatingSystem;
import oshi.software.os.ApplicationInfo;
import oshi.software.os.FileSystem;
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
 * Abstract base for the AIX OperatingSystem. Holds the command-line service/version logic, the perfstat-driven process
 * and thread enumeration (operating on the backend-neutral {@link AixPerfstatProcess} carrier), and the boot-time and
 * installed-application queries. The JNA and FFM subclasses supply the perfstat/partition-config reads, the native
 * process/thread IDs, and the {@code OSProcess}/{@code InternetProtocolStats}/{@code NetworkParams} factories.
 */
@ThreadSafe
public abstract class AixOperatingSystem extends AbstractOperatingSystem {

    private final Supplier<AixPerfstatProcess[]> procCpu = memoize(this::queryPerfstatProcesses, defaultExpiration());
    private final Supplier<List<ApplicationInfo>> installedAppsSupplier = Memoizer
            .memoize(AixInstalledApps::queryInstalledApps, installedAppsExpiration());

    private static final long BOOTTIME = querySystemBootTimeMillis() / 1000L;

    @Override
    public String queryManufacturer() {
        return "IBM";
    }

    @Override
    public Pair<String, OSVersionInfo> queryFamilyVersionInfo() {
        String systemName = System.getProperty("os.name");
        String archName = System.getProperty("os.arch");
        String versionNumber = System.getProperty("os.version");
        if (Util.isBlank(versionNumber)) {
            versionNumber = ExecutingCommand.getFirstAnswer("oslevel");
        }
        String releaseNumber = queryOsBuildRaw();
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
        return is64BitKernel() ? 64 : 32;
    }

    @Override
    public FileSystem getFileSystem() {
        return new AixFileSystem();
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
        AixPerfstatProcess[] perfstat = procCpu.get();
        Map<Integer, Quartet<Long, Long, Long, Long>> cpuMemMap = new HashMap<>();
        for (AixPerfstatProcess stat : perfstat) {
            int statpid = (int) stat.pid;
            if (pid < 0 || statpid == pid) {
                cpuMemMap.put(statpid, new Quartet<>((long) stat.ucpu_time, (long) stat.scpu_time,
                        stat.real_inuse * 1024L, (stat.proc_real_mem_data + stat.proc_real_mem_text) * 1024L));
            }
        }
        for (Entry<Integer, Quartet<Long, Long, Long, Long>> entry : cpuMemMap.entrySet()) {
            OSProcess proc = createProcess(entry.getKey(), entry.getValue(), procCpu);
            if (proc.getState() != INVALID) {
                procs.add(proc);
            }
        }
        return procs;
    }

    @Override
    public int getProcessCount() {
        return procCpu.get().length;
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
        for (AixPerfstatProcess proc : procCpu.get()) {
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
    public List<OSService> getServices() {
        List<OSService> services = parseServices(ExecutingCommand.runNative("lssrc -a"));
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

    /**
     * Parses {@code lssrc -a} output into the list of subsystem services. The first row is the column header.
     *
     * @param lssrc the lines of {@code lssrc -a} output
     * @return the parsed services (active subsystems as {@code RUNNING}, inoperative as {@code STOPPED})
     */
    static List<OSService> parseServices(List<String> lssrc) {
        List<OSService> services = new ArrayList<>();
        boolean header = true;
        for (String systemService : lssrc) {
            // Skip the "Subsystem Group PID Status" header row without mutating the input list
            if (header) {
                header = false;
                continue;
            }
            String[] serviceSplit = ParseUtil.whitespaces.split(systemService.trim());
            if (systemService.contains("active")) {
                if (serviceSplit.length == 4) {
                    services.add(
                            new OSService(serviceSplit[0], ParseUtil.parseIntOrDefault(serviceSplit[2], 0), RUNNING));
                } else if (serviceSplit.length == 3) {
                    services.add(
                            new OSService(serviceSplit[0], ParseUtil.parseIntOrDefault(serviceSplit[1], 0), RUNNING));
                }
            } else if (systemService.contains("inoperative")) {
                services.add(new OSService(serviceSplit[0], 0, STOPPED));
            }
        }
        return services;
    }

    @Override
    public List<ApplicationInfo> getInstalledApplications() {
        return installedAppsSupplier.get();
    }

    /**
     * Queries per-process usage statistics via {@code perfstat_process}.
     *
     * @return one {@link AixPerfstatProcess} per process
     */
    protected abstract AixPerfstatProcess[] queryPerfstatProcesses();

    /**
     * Returns the raw {@code OSBuild} string from the partition configuration, or an empty string if unavailable.
     *
     * @return the raw OS build string
     */
    protected abstract String queryOsBuildRaw();

    /**
     * Returns whether the kernel is 64-bit, derived from the partition configuration {@code conf} flags.
     *
     * @return {@code true} for a 64-bit kernel
     */
    protected abstract boolean is64BitKernel();

    /**
     * Creates a platform-specific {@code OSProcess} for the given process.
     *
     * @param pid     the process ID
     * @param cpuMem  the perfstat-derived (userTime, kernelTime, residentSetSize, privateResidentMemory) quartet
     * @param procCpu the memoized perfstat process array, used by the process to refresh its own attributes
     * @return the OSProcess
     */
    protected abstract OSProcess createProcess(int pid, Quartet<Long, Long, Long, Long> cpuMem,
            Supplier<AixPerfstatProcess[]> procCpu);
}
