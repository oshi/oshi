/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.netbsd;

import static oshi.software.os.OSProcess.State.INVALID;
import static oshi.software.os.OSProcess.State.OTHER;
import static oshi.software.os.OSProcess.State.RUNNING;
import static oshi.software.os.OSProcess.State.SLEEPING;
import static oshi.software.os.OSProcess.State.STOPPED;
import static oshi.software.os.OSProcess.State.WAITING;
import static oshi.software.os.OSProcess.State.ZOMBIE;
import static oshi.software.os.OSThread.ThreadFiltering.VALID_THREAD;
import static oshi.util.Memoizer.memoize;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.platform.unix.LibCAPI.size_t;
import com.sun.jna.platform.unix.Resource;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.ByRef.CloseableSizeTByReference;
import oshi.jna.platform.unix.NetBsdLibc;
import oshi.software.common.AbstractOSProcess;
import oshi.software.os.OSThread;
import oshi.software.os.unix.netbsd.NetBsdOperatingSystem.PsKeywords;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.netbsd.FstatUtil;
import oshi.util.platform.unix.netbsd.NetBsdSysctlUtil;

/**
 * OSProcess implementation
 */
@ThreadSafe
public class NetBsdOSProcess extends AbstractOSProcess {

    private static final Logger LOG = LoggerFactory.getLogger(NetBsdOSProcess.class);

    /*
     * Package-private for use by NetBsdOSThread
     */
    enum PsThreadColumns {
        LID, STATE, ETIME, CPUTIME, NIVCSW, NVCSW, MAJFLT, MINFLT, PRI, ARGS;
    }

    static final String PS_THREAD_COLUMNS = Arrays.stream(PsThreadColumns.values()).map(Enum::name)
            .map(name -> name.toLowerCase(Locale.ROOT)).collect(Collectors.joining(","));

    private static final int ARGMAX;

    static {
        int[] mib = new int[2];
        mib[0] = 1; // CTL_KERN
        mib[1] = 8; // KERN_ARGMAX
        try (Memory m = new Memory(Integer.BYTES);
                CloseableSizeTByReference size = new CloseableSizeTByReference(Integer.BYTES)) {
            if (NetBsdLibc.INSTANCE.sysctl(mib, mib.length, m, size, null, size_t.ZERO) == 0) {
                ARGMAX = m.getInt(0);
            } else {
                LOG.warn("Failed sysctl call for process arguments max size (kern.argmax). Error code: {}",
                        Native.getLastError());
                ARGMAX = 0;
            }
        }
    }

    private final NetBsdOperatingSystem os;

    private Supplier<String> commandLine = memoize(this::queryCommandLine);
    private Supplier<List<String>> arguments = memoize(this::queryArguments);
    private Supplier<Map<String, String>> environmentVariables = memoize(this::queryEnvironmentVariables);

    private String name;
    private String path = "";
    private String user;
    private String userID;
    private String group;
    private String groupID;
    private State state = INVALID;
    private int parentProcessID;
    private int threadCount;
    private int priority;
    private long virtualSize;
    private long residentSetSize;
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
    private int bitness;
    private String commandLineBackup;

    public NetBsdOSProcess(int pid, Map<PsKeywords, String> psMap, NetBsdOperatingSystem os) {
        super(pid);
        this.os = os;
        // NetBSD does not maintain a compatibility layer.
        // Process bitness is OS bitness
        this.bitness = Native.LONG_SIZE * 8;
        updateThreadCount();
        updateAttributes(psMap);
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

    private List<String> queryArguments() {
        // NetBSD provides command line via /proc filesystem
        byte[] cmdBytes = FileUtil.readAllBytes("/proc/" + getProcessID() + "/cmdline", false);
        if (cmdBytes != null && cmdBytes.length > 0) {
            return Collections.unmodifiableList(ParseUtil.parseByteArrayToStrings(cmdBytes));
        }
        return Collections.emptyList();
    }

    @Override
    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables.get();
    }

    private Map<String, String> queryEnvironmentVariables() {
        // For the current process, use Java's System.getenv()
        if (getProcessID() == this.os.getProcessId()) {
            return System.getenv();
        }
        // For other processes, try sysctl KERN_PROC_ARGS with KERN_PROC_ENV
        if (ARGMAX <= 0) {
            return Collections.emptyMap();
        }
        int[] mib = new int[4];
        mib[0] = 1; // CTL_KERN
        mib[1] = 48; // KERN_PROC_ARGS
        mib[2] = getProcessID();
        mib[3] = 3; // KERN_PROC_ENV
        try (Memory m = new Memory(ARGMAX); CloseableSizeTByReference size = new CloseableSizeTByReference(ARGMAX)) {
            if (NetBsdLibc.INSTANCE.sysctl(mib, mib.length, m, size, null, size_t.ZERO) == 0) {
                Map<String, String> env = new LinkedHashMap<>();
                long bytesReturned = size.getValue().longValue();
                long offset = 0;
                while (offset < bytesReturned) {
                    String envStr = m.getString(offset);
                    if (envStr.isEmpty()) {
                        break;
                    }
                    int idx = envStr.indexOf('=');
                    if (idx > 0) {
                        env.put(envStr.substring(0, idx), envStr.substring(idx + 1));
                    }
                    offset += envStr.length() + 1;
                }
                if (!env.isEmpty()) {
                    return Collections.unmodifiableMap(env);
                }
            }
        }
        return Collections.emptyMap();
    }

    @Override
    public String getCurrentWorkingDirectory() {
        return FstatUtil.getCwd(getProcessID());
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
    public long getOpenFiles() {
        return FstatUtil.getOpenFiles(getProcessID());
    }

    @Override
    public long getSoftOpenFileLimit() {
        if (getProcessID() == this.os.getProcessId()) {
            final Resource.Rlimit rlimit = new Resource.Rlimit();
            NetBsdLibc.INSTANCE.getrlimit(NetBsdLibc.RLIMIT_NOFILE, rlimit);
            return rlimit.rlim_cur;
        } else {
            return -1L; // not supported
        }
    }

    @Override
    public long getHardOpenFileLimit() {
        if (getProcessID() == this.os.getProcessId()) {
            final Resource.Rlimit rlimit = new Resource.Rlimit();
            NetBsdLibc.INSTANCE.getrlimit(NetBsdLibc.RLIMIT_NOFILE, rlimit);
            return rlimit.rlim_max;
        } else {
            return -1L; // not supported
        }
    }

    @Override
    public int getBitness() {
        return this.bitness;
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
        switch (psMap.get(PsKeywords.STATE).charAt(0)) {
            case 'R':
                this.state = RUNNING;
                break;
            case 'I':
            case 'S':
                this.state = SLEEPING;
                break;
            case 'D':
            case 'L':
            case 'U':
                this.state = WAITING;
                break;
            case 'Z':
                this.state = ZOMBIE;
                break;
            case 'T':
                this.state = STOPPED;
                break;
            default:
                this.state = OTHER;
                break;
        }
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
