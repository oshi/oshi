/*
 * Copyright 2016-2023 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.solaris;

import static oshi.software.os.OSProcess.State.INVALID;
import static oshi.software.os.OSService.State.RUNNING;
import static oshi.software.os.OSService.State.STOPPED;
import static oshi.util.Memoizer.defaultExpiration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.sun.jna.platform.unix.solaris.Kstat2;
import com.sun.jna.platform.unix.solaris.LibKstat.Kstat;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.linux.proc.ProcessStat;
import oshi.driver.unix.solaris.Who;
import oshi.jna.platform.unix.SolarisLibc;
import oshi.software.common.AbstractOperatingSystem;
import oshi.software.os.FileSystem;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.software.os.OSService;
import oshi.software.os.OSSession;
import oshi.software.os.OSThread;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.GlobalConfig;
import oshi.util.Memoizer;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.solaris.KstatUtil;
import oshi.util.platform.unix.solaris.KstatUtil.KstatChain;
import oshi.util.tuples.Pair;

/**
 * Solaris is a non-free Unix operating system originally developed by Sun Microsystems. It superseded the company's
 * earlier SunOS in 1993. In 2010, after the Sun acquisition by Oracle, it was renamed Oracle Solaris.
 */
@ThreadSafe
public class SolarisOperatingSystem extends AbstractOperatingSystem {

    private static final String VERSION;
    private static final String BUILD_NUMBER;
    static {
        String[] split = ParseUtil.whitespaces.split(ExecutingCommand.getFirstAnswer("uname -rv"));
        VERSION = split[0];
        BUILD_NUMBER = split.length > 1 ? split[1] : "";
    }

    private static final boolean ALLOW_KSTAT2 = GlobalConfig.get(GlobalConfig.OSHI_OS_SOLARIS_ALLOWKSTAT2, true);

    /**
     * This static field identifies if the kstat2 library (available in Solaris 11.4 or greater) can be loaded.
     */
    public static final boolean HAS_KSTAT2;
    static {
        Kstat2 lib = null;
        try {
            if (ALLOW_KSTAT2) {
                lib = Kstat2.INSTANCE;
            }
        } catch (UnsatisfiedLinkError e) {
            // 11.3 or earlier, no kstat2
        }
        HAS_KSTAT2 = lib != null;
    }

    private static final Supplier<Pair<Long, Long>> BOOT_UPTIME = Memoizer
            .memoize(SolarisOperatingSystem::queryBootAndUptime, defaultExpiration());

    private static final long BOOTTIME = querySystemBootTime();

    @Override
    public String queryManufacturer() {
        return "Oracle";
    }

    @Override
    public Pair<String, OSVersionInfo> queryFamilyVersionInfo() {
        return new Pair<>("SunOS", new OSVersionInfo(VERSION, "Solaris", BUILD_NUMBER));
    }

    @Override
    protected int queryBitness(int jvmBitness) {
        if (jvmBitness == 64) {
            return 64;
        }
        return ParseUtil.parseIntOrDefault(ExecutingCommand.getFirstAnswer("isainfo -b"), 32);
    }

    @Override
    public FileSystem getFileSystem() {
        return new SolarisFileSystem();
    }

    @Override
    public InternetProtocolStats getInternetProtocolStats() {
        return new SolarisInternetProtocolStats();
    }

    @Override
    public List<OSSession> getSessions() {
        return USE_WHO_COMMAND ? super.getSessions() : Who.queryUtxent();
    }

    @Override
    public OSProcess getProcess(int pid) {
        List<OSProcess> procs = getProcessListFromProcfs(pid);
        if (procs.isEmpty()) {
            return null;
        }
        return procs.get(0);
    }

    @Override
    public List<OSProcess> queryAllProcesses() {
        return queryAllProcessesFromPrStat();
    }

    @Override
    public List<OSProcess> queryChildProcesses(int parentPid) {
        List<OSProcess> allProcs = queryAllProcessesFromPrStat();
        Set<Integer> descendantPids = getChildrenOrDescendants(allProcs, parentPid, false);
        return allProcs.stream().filter(p -> descendantPids.contains(p.getProcessID())).collect(Collectors.toList());
    }

    @Override
    public List<OSProcess> queryDescendantProcesses(int parentPid) {
        List<OSProcess> allProcs = queryAllProcessesFromPrStat();
        Set<Integer> descendantPids = getChildrenOrDescendants(allProcs, parentPid, true);
        return allProcs.stream().filter(p -> descendantPids.contains(p.getProcessID())).collect(Collectors.toList());
    }

    private List<OSProcess> queryAllProcessesFromPrStat() {
        return getProcessListFromProcfs(-1);
    }

    private List<OSProcess> getProcessListFromProcfs(int pid) {
        List<OSProcess> procs = new ArrayList<>();

        File[] numericFiles = null;
        if (pid < 0) {
            // If no pid, get process files in proc
            File directory = new File("/proc");
            numericFiles = directory.listFiles(file -> Constants.DIGITS.matcher(file.getName()).matches());
        } else {
            // If pid specified just find that file
            File pidFile = new File("/proc/" + pid);
            if (pidFile.exists()) {
                numericFiles = new File[1];
                numericFiles[0] = pidFile;
            }
        }
        if (numericFiles == null) {
            return procs;
        }

        // Iterate files
        for (File pidFile : numericFiles) {
            int pidNum = ParseUtil.parseIntOrDefault(pidFile.getName(), 0);
            OSProcess proc = new SolarisOSProcess(pidNum, this);
            if (proc.getState() != INVALID) {
                procs.add(proc);
            }
        }
        return procs;
    }

    @Override
    public int getProcessId() {
        return SolarisLibc.INSTANCE.getpid();
    }

    @Override
    public int getProcessCount() {
        return ProcessStat.getPidFiles().length;
    }

    @Override
    public int getThreadId() {
        return SolarisLibc.INSTANCE.thr_self();
    }

    @Override
    public OSThread getCurrentThread() {
        return new SolarisOSThread(getProcessId(), getThreadId());
    }

    @Override
    public int getThreadCount() {
        List<String> threadList = ExecutingCommand.runNative("ps -eLo pid");
        if (!threadList.isEmpty()) {
            // Subtract 1 for header
            return threadList.size() - 1;
        }
        return getProcessCount();
    }

    @Override
    public long getSystemUptime() {
        return querySystemUptime();
    }

    private static long querySystemUptime() {
        if (HAS_KSTAT2) {
            // Use Kstat2 implementation
            return BOOT_UPTIME.get().getB();
        }
        try (KstatChain kc = KstatUtil.openChain()) {
            Kstat ksp = kc.lookup("unix", 0, "system_misc");
            if (ksp != null && kc.read(ksp)) {
                // Snap Time is in nanoseconds; divide for seconds
                return ksp.ks_snaptime / 1_000_000_000L;
            }
        }
        return 0L;
    }

    @Override
    public long getSystemBootTime() {
        return BOOTTIME;
    }

    private static long querySystemBootTime() {
        if (HAS_KSTAT2) {
            // Use Kstat2 implementation
            return BOOT_UPTIME.get().getA();
        }
        try (KstatChain kc = KstatUtil.openChain()) {
            Kstat ksp = kc.lookup("unix", 0, "system_misc");
            if (ksp != null && kc.read(ksp)) {
                return KstatUtil.dataLookupLong(ksp, "boot_time");
            }
        }
        return System.currentTimeMillis() / 1000L - querySystemUptime();
    }

    private static Pair<Long, Long> queryBootAndUptime() {
        Object[] results = KstatUtil.queryKstat2("/misc/unix/system_misc", "boot_time", "snaptime");

        long boot = results[0] == null ? System.currentTimeMillis() : (long) results[0];
        // Snap Time is in nanoseconds; divide for seconds
        long snap = results[1] == null ? 0L : (long) results[1] / 1_000_000_000L;

        return new Pair<>(boot, snap);
    }

    @Override
    public NetworkParams getNetworkParams() {
        return new SolarisNetworkParams();
    }

    @Override
    public List<OSService> getServices() {
        List<OSService> services = new ArrayList<>();
        // Get legacy RC service name possibilities
        List<String> legacySvcs = new ArrayList<>();
        File dir = new File("/etc/init.d");
        File[] listFiles;
        if (dir.exists() && dir.isDirectory() && (listFiles = dir.listFiles()) != null) {
            for (File f : listFiles) {
                legacySvcs.add(f.getName());
            }
        }
        // Iterate service list
        List<String> svcs = ExecutingCommand.runNative("svcs -p");
        /*-
         Output:
         STATE          STIME    FRMI
         legacy_run     23:56:49 lrc:/etc/rc2_d/S47pppd
         legacy_run     23:56:49 lrc:/etc/rc2_d/S81dodatadm_udaplt
         legacy_run     23:56:49 lrc:/etc/rc2_d/S89PRESERVE
         online         23:56:25 svc:/system/early-manifest-import:default
         online         23:56:25 svc:/system/svc/restarter:default
                        23:56:24       13 svc.startd
                        ...
         */
        for (String line : svcs) {
            if (line.startsWith("online")) {
                int delim = line.lastIndexOf(":/");
                if (delim > 0) {
                    String name = line.substring(delim + 1);
                    if (name.endsWith(":default")) {
                        name = name.substring(0, name.length() - 8);
                    }
                    services.add(new OSService(name, 0, STOPPED));
                }
            } else if (line.startsWith(" ")) {
                String[] split = ParseUtil.whitespaces.split(line.trim());
                if (split.length == 3) {
                    services.add(new OSService(split[2], ParseUtil.parseIntOrDefault(split[1], 0), RUNNING));
                }
            } else if (line.startsWith("legacy_run")) {
                for (String svc : legacySvcs) {
                    if (line.endsWith(svc)) {
                        services.add(new OSService(svc, 0, STOPPED));
                        break;
                    }
                }
            }
        }
        return services;
    }
}
