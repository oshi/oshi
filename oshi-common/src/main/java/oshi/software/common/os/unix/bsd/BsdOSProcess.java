/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.bsd;

import static oshi.software.os.OSProcess.State.OTHER;
import static oshi.software.os.OSProcess.State.RUNNING;
import static oshi.software.os.OSProcess.State.SLEEPING;
import static oshi.software.os.OSProcess.State.STOPPED;
import static oshi.software.os.OSProcess.State.WAITING;
import static oshi.software.os.OSProcess.State.ZOMBIE;
import static oshi.util.Memoizer.memoize;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractOSProcess;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;

/**
 * Abstract base shared by the BSD-family OSProcess implementations (FreeBSD, OpenBSD, DragonFly, NetBSD). Holds the
 * field storage, the trivial accessors, command-line/argument/environment/bitness memoization, the {@code ps} state
 * mapping, the {@code cpuset} affinity lookup, and the {@code /proc/<pid>/limits} fallback. Platform-specific work (the
 * {@code ps} attribute query, thread enumeration, cwd/open-file lookups, and the native argument/environment and
 * file-limit reads) is provided by the per-platform subclasses.
 */
@ThreadSafe
public abstract class BsdOSProcess extends AbstractOSProcess {

    private final Supplier<Integer> bitness = memoize(this::queryBitness);
    private final Supplier<String> commandLine = memoize(this::queryCommandLine);
    private final Supplier<List<String>> arguments = memoize(this::queryArguments);
    private final Supplier<Map<String, String>> environmentVariables = memoize(this::queryEnvironmentVariables);

    protected String name;
    protected String path = "";
    protected String user;
    protected String userID;
    protected String group;
    protected String groupID;
    protected State state = State.INVALID;
    protected int parentProcessID;
    protected int threadCount;
    protected int priority;
    protected long virtualSize;
    protected long residentSetSize;
    protected long kernelTime;
    protected long userTime;
    protected long startTime;
    protected long upTime;
    protected long bytesRead;
    protected long bytesWritten;
    protected long minorFaults;
    protected long majorFaults;
    protected long voluntaryContextSwitches;
    protected long involuntaryContextSwitches;
    protected String commandLineBackup;

    protected BsdOSProcess(int pid) {
        super(pid);
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
        return arguments.get();
    }

    @Override
    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables.get();
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
    public int getBitness() {
        return this.bitness.get();
    }

    @Override
    public long getAffinityMask() {
        long bitMask = 0L;
        // Would prefer to use native cpuset_getaffinity call but variable sizing is
        // kernel-dependent and requires C macros, so we use commandline instead.
        String cpuset = ExecutingCommand.getFirstAnswer("cpuset -gp " + getProcessID());
        // Sample output:
        // pid 8 mask: 0, 1
        // cpuset: getaffinity: No such process
        String[] split = cpuset.split(":");
        if (split.length > 1) {
            String[] bits = split[1].split(",");
            for (String bit : bits) {
                int bitToSet = ParseUtil.parseIntOrDefault(bit.trim(), -1);
                if (bitToSet >= 0 && bitToSet < Long.SIZE) {
                    bitMask |= 1L << bitToSet;
                }
            }
        }
        return bitMask;
    }

    /**
     * Maps a {@code ps} single-character state to an {@link State}.
     *
     * @param stateValue the first character of the {@code ps} STATE column
     * @return the corresponding process state
     */
    protected static State getStateFromOutput(char stateValue) {
        switch (stateValue) {
            case 'R':
                return RUNNING;
            case 'I':
            case 'S':
                return SLEEPING;
            case 'D':
            case 'L':
            case 'U':
                return WAITING;
            case 'Z':
                return ZOMBIE;
            case 'T':
                return STOPPED;
            default:
                return OTHER;
        }
    }

    /**
     * Parses the soft or hard open-file limit for another process from {@code /proc/<pid>/limits}.
     *
     * @param processId the process ID
     * @param index     {@code 1} for the soft limit, {@code 2} for the hard limit
     * @return the limit, or {@code -1} if unavailable
     */
    protected static long getProcessOpenFileLimit(long processId, int index) {
        final String limitsPath = String.format(Locale.ROOT, "/proc/%d/limits", processId);
        if (!Files.exists(Paths.get(limitsPath))) {
            return -1; // not supported
        }
        final List<String> lines = FileUtil.readFile(limitsPath);
        final Optional<String> maxOpenFilesLine = lines.stream().filter(line -> line.startsWith("Max open files"))
                .findFirst();
        if (!maxOpenFilesLine.isPresent()) {
            return -1;
        }
        // Split all non-Digits away -> ["", "{soft-limit}, "{hard-limit}"]
        final String[] split = maxOpenFilesLine.get().split("\\D+");
        if (split.length <= index) {
            return -1;
        }
        return ParseUtil.parseLongOrDefault(split[index], -1);
    }

    /**
     * Queries this process's argument list.
     *
     * @return the arguments, or an empty list if unavailable
     */
    protected abstract List<String> queryArguments();

    /**
     * Queries this process's environment variables.
     *
     * @return the environment map, or an empty map if unavailable
     */
    protected abstract Map<String, String> queryEnvironmentVariables();

    /**
     * Queries this process's bitness (32 or 64), or {@code 0} if unknown.
     *
     * @return the bitness
     */
    protected abstract int queryBitness();
}
