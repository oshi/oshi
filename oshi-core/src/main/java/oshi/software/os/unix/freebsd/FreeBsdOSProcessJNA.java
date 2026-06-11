/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.freebsd;

import static oshi.software.os.OSProcess.State.INVALID;
import static oshi.software.os.OSThread.ThreadFiltering.VALID_THREAD;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.platform.unix.LibCAPI.size_t;
import com.sun.jna.platform.unix.Resource;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.ByRef.CloseableSizeTByReference;
import oshi.jna.platform.unix.FreeBsdLibc;
import oshi.software.common.os.unix.freebsd.FreeBsdOSProcess;
import oshi.software.common.os.unix.freebsd.FreeBsdOSThread;
import oshi.software.os.OSThread;
import oshi.software.os.unix.freebsd.FreeBsdOperatingSystemJNA.PsKeywords;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.common.platform.unix.freebsd.ProcstatUtil;
import oshi.util.platform.unix.freebsd.BsdSysctlUtil;

/**
 * JNA-backed FreeBSD OSProcess.
 */
@ThreadSafe
public class FreeBsdOSProcessJNA extends FreeBsdOSProcess {

    private static final Logger LOG = LoggerFactory.getLogger(FreeBsdOSProcessJNA.class);

    private static final int ARGMAX = BsdSysctlUtil.sysctl("kern.argmax", 0);

    private final FreeBsdOperatingSystemJNA os;

    public FreeBsdOSProcessJNA(int pid, Map<PsKeywords, String> psMap, FreeBsdOperatingSystemJNA os) {
        super(pid);
        this.os = os;
        updateAttributes(psMap);
    }

    @Override
    protected List<String> queryArguments() {
        if (ARGMAX > 0) {
            // Get arguments via sysctl(3)
            int[] mib = new int[4];
            mib[0] = 1; // CTL_KERN
            mib[1] = 14; // KERN_PROC
            mib[2] = 7; // KERN_PROC_ARGS
            mib[3] = getProcessID();
            // Allocate memory for arguments
            try (Memory m = new Memory(ARGMAX);
                    CloseableSizeTByReference size = new CloseableSizeTByReference(ARGMAX)) {
                // Fetch arguments
                if (FreeBsdLibc.INSTANCE.sysctl(mib, mib.length, m, size, null, size_t.ZERO) == 0) {
                    return Collections.unmodifiableList(
                            ParseUtil.parseByteArrayToStrings(m.getByteArray(0, size.getValue().intValue())));
                } else {
                    LOG.warn(
                            "Failed sysctl call for process arguments (kern.proc.args), process {} may not exist. Error code: {}",
                            getProcessID(), Native.getLastError());
                }
            }
        }
        return Collections.emptyList();
    }

    @Override
    protected Map<String, String> queryEnvironmentVariables() {
        if (ARGMAX > 0) {
            // Get environment variables via sysctl(3)
            int[] mib = new int[4];
            mib[0] = 1; // CTL_KERN
            mib[1] = 14; // KERN_PROC
            mib[2] = 35; // KERN_PROC_ENV
            mib[3] = getProcessID();
            // Allocate memory for environment variables
            try (Memory m = new Memory(ARGMAX);
                    CloseableSizeTByReference size = new CloseableSizeTByReference(ARGMAX)) {
                // Fetch environment variables
                if (FreeBsdLibc.INSTANCE.sysctl(mib, mib.length, m, size, null, size_t.ZERO) == 0) {
                    return Collections.unmodifiableMap(
                            ParseUtil.parseByteArrayToStringMap(m.getByteArray(0, size.getValue().intValue())));
                } else {
                    LOG.warn(
                            "Failed sysctl call for process environment variables (kern.proc.env), process {} may not exist. Error code: {}",
                            getProcessID(), Native.getLastError());
                }
            }
        }
        return Collections.emptyMap();
    }

    @Override
    public String getCurrentWorkingDirectory() {
        return ProcstatUtil.getCwd(getProcessID());
    }

    @Override
    public long getOpenFiles() {
        return ProcstatUtil.getOpenFiles(getProcessID());
    }

    @Override
    public long getSoftOpenFileLimit() {
        if (getProcessID() == this.os.getProcessId()) {
            final Resource.Rlimit rlimit = new Resource.Rlimit();
            FreeBsdLibc.INSTANCE.getrlimit(FreeBsdLibc.RLIMIT_NOFILE, rlimit);
            return rlimit.rlim_cur;
        } else {
            return getProcessOpenFileLimit(getProcessID(), 1);
        }
    }

    @Override
    public long getHardOpenFileLimit() {
        if (getProcessID() == this.os.getProcessId()) {
            final Resource.Rlimit rlimit = new Resource.Rlimit();
            FreeBsdLibc.INSTANCE.getrlimit(FreeBsdLibc.RLIMIT_NOFILE, rlimit);
            return rlimit.rlim_max;
        } else {
            return getProcessOpenFileLimit(getProcessID(), 2);
        }
    }

    @Override
    protected int queryBitness() {
        // Get process abi vector
        int[] mib = new int[4];
        mib[0] = 1; // CTL_KERN
        mib[1] = 14; // KERN_PROC
        mib[2] = 9; // KERN_PROC_SV_NAME
        mib[3] = getProcessID();
        // Allocate memory for arguments
        try (Memory abi = new Memory(32); CloseableSizeTByReference size = new CloseableSizeTByReference(32)) {
            // Fetch abi vector
            if (0 == FreeBsdLibc.INSTANCE.sysctl(mib, mib.length, abi, size, null, size_t.ZERO)) {
                String elf = abi.getString(0);
                if (elf.contains("ELF32")) {
                    return 32;
                } else if (elf.contains("ELF64")) {
                    return 64;
                }
            }
        }
        return 0;
    }

    @Override
    public List<OSThread> getThreadDetails() {
        String psCommand = "ps -awwxo " + PS_THREAD_COLUMNS + " -H";
        if (getProcessID() >= 0) {
            psCommand += " -p " + getProcessID();
        }
        Predicate<Map<PsThreadColumns, String>> hasColumnsPri = threadMap -> threadMap.containsKey(PsThreadColumns.PRI);
        return ExecutingCommand.runNative(psCommand).stream().skip(1).parallel()
                .map(thread -> ParseUtil.stringToEnumMap(PsThreadColumns.class, thread.trim(), ' '))
                .filter(hasColumnsPri).map(threadMap -> new FreeBsdOSThread(getProcessID(), threadMap))
                .filter(VALID_THREAD).collect(Collectors.toList());
    }

    @Override
    public boolean updateAttributes() {
        String psCommand = "ps -awwxo " + FreeBsdOperatingSystemJNA.PS_COMMAND_ARGS + " -p " + getProcessID();
        List<String> procList = ExecutingCommand.runNative(psCommand);
        if (procList.size() > 1) {
            // skip header row
            Map<PsKeywords, String> psMap = ParseUtil.stringToEnumMap(PsKeywords.class, procList.get(1).trim(), ' ');
            // Check if last (thus all) value populated
            if (psMap.containsKey(PsKeywords.ARGS)) {
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
        this.threadCount = ParseUtil.parseIntOrDefault(psMap.get(PsKeywords.NLWP), 0);
        this.priority = ParseUtil.parseIntOrDefault(psMap.get(PsKeywords.PRI), 0);
        // These are in KB, multiply
        this.virtualSize = ParseUtil.parseLongOrDefault(psMap.get(PsKeywords.VSZ), 0) * 1024;
        this.residentSetSize = ParseUtil.parseLongOrDefault(psMap.get(PsKeywords.RSS), 0) * 1024;
        // Avoid divide by zero for processes up less than a second
        long elapsedTime = ParseUtil.parseDHMSOrDefault(psMap.get(PsKeywords.ETIMES), 0L);
        this.upTime = elapsedTime < 1L ? 1L : elapsedTime;
        this.startTime = now - this.upTime;
        this.kernelTime = ParseUtil.parseDHMSOrDefault(psMap.get(PsKeywords.SYSTIME), 0L);
        this.userTime = ParseUtil.parseDHMSOrDefault(psMap.get(PsKeywords.TIME), 0L) - this.kernelTime;
        this.path = psMap.get(PsKeywords.COMM);
        this.name = this.path.substring(this.path.lastIndexOf('/') + 1);
        this.minorFaults = ParseUtil.parseLongOrDefault(psMap.get(PsKeywords.MINFLT), 0L);
        this.majorFaults = ParseUtil.parseLongOrDefault(psMap.get(PsKeywords.MAJFLT), 0L);
        this.voluntaryContextSwitches = ParseUtil.parseLongOrDefault(psMap.get(PsKeywords.NVCSW), 0L);
        this.involuntaryContextSwitches = ParseUtil.parseLongOrDefault(psMap.get(PsKeywords.NIVCSW), 0L);
        this.commandLineBackup = psMap.get(PsKeywords.ARGS);
        return true;
    }
}
