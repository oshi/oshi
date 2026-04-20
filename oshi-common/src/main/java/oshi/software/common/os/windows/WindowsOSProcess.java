/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.windows;

import static oshi.software.os.OSProcess.State.INVALID;
import static oshi.software.os.OSProcess.State.RUNNING;
import static oshi.software.os.OSProcess.State.SUSPENDED;
import static oshi.util.Memoizer.memoize;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.registry.ProcessPerfCounterBlock;
import oshi.driver.common.windows.registry.ThreadPerfCounterBlock;
import oshi.driver.common.windows.registry.WtsInfo;
import oshi.software.common.AbstractOSProcess;
import oshi.software.os.OSThread;
import oshi.software.os.OperatingSystem;
import oshi.util.Constants;
import oshi.util.GlobalConfig;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

/**
 * Common base class for Windows OS process implementations, containing shared fields, getters, and non-native methods.
 */
@ThreadSafe
public abstract class WindowsOSProcess extends AbstractOSProcess {

    // See https://blogs.technet.microsoft.com/markrussinovich/2009/09/29/pushing-the-limits-of-windows-handles/
    protected static final long MAX_WINDOWS_HANDLES;
    static {
        if (System.getenv("ProgramFiles(x86)") == null) {
            MAX_WINDOWS_HANDLES = 16_777_216L - 32_768L;
        } else {
            MAX_WINDOWS_HANDLES = 16_777_216L - 65_536L;
        }
    }

    protected static final boolean USE_BATCH_COMMANDLINE = GlobalConfig
            .get(GlobalConfig.OSHI_OS_WINDOWS_COMMANDLINE_BATCH, false);

    protected static final boolean USE_PROCSTATE_SUSPENDED = GlobalConfig
            .get(GlobalConfig.OSHI_OS_WINDOWS_PROCSTATE_SUSPENDED, false);

    private final OperatingSystem os;

    private Supplier<Pair<String, String>> userInfo = memoize(this::queryUserInfo);
    private Supplier<Pair<String, String>> groupInfo = memoize(this::queryGroupInfo);
    private Supplier<String> currentWorkingDirectory = memoize(this::queryCwd);
    private Supplier<String> commandLine = memoize(this::queryCommandLine);
    private Supplier<List<String>> args = memoize(this::queryArguments);
    private Supplier<Triplet<String, String, Map<String, String>>> cwdCmdEnv = memoize(
            this::queryCwdCommandlineEnvironment);
    private Map<Integer, ThreadPerfCounterBlock> tcb;

    private String name;
    private String path = "";
    private State state = INVALID;
    private int parentProcessID;
    private int threadCount;
    private int priority;
    private long virtualSize;
    private long workingSetSize;
    private long privateWorkingSetSize;
    private long kernelTime;
    private long userTime;
    private long startTime;
    private long upTime;
    private long bytesRead;
    private long bytesWritten;
    private long openFiles;
    private int bitness;
    private long pageFaults;

    protected WindowsOSProcess(int pid, OperatingSystem os, Map<Integer, ProcessPerfCounterBlock> processMap,
            Map<Integer, WtsInfo> processWtsMap, Map<Integer, ThreadPerfCounterBlock> threadMap) {
        super(pid);
        this.os = os;
        this.bitness = os.getBitness();
        this.tcb = threadMap;
        updateAttributes(processMap.get(pid), processWtsMap.get(pid));
    }

    /**
     * Returns the {@link OperatingSystem} instance associated with this process.
     *
     * @return the operating system instance
     */
    protected OperatingSystem getOs() {
        return this.os;
    }

    /**
     * Returns the memoized CWD/CommandLine/Environment triplet.
     *
     * @return the triplet
     */
    protected Triplet<String, String, Map<String, String>> getCwdCmdEnv() {
        return cwdCmdEnv.get();
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

    @Override
    public List<String> getArguments() {
        return args.get();
    }

    @Override
    public Map<String, String> getEnvironmentVariables() {
        return cwdCmdEnv.get().getC();
    }

    @Override
    public String getCurrentWorkingDirectory() {
        return currentWorkingDirectory.get();
    }

    @Override
    public String getUser() {
        return userInfo.get().getA();
    }

    @Override
    public String getUserID() {
        return userInfo.get().getB();
    }

    @Override
    public String getGroup() {
        return groupInfo.get().getA();
    }

    @Override
    public String getGroupID() {
        return groupInfo.get().getB();
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
        return this.workingSetSize;
    }

    @Override
    public long getPrivateResidentMemory() {
        return this.privateWorkingSetSize;
    }

    /**
     * {@inheritDoc}
     * <p>
     * On Windows, delegates to {@link #getPrivateResidentMemory()} for backwards compatibility with prior behavior that
     * returned the Private Working Set.
     */
    @Deprecated
    @Override
    public long getResidentSetSize() {
        return getPrivateResidentMemory();
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
    public long getSoftOpenFileLimit() {
        return MAX_WINDOWS_HANDLES;
    }

    @Override
    public long getHardOpenFileLimit() {
        return MAX_WINDOWS_HANDLES;
    }

    @Override
    public int getBitness() {
        return this.bitness;
    }

    @Override
    public long getMinorFaults() {
        return this.pageFaults;
    }

    @Override
    public List<OSThread> getThreadDetails() {
        Map<Integer, ThreadPerfCounterBlock> threads = tcb == null
                ? queryMatchingThreads(Collections.singleton(this.getProcessID()))
                : tcb;
        if (threads == null) {
            threads = Collections.emptyMap();
        }
        return threads.entrySet().stream().parallel()
                .filter(entry -> entry.getValue().getOwningProcessID() == this.getProcessID())
                .map(entry -> createOSThread(getProcessID(), entry.getKey(), this.name, entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * Creates a platform-specific OS thread instance.
     *
     * @param pid      the owning process ID
     * @param tid      the thread ID
     * @param procName the process name
     * @param pcb      the thread performance counter block
     * @return a new OSThread instance
     */
    protected abstract OSThread createOSThread(int pid, int tid, String procName, ThreadPerfCounterBlock pcb);

    /**
     * Updates process attributes from performance counter and WTS data, then performs native-specific updates.
     * Subclasses should call {@code super.updateAttributes(pcb, wts)} and then perform native handle-based updates.
     *
     * @param pcb Performance counter block for this process, or null if unavailable
     * @param wts WTS info for this process, or null if unavailable
     * @return true if the process is valid after the update
     */
    protected boolean updateAttributes(ProcessPerfCounterBlock pcb, WtsInfo wts) {
        if (pcb == null) {
            this.state = INVALID;
            return false;
        }
        this.name = pcb.getName();
        this.parentProcessID = pcb.getParentProcessID();
        this.priority = pcb.getPriority();
        this.workingSetSize = pcb.getWorkingSetSize();
        this.privateWorkingSetSize = pcb.getPrivateWorkingSetSize();
        this.startTime = pcb.getStartTime();
        this.upTime = pcb.getUpTime();
        this.bytesRead = pcb.getBytesRead();
        this.bytesWritten = pcb.getBytesWritten();
        this.pageFaults = pcb.getPageFaults();
        if (wts != null) {
            this.path = wts.getPath();
            this.threadCount = wts.getThreadCount();
            this.virtualSize = wts.getVirtualSize();
            this.kernelTime = wts.getKernelTime();
            this.userTime = wts.getUserTime();
            this.openFiles = wts.getOpenFiles();
        }

        this.state = RUNNING;
        if (this.tcb != null) {
            int pid = this.getProcessID();
            for (ThreadPerfCounterBlock tpd : this.tcb.values()) {
                if (tpd.getOwningProcessID() == pid) {
                    if (tpd.getThreadWaitReason() == 5) {
                        this.state = SUSPENDED;
                    } else {
                        this.state = RUNNING;
                        break;
                    }
                }
            }
        }

        return !this.state.equals(INVALID);
    }

    /**
     * Sets the process name. Used by subclasses to ensure the name is current before querying threads.
     *
     * @param name the process name to set
     */
    protected void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the process bitness. Used by subclasses to update after WOW64 check.
     *
     * @param bitness the bitness to set
     */
    protected void setBitness(int bitness) {
        this.bitness = bitness;
    }

    /**
     * Sets the process executable path. Used by subclasses to update after native query.
     *
     * @param path the path to set
     */
    protected void setPath(String path) {
        this.path = path;
    }

    /**
     * Sets the process state. Used by subclasses to mark a process as invalid.
     *
     * @param state the state to set
     */
    protected void setState(State state) {
        this.state = state;
    }

    /**
     * Sets the thread counter block map. Used by subclasses during attribute updates.
     *
     * @param tcb the thread performance counter block map
     */
    protected void setTcb(Map<Integer, ThreadPerfCounterBlock> tcb) {
        this.tcb = tcb;
    }

    /**
     * Queries thread performance data matching the given process IDs.
     *
     * @param pids the set of process IDs to match
     * @return a map of thread ID to thread performance counter block
     */
    protected abstract Map<Integer, ThreadPerfCounterBlock> queryMatchingThreads(Set<Integer> pids);

    /**
     * Queries the command line for this process.
     *
     * @return the command line string
     */
    protected abstract String queryCommandLine();

    /**
     * Queries the argument list for this process.
     *
     * @return the list of arguments
     */
    protected abstract List<String> queryArguments();

    private String queryCwd() {
        if (!cwdCmdEnv.get().getA().isEmpty()) {
            return cwdCmdEnv.get().getA();
        }
        if (getProcessID() == this.os.getProcessId()) {
            String cwd = new File(".").getAbsolutePath();
            if (!cwd.isEmpty()) {
                return cwd.substring(0, cwd.length() - 1);
            }
        }
        return "";
    }

    /**
     * Queries user account information for this process.
     *
     * @return a pair of (account name, SID string)
     */
    protected abstract Pair<String, String> queryUserInfo();

    /**
     * Queries group account information for this process.
     *
     * @return a pair of (group name, SID string)
     */
    protected abstract Pair<String, String> queryGroupInfo();

    /**
     * Queries the current working directory, command line, and environment variables from process memory.
     *
     * @return a triplet of (cwd, commandLine, environmentVariables)
     */
    protected abstract Triplet<String, String, Map<String, String>> queryCwdCommandlineEnvironment();

    protected static Triplet<String, String, Map<String, String>> defaultCwdCommandlineEnvironment() {
        return new Triplet<>("", "", Collections.emptyMap());
    }

    protected static Pair<String, String> defaultPair() {
        return new Pair<>(Constants.UNKNOWN, Constants.UNKNOWN);
    }
}
