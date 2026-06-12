/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.mac;

import static oshi.software.os.OSProcess.State.INVALID;
import static oshi.software.os.OSProcess.State.NEW;
import static oshi.software.os.OSProcess.State.OTHER;
import static oshi.software.os.OSProcess.State.RUNNING;
import static oshi.software.os.OSProcess.State.SLEEPING;
import static oshi.software.os.OSProcess.State.STOPPED;
import static oshi.software.os.OSProcess.State.WAITING;
import static oshi.software.os.OSProcess.State.ZOMBIE;
import static oshi.util.Memoizer.memoize;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.mac.ThreadInfo;
import oshi.software.common.AbstractOSProcess;
import oshi.software.os.OSThread;
import oshi.util.GlobalConfig;
import oshi.util.tuples.Pair;

/**
 * Abstract base shared by the macOS OSProcess implementations (JNA and FFM). Holds the field storage, the trivial
 * accessors, the command-line/argument/environment memoization, thread enumeration, affinity mask, and the {@code
 * pbi_status} state mapping. The native attribute reads ({@code proc_pidinfo}, the {@code KERN_PROCARGS2} sysctl, the
 * {@code getrlimit} file limits, and the logical-processor sysctl) are provided by the per-backend subclasses.
 */
@ThreadSafe
public abstract class MacOSProcess extends AbstractOSProcess {

    protected static final boolean LOG_MAC_SYSCTL_WARNING = GlobalConfig.get(GlobalConfig.OSHI_OS_MAC_SYSCTL_LOGWARNING,
            false);

    protected static final int MAC_RLIMIT_NOFILE = 8;

    // 64-bit flag
    protected static final int P_LP64 = 0x4;

    /*
     * macOS process states
     */
    private static final int SSLEEP = 1; // sleeping on high priority
    private static final int SWAIT = 2; // sleeping on low priority
    private static final int SRUN = 3; // running
    private static final int SIDL = 4; // intermediate state in process creation
    private static final int SZOMB = 5; // intermediate state in process termination
    private static final int SSTOP = 6; // process being traced

    protected final int majorVersion;
    protected final int minorVersion;
    protected final MacOperatingSystem os;

    private final Supplier<String> commandLine = memoize(this::queryCommandLine);
    private final Supplier<Pair<List<String>, Map<String, String>>> argsEnviron = memoize(
            this::queryArgsAndEnvironment);

    protected String name = "";
    protected String path = "";
    protected String currentWorkingDirectory;
    protected String user;
    protected String userID;
    protected String group;
    protected String groupID;
    protected State state = INVALID;
    protected int parentProcessID;
    protected int threadCount;
    protected int priority;
    protected long virtualSize;
    protected long residentSetSize;
    protected long memoryFootprint;
    protected long kernelTime;
    protected long userTime;
    protected long startTime;
    protected long upTime;
    protected long bytesRead;
    protected long bytesWritten;
    protected long openFiles;
    protected int bitness;
    protected long minorFaults;
    protected long majorFaults;
    protected long contextSwitches;
    protected long voluntaryContextSwitches;
    protected long involuntaryContextSwitches;

    protected MacOSProcess(int pid, int major, int minor, MacOperatingSystem os) {
        super(pid);
        this.majorVersion = major;
        this.minorVersion = minor;
        this.os = os;
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
        return String.join(" ", getArguments());
    }

    @Override
    public List<String> getArguments() {
        return argsEnviron.get().getA();
    }

    @Override
    public Map<String, String> getEnvironmentVariables() {
        return argsEnviron.get().getB();
    }

    @Override
    public String getCurrentWorkingDirectory() {
        return this.currentWorkingDirectory;
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
    public List<OSThread> getThreadDetails() {
        long now = System.currentTimeMillis();
        return ThreadInfo.queryTaskThreads(getProcessID()).stream().parallel().map(stat -> {
            // For long running threads the start time calculation can overestimate
            long start = Math.max(now - stat.getUpTime(), getStartTime());
            return new MacOSThread(getProcessID(), stat.getThreadId(), stat.getState(), stat.getSystemTime(),
                    stat.getUserTime(), start, now - start, stat.getPriority());
        }).collect(Collectors.toList());
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
        return this.memoryFootprint;
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
    public long getOpenFiles() {
        return this.openFiles;
    }

    @Override
    public int getBitness() {
        return this.bitness;
    }

    @Override
    public long getAffinityMask() {
        // macOS doesn't do affinity. Return a bitmask of the current processors.
        int logicalProcessorCount = queryLogicalProcessorCount();
        return logicalProcessorCount < 64 ? (1L << logicalProcessorCount) - 1 : -1L;
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
    public long getContextSwitches() {
        return this.contextSwitches;
    }

    @Override
    public long getVoluntaryContextSwitches() {
        return this.voluntaryContextSwitches;
    }

    @Override
    public long getInvoluntaryContextSwitches() {
        return this.involuntaryContextSwitches;
    }

    /**
     * Maps a macOS {@code pbi_status} value to an {@link State}.
     *
     * @param status the {@code pbi_status} value
     * @return the corresponding process state
     */
    protected static State stateFromStatus(int status) {
        switch (status) {
            case SSLEEP:
                return SLEEPING;
            case SWAIT:
                return WAITING;
            case SRUN:
                return RUNNING;
            case SIDL:
                return NEW;
            case SZOMB:
                return ZOMBIE;
            case SSTOP:
                return STOPPED;
            default:
                return OTHER;
        }
    }

    /**
     * Queries this process's arguments and environment via the backend-specific {@code KERN_PROCARGS2} sysctl.
     *
     * @return a pair of the argument list and the environment map
     */
    protected abstract Pair<List<String>, Map<String, String>> queryArgsAndEnvironment();

    /**
     * Returns the number of logical processors via the backend-specific {@code hw.logicalcpu} sysctl.
     *
     * @return the logical processor count
     */
    protected abstract int queryLogicalProcessorCount();
}
