/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.netbsd;

import static oshi.software.os.OSProcess.State.INVALID;
import static oshi.software.os.OSThread.ThreadFiltering.VALID_THREAD;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.os.unix.bsd.BsdOSProcess;
import oshi.software.common.os.unix.netbsd.NetBsdOperatingSystem.PsKeywords;
import oshi.software.os.OSThread;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.common.platform.unix.netbsd.FstatUtil;
import oshi.util.common.platform.unix.netbsd.NetBsdSysctlUtil;

/**
 * OSProcess implementation
 */
@ThreadSafe
public class NetBsdOSProcess extends BsdOSProcess {

    private static final Logger LOG = LoggerFactory.getLogger(NetBsdOSProcess.class);

    /*
     * Package-private for use by NetBsdOSThread
     */
    enum PsThreadColumns {
        LID, STATE, ETIME, CPUTIME, NIVCSW, NVCSW, MAJFLT, MINFLT, PRI, ARGS;
    }

    static final String PS_THREAD_COLUMNS = Arrays.stream(PsThreadColumns.values()).map(Enum::name)
            .map(name -> name.toLowerCase(Locale.ROOT)).collect(Collectors.joining(","));

    private static final int ARGMAX = NetBsdSysctlUtil.sysctl("kern.argmax", 0);

    private final NetBsdOperatingSystem os;

    public NetBsdOSProcess(int pid, Map<PsKeywords, String> psMap, NetBsdOperatingSystem os) {
        super(pid);
        this.os = os;
        updateThreadCount();
        updateAttributes(psMap);
    }

    @Override
    protected List<String> queryArguments() {
        // NetBSD provides command line via /proc filesystem
        byte[] cmdBytes = FileUtil.readAllBytes("/proc/" + getProcessID() + "/cmdline", false);
        if (cmdBytes != null && cmdBytes.length > 0) {
            return Collections.unmodifiableList(ParseUtil.parseByteArrayToStrings(cmdBytes));
        }
        return Collections.emptyList();
    }

    @Override
    protected Map<String, String> queryEnvironmentVariables() {
        // For the current process, use Java's System.getenv()
        if (getProcessID() == this.os.getProcessId()) {
            return System.getenv();
        }
        // Environment of other processes is not accessible without JNA on NetBSD
        return Collections.emptyMap();
    }

    @Override
    public String getCurrentWorkingDirectory() {
        return FstatUtil.getCwd(getProcessID());
    }

    @Override
    public long getOpenFiles() {
        return FstatUtil.getOpenFiles(getProcessID());
    }

    @Override
    public long getSoftOpenFileLimit() {
        if (getProcessID() == this.os.getProcessId()) {
            long limit = NetBsdSysctlUtil.sysctl("kern.maxfilesperproc", 0L);
            if (limit <= 0) {
                limit = NetBsdSysctlUtil.sysctl("kern.maxfiles", 0L);
            }
            return limit > 0 ? limit : -1L;
        }
        return -1L;
    }

    @Override
    public long getHardOpenFileLimit() {
        if (getProcessID() == this.os.getProcessId()) {
            long limit = NetBsdSysctlUtil.sysctl("kern.maxfiles", 0L);
            return limit > 0 ? limit : -1L;
        }
        return -1L;
    }

    @Override
    protected int queryBitness() {
        // NetBSD does not maintain a compatibility layer.
        // Process bitness is OS bitness
        return System.getProperty("os.arch", "").contains("64") ? 64 : 32;
    }

    @Override
    public long getAffinityMask() {
        long bitMask = 0L;
        // schedctl -p <pid> shows affinity; -A cpus would set it
        List<String> schedctl = ExecutingCommand.runNative("schedctl -p " + getProcessID());
        for (String line : schedctl) {
            // Output includes "Affinity: <list>" when bound
            if (line.contains("Affinity:")) {
                String[] parts = line.split("Affinity:")[1].trim().split("[,\\s]+");
                for (String part : parts) {
                    int bitToSet = ParseUtil.parseIntOrDefault(part.trim(), -1);
                    if (bitToSet >= 0) {
                        bitMask |= 1L << bitToSet;
                    }
                }
                return bitMask;
            }
        }
        // Not bound — return all CPUs
        int ncpu = NetBsdSysctlUtil.sysctl("hw.ncpuonline", 1);
        return ncpu < 64 ? (1L << ncpu) - 1 : -1L;
    }

    @Override
    public List<OSThread> getThreadDetails() {
        // NetBSD ps shows per-LWP rows when lid is in the format specifier
        String psCommand = "ps -awwxo " + PS_THREAD_COLUMNS;
        if (getProcessID() >= 0) {
            psCommand += " -p " + getProcessID();
        }
        Predicate<Map<PsThreadColumns, String>> hasColumnsArgs = threadMap -> threadMap
                .containsKey(PsThreadColumns.ARGS);
        return ExecutingCommand.runNative(psCommand).stream().skip(1)
                .map(thread -> ParseUtil.stringToEnumMap(PsThreadColumns.class, thread.trim(), ' '))
                .filter(hasColumnsArgs).map(threadMap -> new NetBsdOSThread(getProcessID(), threadMap))
                .filter(VALID_THREAD).collect(Collectors.toList());
    }

    @Override
    public boolean updateAttributes() {
        // 'ps' does not provide threadCount or kernelTime on NetBSD
        String psCommand = "ps -awwxo " + NetBsdOperatingSystem.PS_COMMAND_ARGS + " -p " + getProcessID();
        List<String> procList = ExecutingCommand.runNative(psCommand);
        if (procList.size() > 1) {
            // skip header row
            Map<PsKeywords, String> psMap = ParseUtil.stringToEnumMap(PsKeywords.class, procList.get(1).trim(), ' ');
            // Check if last (thus all) value populated
            if (psMap.containsKey(PsKeywords.ARGS)) {
                updateThreadCount();
                return updateAttributes(psMap);
            }
        }
        this.state = INVALID;
        return false;
    }

    private boolean updateAttributes(Map<PsKeywords, String> psMap) {
        long now = System.currentTimeMillis();
        this.state = getStateFromOutput(psMap.get(PsKeywords.STATE).charAt(0));
        this.parentProcessID = ParseUtil.parseIntOrDefault(psMap.get(PsKeywords.PPID), 0);
        this.user = psMap.get(PsKeywords.USER);
        this.userID = psMap.get(PsKeywords.UID);
        this.group = psMap.get(PsKeywords.GROUP);
        this.groupID = psMap.get(PsKeywords.GID);
        this.priority = ParseUtil.parseIntOrDefault(psMap.get(PsKeywords.PRI), 0);
        // These are in KB, multiply
        this.virtualSize = ParseUtil.parseLongOrDefault(psMap.get(PsKeywords.VSZ), 0) * 1024;
        this.residentSetSize = ParseUtil.parseLongOrDefault(psMap.get(PsKeywords.RSS), 0) * 1024;
        // Avoid divide by zero for processes up less than a second
        long elapsedTime = ParseUtil.parseDHMSOrDefault(psMap.get(PsKeywords.ETIME), 0L);
        this.upTime = elapsedTime < 1L ? 1L : elapsedTime;
        this.startTime = now - this.upTime;
        this.userTime = ParseUtil.parseDHMSOrDefault(psMap.get(PsKeywords.CPUTIME), 0L);
        // kernel time is included in user time
        this.kernelTime = 0L;
        this.path = psMap.get(PsKeywords.COMM);
        this.name = this.path.substring(this.path.lastIndexOf('/') + 1);
        this.minorFaults = ParseUtil.parseLongOrDefault(psMap.get(PsKeywords.MINFLT), 0L);
        this.majorFaults = ParseUtil.parseLongOrDefault(psMap.get(PsKeywords.MAJFLT), 0L);
        long nonVoluntaryContextSwitches = ParseUtil.parseLongOrDefault(psMap.get(PsKeywords.NIVCSW), 0L);
        long voluntaryContextSwitches = ParseUtil.parseLongOrDefault(psMap.get(PsKeywords.NVCSW), 0L);
        this.voluntaryContextSwitches = voluntaryContextSwitches;
        this.involuntaryContextSwitches = nonVoluntaryContextSwitches;
        this.commandLineBackup = psMap.get(PsKeywords.ARGS);
        return true;
    }

    private void updateThreadCount() {
        // Use nlwp keyword to get LWP count for this process
        List<String> nlwpList = ExecutingCommand.runNative("ps -o nlwp -p " + getProcessID());
        if (nlwpList.size() > 1) {
            this.threadCount = ParseUtil.parseIntOrDefault(nlwpList.get(1).trim(), 1);
        } else {
            this.threadCount = 1;
        }
    }
}
