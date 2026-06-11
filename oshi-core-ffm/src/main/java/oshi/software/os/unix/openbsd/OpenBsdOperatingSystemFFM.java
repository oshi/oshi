/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.openbsd;

import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.CTL_KERN;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.KERN_OSRELEASE;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.KERN_OSTYPE;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.KERN_VERSION;
import static oshi.software.os.OSService.State.RUNNING;
import static oshi.software.os.OSService.State.STOPPED;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.ForeignFunctions;
import oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions;
import oshi.ffm.util.platform.unix.openbsd.OpenBsdSysctlUtilFFM;
import oshi.software.common.AbstractOperatingSystem;
import oshi.software.common.os.unix.bsd.BsdPsKeyword;
import oshi.software.common.os.unix.openbsd.OpenBsdInternetProtocolStats;
import oshi.software.common.os.unix.openbsd.OpenBsdNetworkParams;
import oshi.software.common.os.unix.openbsd.OpenBsdOSProcess;
import oshi.software.common.os.unix.openbsd.OpenBsdOSThread;
import oshi.software.os.FileSystem;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.software.os.OSService;
import oshi.software.os.OSThread;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;

@ThreadSafe
public class OpenBsdOperatingSystemFFM extends AbstractOperatingSystem {

    private static final Logger LOG = LoggerFactory.getLogger(OpenBsdOperatingSystemFFM.class);

    private static final long BOOTTIME = querySystemBootTime();

    @Override
    public String queryManufacturer() {
        return "Unix/BSD";
    }

    @Override
    public Pair<String, OSVersionInfo> queryFamilyVersionInfo() {
        int[] mibType = { CTL_KERN, KERN_OSTYPE };
        String family = OpenBsdSysctlUtilFFM.sysctl(mibType, "OpenBSD");

        int[] mibRelease = { CTL_KERN, KERN_OSRELEASE };
        String version = OpenBsdSysctlUtilFFM.sysctl(mibRelease, "");

        int[] mibVersion = { CTL_KERN, KERN_VERSION };
        String versionInfo = OpenBsdSysctlUtilFFM.sysctl(mibVersion, "");
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
        return new OpenBsdFileSystemFFM();
    }

    @Override
    public InternetProtocolStats getInternetProtocolStats() {
        return new OpenBsdInternetProtocolStats();
    }

    @Override
    public List<OSProcess> queryAllProcesses() {
        return getProcessListFromPS(-1);
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

    @Override
    public OSProcess getProcess(int pid) {
        List<OSProcess> procs = getProcessListFromPS(pid);
        if (procs.isEmpty()) {
            return null;
        }
        return procs.get(0);
    }

    private List<OSProcess> getProcessListFromPS(int pid) {
        String psCommand = "ps -awwxo " + OpenBsdOSProcess.PS_COMMAND_ARGS;
        if (pid >= 0) {
            psCommand += " -p " + pid;
        }
        List<String> procList = ExecutingCommand.runNative(psCommand);
        if (procList.size() < 2) {
            return new ArrayList<>();
        }
        procList.remove(0);
        Predicate<Map<BsdPsKeyword, String>> hasKeywordArgs = psMap -> psMap.containsKey(BsdPsKeyword.ARGS);
        List<OSProcess> procs = new ArrayList<>();
        for (String proc : procList) {
            Map<BsdPsKeyword, String> psMap = ParseUtil.stringToEnumMap(BsdPsKeyword.class,
                    OpenBsdOSProcess.PS_KEYWORDS, proc.trim(), ' ');
            if (hasKeywordArgs.test(psMap)) {
                procs.add(new OpenBsdOSProcessFFM(
                        pid < 0 ? ParseUtil.parseIntOrDefault(psMap.get(BsdPsKeyword.PID), 0) : pid, psMap, this));
            }
        }
        return procs;
    }

    @Override
    public int getProcessId() {
        return ForeignFunctions.callInArenaIntOrDefault(arena -> OpenBsdLibcFunctions.getpid(), LOG, Level.WARN,
                "getpid failed", 0);
    }

    @Override
    public int getProcessCount() {
        List<String> procList = ExecutingCommand.runNative("ps -axo pid");
        if (!procList.isEmpty()) {
            return procList.size() - 1;
        }
        return 0;
    }

    @Override
    public int getThreadId() {
        return ForeignFunctions.callInArenaIntOrDefault(arena -> OpenBsdLibcFunctions.getthrid(), LOG, Level.WARN,
                "getthrid failed", 0);
    }

    @Override
    public OSThread getCurrentThread() {
        OSProcess proc = getCurrentProcess();
        final int tid = getThreadId();
        return proc.getThreadDetails().stream().filter(t -> t.getThreadId() == tid).findFirst()
                .orElse(new OpenBsdOSThread(proc.getProcessID(), tid));
    }

    @Override
    public int getThreadCount() {
        List<String> threadList = ExecutingCommand.runNative("ps -axHo tid");
        if (!threadList.isEmpty()) {
            return threadList.size() - 1;
        }
        return 0;
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
        return ParseUtil.parseLongOrDefault(
                ExecutingCommand.getFirstAnswer("sysctl -n kern.boottime").split(",")[0].replaceAll("\\D", ""),
                System.currentTimeMillis() / 1000);
    }

    @Override
    public NetworkParams getNetworkParams() {
        return new OpenBsdNetworkParams();
    }

    @Override
    public List<OSService> getServices() {
        List<OSService> services = new ArrayList<>();
        Set<String> running = new HashSet<>();
        for (OSProcess p : getChildProcesses(1, ProcessFiltering.ALL_PROCESSES, ProcessSorting.PID_ASC, 0)) {
            OSService s = new OSService(p.getName(), p.getProcessID(), RUNNING);
            services.add(s);
            running.add(p.getName());
        }
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
