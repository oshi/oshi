/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.solaris;

import static oshi.software.os.OSProcess.State.INVALID;
import static oshi.software.os.OSService.State.RUNNING;
import static oshi.software.os.OSService.State.STOPPED;

import java.io.File;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.ForeignFunctions;
import oshi.ffm.platform.unix.solaris.LibKstatFunctions;
import oshi.ffm.platform.unix.solaris.SolarisLibcFunctions;
import oshi.ffm.util.platform.unix.solaris.KstatUtilFFM;
import oshi.ffm.util.platform.unix.solaris.KstatUtilFFM.KstatChain;
import oshi.software.common.AbstractOperatingSystem;
import oshi.software.common.os.unix.solaris.SolarisInternetProtocolStats;
import oshi.software.os.FileSystem;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.software.os.OSService;
import oshi.software.os.OSThread;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.driver.linux.proc.ProcessStat;
import oshi.util.tuples.Pair;

@ThreadSafe
public class SolarisOperatingSystemFFM extends AbstractOperatingSystem {

    private static final Logger LOG = LoggerFactory.getLogger(SolarisOperatingSystemFFM.class);

    private static final String VERSION;
    private static final String BUILD_NUMBER;
    static {
        String[] split = ParseUtil.whitespaces.split(ExecutingCommand.getFirstAnswer("uname -rv"));
        VERSION = split[0];
        BUILD_NUMBER = split.length > 1 ? split[1] : "";
    }

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
        return new SolarisFileSystemFFM();
    }

    @Override
    public InternetProtocolStats getInternetProtocolStats() {
        return new SolarisInternetProtocolStats();
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
        return getProcessListFromProcfs(-1);
    }

    @Override
    public List<OSProcess> queryChildProcesses(int parentPid) {
        List<OSProcess> allProcs = queryAllProcesses();
        Set<Integer> descendantPids = getChildrenOrDescendants(allProcs, parentPid, false);
        return allProcs.stream().filter(p -> descendantPids.contains(p.getProcessID())).toList();
    }

    @Override
    public List<OSProcess> queryDescendantProcesses(int parentPid) {
        List<OSProcess> allProcs = queryAllProcesses();
        Set<Integer> descendantPids = getChildrenOrDescendants(allProcs, parentPid, true);
        return allProcs.stream().filter(p -> descendantPids.contains(p.getProcessID())).toList();
    }

    private List<OSProcess> getProcessListFromProcfs(int pid) {
        List<OSProcess> procs = new ArrayList<>();

        File[] numericFiles = null;
        if (pid < 0) {
            File directory = new File("/proc");
            numericFiles = directory.listFiles(file -> Constants.DIGITS.matcher(file.getName()).matches());
        } else {
            File pidFile = new File("/proc/" + pid);
            if (pidFile.exists()) {
                numericFiles = new File[1];
                numericFiles[0] = pidFile;
            }
        }
        if (numericFiles == null) {
            return procs;
        }

        for (File pidFile : numericFiles) {
            int pidNum = ParseUtil.parseIntOrDefault(pidFile.getName(), 0);
            OSProcess proc = new SolarisOSProcessFFM(pidNum, this);
            if (proc.getState() != INVALID) {
                procs.add(proc);
            }
        }
        return procs;
    }

    @Override
    public int getProcessId() {
        return ForeignFunctions.callInArenaIntOrDefault(arena -> SolarisLibcFunctions.getpid(), LOG, Level.WARN,
                "getpid failed", 0);
    }

    @Override
    public int getProcessCount() {
        return ProcessStat.getPidFiles().length;
    }

    @Override
    public int getThreadId() {
        return ForeignFunctions.callInArenaIntOrDefault(arena -> SolarisLibcFunctions.thr_self(), LOG, Level.WARN,
                "thr_self failed", 0);
    }

    @Override
    public OSThread getCurrentThread() {
        return new SolarisOSThreadFFM(getProcessId(), getThreadId());
    }

    @Override
    public int getThreadCount() {
        List<String> threadList = ExecutingCommand.runNative("ps -eLo pid");
        if (!threadList.isEmpty()) {
            return threadList.size() - 1;
        }
        return getProcessCount();
    }

    @Override
    public long getSystemUptime() {
        try (KstatChain kc = KstatUtilFFM.openChain()) {
            MemorySegment ksp = kc.lookup("unix", 0, "system_misc");
            if (ksp.address() != 0L && kc.read(ksp)) {
                return LibKstatFunctions.kstatSnaptime(ksp) / 1_000_000_000L;
            }
        }
        return 0L;
    }

    @Override
    public long getSystemBootTime() {
        return BOOTTIME;
    }

    private static long querySystemBootTime() {
        try (KstatChain kc = KstatUtilFFM.openChain()) {
            MemorySegment ksp = kc.lookup("unix", 0, "system_misc");
            if (ksp.address() != 0L && kc.read(ksp)) {
                return KstatUtilFFM.dataLookupLong(ksp, "boot_time");
            }
        }
        return System.currentTimeMillis() / 1000L;
    }

    @Override
    public NetworkParams getNetworkParams() {
        return new SolarisNetworkParamsFFM();
    }

    @Override
    public List<OSService> getServices() {
        List<OSService> services = new ArrayList<>();
        List<String> legacySvcs = new ArrayList<>();
        File dir = new File("/etc/init.d");
        File[] listFiles;
        if (dir.exists() && dir.isDirectory() && (listFiles = dir.listFiles()) != null) {
            for (File f : listFiles) {
                legacySvcs.add(f.getName());
            }
        }
        List<String> svcs = ExecutingCommand.runNative("svcs -p");
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
