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
import static oshi.software.os.OSThread.ThreadFiltering.VALID_THREAD;
import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.driver.common.unix.solaris.PsInfo;
import oshi.driver.common.unix.solaris.SolarisPrUsage;
import oshi.driver.common.unix.solaris.SolarisPsInfo;
import oshi.software.common.AbstractOSProcess;
import oshi.software.os.OSThread;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.UserGroupInfo;
import oshi.util.tuples.Pair;

/**
 * Abstract base for Solaris OSProcess. The {@code /proc} parsing, command-line/env memoization, thread enumeration, and
 * field assignment are shared; concrete subclasses (JNA/FFM) provide the {@code queryArgsEnv} read, the self-process
 * {@code rlimit} read, and the {@link OSThread} factory.
 */
public abstract class SolarisOSProcess extends AbstractOSProcess {

    private static final Logger LOG = LoggerFactory.getLogger(SolarisOSProcess.class);

    private final Supplier<Integer> bitness = memoize(this::queryBitness);
    private final Supplier<SolarisPsInfo> psinfo = memoize(this::queryPsInfo, defaultExpiration());
    private final Supplier<String> commandLine = memoize(this::queryCommandLine);
    private final Supplier<Pair<List<String>, Map<String, String>>> cmdEnv = memoize(this::queryCommandlineEnvironment);
    private final Supplier<SolarisPrUsage> prusage = memoize(this::queryPrUsage, defaultExpiration());

    private String name;
    private String path = "";
    private String commandLineBackup;
    private String user;
    private String userID;
    private String group;
    private String groupID;
    private State state = State.INVALID;
    private int parentProcessID;
    private int threadCount;
    private int priority;
    private long virtualSize;
    private long residentSetSize;
    private long residentSetSizePrivate;
    private long kernelTime;
    private long userTime;
    private long startTime;
    private long upTime;
    private long bytesRead;
    private long bytesWritten;
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
    public String getName() {
        return this.name;
    }

    @Override
    public String getPath() {
        return this.path;
    }

    @Override
    public String getCommandLine() {
        return this.commandLine.get();
    }

    private String queryCommandLine() {
        String cl = String.join(" ", getArguments());
        return cl.isEmpty() ? this.commandLineBackup : cl;
    }

    @Override
    public List<String> getArguments() {
        return cmdEnv.get().getA();
    }

    @Override
    public Map<String, String> getEnvironmentVariables() {
        return cmdEnv.get().getB();
    }

    private Pair<List<String>, Map<String, String>> queryCommandlineEnvironment() {
        return queryArgsEnv();
    }

    @Override
    public String getCurrentWorkingDirectory() {
        try {
            String cwdLink = "/proc/" + getProcessID() + "/cwd";
            String cwd = new File(cwdLink).getCanonicalPath();
            if (!cwd.equals(cwdLink)) {
                return cwd;
            }
        } catch (IOException e) {
            LOG.trace("Couldn't find cwd for pid {}: {}", getProcessID(), e.getMessage());
        }
        return "";
    }

    @Override
    public String getUser() {
        return this.user;
    }

    @Override
    public String getUserID() {
        return this.userID;
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public String getGroupID() {
        return this.groupID;
    }

    @Override
    public State getState() {
        return this.state;
    }

    @Override
    public int getParentProcessID() {
        return this.parentProcessID;
    }

    @Override
    public int getThreadCount() {
        return this.threadCount;
    }

    @Override
    public int getPriority() {
        return this.priority;
    }

    @Override
    public long getVirtualSize() {
        return this.virtualSize;
    }

    @Override
    public long getResidentMemory() {
        return this.residentSetSize;
    }

    @Override
    public long getPrivateResidentMemory() {
        return this.residentSetSizePrivate;
    }

    @Override
    public long getKernelTime() {
        return this.kernelTime;
    }

    @Override
    public long getUserTime() {
        return this.userTime;
    }

    @Override
    public long getUpTime() {
        return this.upTime;
    }

    @Override
    public long getStartTime() {
        return this.startTime;
    }

    @Override
    public long getBytesRead() {
        return this.bytesRead;
    }

    @Override
    public long getBytesWritten() {
        return this.bytesWritten;
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
    public long getOpenFiles() {
        try (Stream<Path> fd = Files.list(Paths.get("/proc/" + getProcessID() + "/fd"))) {
            return fd.count();
        } catch (IOException e) {
            return 0L;
        }
    }

    @Override
    public int getBitness() {
        return this.bitness.get();
    }

    private int queryBitness() {
        List<String> pflags = ExecutingCommand.runNative("pflags " + getProcessID());
        for (String line : pflags) {
            if (line.contains("data model")) {
                if (line.contains("LP32")) {
                    return 32;
                } else if (line.contains("LP64")) {
                    return 64;
                }
            }
        }
        return 0;
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
    public List<OSThread> getThreadDetails() {
        // Get process files in proc
        File directory = new File(String.format(Locale.ROOT, "/proc/%d/lwp", getProcessID()));
        File[] numericFiles = directory.listFiles(file -> Constants.DIGITS.matcher(file.getName()).matches());
        if (numericFiles == null) {
            return Collections.emptyList();
        }

        return Arrays.stream(numericFiles).parallel()
                .map(lwpidFile -> createThread(ParseUtil.parseIntOrDefault(lwpidFile.getName(), 0)))
                .filter(VALID_THREAD).collect(Collectors.toList());
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
        this.residentSetSizePrivate = info.pr_rssizepriv * 1024;
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
        final String[] split = nofilesLine.get().split("\\D+");
        return ParseUtil.parseLongOrDefault(split[index], -1);
    }

    /**
     * Reads this process's argument list and environment.
     *
     * @return a pair of (arg list, env map); either may be empty if it cannot be read
     */
    protected abstract Pair<List<String>, Map<String, String>> queryArgsEnv();

    /**
     * Creates a platform-specific {@link OSThread} for the given lwpid of this process.
     *
     * @param lwpid the thread (lwp) ID
     * @return a new OSThread
     */
    protected abstract OSThread createThread(int lwpid);
}
