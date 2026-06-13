/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.solaris;

import static oshi.software.os.OSProcess.State.INVALID;
import static oshi.software.os.OSProcess.State.OTHER;
import static oshi.software.os.OSProcess.State.RUNNING;
import static oshi.software.os.OSProcess.State.SLEEPING;
import static oshi.software.os.OSProcess.State.STOPPED;
import static oshi.software.os.OSProcess.State.WAITING;
import static oshi.software.os.OSProcess.State.ZOMBIE;
import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import oshi.driver.common.unix.solaris.PsInfo;
import oshi.driver.common.unix.solaris.SolarisPrUsage;
import oshi.driver.common.unix.solaris.SolarisPsInfo;
import oshi.software.common.os.unix.AbstractProcOSProcess;
import oshi.software.os.OSThread;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.UserGroupInfo;
import oshi.util.tuples.Pair;

/**
 * Abstract base for Solaris OSProcess. The {@code /proc} parsing, command-line/env memoization, thread enumeration, and
 * field assignment are shared (via {@link AbstractProcOSProcess}); concrete subclasses (JNA/FFM) provide the
 * {@code queryArgsEnv} read, the self-process {@code rlimit} read, and the {@link OSThread} factory.
 */
public abstract class SolarisOSProcess extends AbstractProcOSProcess {

    private final Supplier<SolarisPsInfo> psinfo = memoize(this::queryPsInfo, defaultExpiration());
    private final Supplier<SolarisPrUsage> prusage = memoize(this::queryPrUsage, defaultExpiration());

    private long minorFaults;
    private long majorFaults;
    private long voluntaryContextSwitches;
    private long involuntaryContextSwitches;

    protected SolarisOSProcess(int pid) {
        super(pid);
    }

    private SolarisPsInfo queryPsInfo() {
        return PsInfo.queryPsInfo(this.getProcessID());
    }

    private SolarisPrUsage queryPrUsage() {
        return PsInfo.queryPrUsage(this.getProcessID());
    }

    /**
     * Returns the memoized {@code psinfo} for this process, for subclasses that need its arg/env offset pointers.
     *
     * @return the parsed {@link SolarisPsInfo}, or {@code null} if it could not be read
     */
    protected SolarisPsInfo getPsInfo() {
        return psinfo.get();
    }

    @Override
    protected Pair<List<String>, Map<String, String>> queryCommandlineEnvironment() {
        return queryArgsEnv();
    }

    @Override
    public long getMinorFaults() {
        return this.minorFaults;
    }

    @Override
    public long getMajorFaults() {
        return this.majorFaults;
    }

    @Override
    public long getVoluntaryContextSwitches() {
        return this.voluntaryContextSwitches;
    }

    @Override
    public long getInvoluntaryContextSwitches() {
        return this.involuntaryContextSwitches;
    }

    @Override
    public long getAffinityMask() {
        long bitMask = 0L;
        String cpuset = ExecutingCommand.getFirstAnswer("pbind -q " + getProcessID());
        // Sample output:
        // <empty string if no binding>
        // pid 101048 strongly bound to processor(s) 0 1 2 3.
        if (cpuset.isEmpty()) {
            List<String> allProcs = ExecutingCommand.runNative("psrinfo");
            for (String proc : allProcs) {
                String[] split = ParseUtil.whitespaces.split(proc);
                int bitToSet = ParseUtil.parseIntOrDefault(split[0], -1);
                if (bitToSet >= 0) {
                    bitMask |= 1L << bitToSet;
                }
            }
            return bitMask;
        } else if (cpuset.endsWith(".") && cpuset.contains("strongly bound to processor(s)")) {
            String parse = cpuset.substring(0, cpuset.length() - 1);
            String[] split = ParseUtil.whitespaces.split(parse);
            for (int i = split.length - 1; i >= 0; i--) {
                int bitToSet = ParseUtil.parseIntOrDefault(split[i], -1);
                if (bitToSet >= 0) {
                    bitMask |= 1L << bitToSet;
                } else {
                    // Once we run into the word processor(s) we're done
                    break;
                }
            }
        }
        return bitMask;
    }

    @Override
    public boolean updateAttributes() {
        SolarisPsInfo info = psinfo.get();
        if (info == null) {
            this.state = INVALID;
            return false;
        }
        SolarisPrUsage usage = prusage.get();
        long now = System.currentTimeMillis();
        this.state = getStateFromOutput((char) info.pr_lwp.pr_sname);
        this.parentProcessID = info.pr_ppid;
        this.userID = Integer.toString(info.pr_euid);
        this.user = UserGroupInfo.getUser(this.userID);
        this.groupID = Integer.toString(info.pr_egid);
        this.group = UserGroupInfo.getGroupName(this.groupID);
        this.threadCount = info.pr_nlwp;
        this.priority = info.pr_lwp.pr_pri;
        // These are in KB, multiply
        this.virtualSize = info.pr_size * 1024;
        this.residentSetSize = info.pr_rssize * 1024;
        this.privateResidentMemory = info.pr_rssizepriv * 1024;
        this.startTime = info.pr_start.toMillis();
        // Avoid divide by zero for processes up less than a millisecond
        long elapsedTime = now - this.startTime;
        this.upTime = elapsedTime < 1L ? 1L : elapsedTime;
        this.kernelTime = 0L;
        this.userTime = info.pr_time.toMillis();
        // 80 character truncation but enough for path and name (usually)
        this.commandLineBackup = PsInfo.bytesToString(info.pr_psargs);
        String[] parts = ParseUtil.whitespaces.split(commandLineBackup);
        this.path = parts.length > 0 ? parts[0] : "";
        this.name = this.path.substring(this.path.lastIndexOf('/') + 1);
        if (usage != null) {
            this.userTime = usage.pr_utime.toMillis();
            this.kernelTime = usage.pr_stime.toMillis();
            this.bytesRead = usage.pr_ioch;
            this.majorFaults = usage.pr_majf;
            this.minorFaults = usage.pr_minf;
            this.voluntaryContextSwitches = usage.pr_vctx;
            this.involuntaryContextSwitches = usage.pr_ictx;
        }
        return true;
    }

    /***
     * Returns Enum STATE for the state value obtained from status string of thread/process.
     *
     * @param stateValue state value from the status string
     * @return The state
     */
    public static State getStateFromOutput(char stateValue) {
        State state;
        switch (stateValue) {
            case 'O':
                state = RUNNING;
                break;
            case 'S':
                state = SLEEPING;
                break;
            case 'R':
            case 'W':
                state = WAITING;
                break;
            case 'Z':
                state = ZOMBIE;
                break;
            case 'T':
                state = STOPPED;
                break;
            default:
                state = OTHER;
                break;
        }
        return state;
    }

    /**
     * Parses the soft or hard open-file limit for another process from {@code plimit}.
     *
     * @param processId the process ID
     * @param index     {@code 1} for the soft limit, {@code 2} for the hard limit
     * @return the limit, or {@code -1} if unavailable
     */
    protected static long getProcessOpenFileLimit(long processId, int index) {
        final List<String> output = ExecutingCommand.runNative("plimit " + processId);
        if (output.isEmpty()) {
            return -1; // not supported
        }
        final Optional<String> nofilesLine = output.stream().filter(line -> line.trim().startsWith("nofiles"))
                .findFirst();
        if (!nofilesLine.isPresent()) {
            return -1;
        }
        // Split all non-Digits away -> ["", "{soft-limit}, "{hard-limit}"]
        // A non-numeric limit (e.g. "nofiles(descriptors) 256 unlimited") yields fewer tokens
        final String[] split = nofilesLine.get().split("\\D+");
        if (split.length <= index) {
            return -1;
        }
        return ParseUtil.parseLongOrDefault(split[index], -1);
    }

    /**
     * Reads this process's argument list and environment.
     *
     * @return a pair of (arg list, env map); either may be empty if it cannot be read
     */
    protected abstract Pair<List<String>, Map<String, String>> queryArgsEnv();
}
