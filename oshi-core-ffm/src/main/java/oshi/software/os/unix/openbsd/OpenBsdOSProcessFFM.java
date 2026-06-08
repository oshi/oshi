/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.openbsd;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static oshi.ffm.ForeignFunctions.CAPTURED_STATE_LAYOUT;
import static oshi.ffm.ForeignFunctions.NATIVE_LONG_SIZE;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.CTL_KERN;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.KERN_ARGMAX;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.KERN_PROC_ARGS;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.KERN_PROC_ARGV;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.KERN_PROC_ENV;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.RLIMIT_NOFILE;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.SIZE_T;
import static oshi.software.os.OSProcess.State.INVALID;
import static oshi.software.os.OSProcess.State.OTHER;
import static oshi.software.os.OSProcess.State.RUNNING;
import static oshi.software.os.OSProcess.State.SLEEPING;
import static oshi.software.os.OSProcess.State.STOPPED;
import static oshi.software.os.OSProcess.State.WAITING;
import static oshi.software.os.OSProcess.State.ZOMBIE;
import static oshi.software.os.OSThread.ThreadFiltering.VALID_THREAD;
import static oshi.util.Memoizer.memoize;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.ForeignFunctions;
import oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions;
import oshi.ffm.util.platform.unix.openbsd.OpenBsdSysctlUtilFFM;
import oshi.software.common.os.unix.openbsd.OpenBsdOSThread;
import oshi.software.os.OSThread;
import oshi.software.os.unix.openbsd.OpenBsdOperatingSystemFFM.PsKeywords;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.common.platform.unix.openbsd.FstatUtil;

@ThreadSafe
public class OpenBsdOSProcessFFM extends oshi.software.common.os.unix.openbsd.OpenBsdOSProcess {

    private static final Logger LOG = LoggerFactory.getLogger(OpenBsdOSProcessFFM.class);

    private static final int ARGMAX;

    static {
        int[] mib = { CTL_KERN, KERN_ARGMAX };
        ARGMAX = OpenBsdSysctlUtilFFM.sysctl(mib, 0);
    }

    private final OpenBsdOperatingSystemFFM os;

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

    public OpenBsdOSProcessFFM(int pid, Map<PsKeywords, String> psMap, OpenBsdOperatingSystemFFM os) {
        super(pid);
        this.os = os;
        this.bitness = (int) (NATIVE_LONG_SIZE * 8);
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
        if (ARGMAX > 0) {
            int[] mib = { CTL_KERN, KERN_PROC_ARGS, getProcessID(), KERN_PROC_ARGV };
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment mibSeg = arena.allocate(JAVA_INT, mib.length);
                for (int i = 0; i < mib.length; i++) {
                    mibSeg.setAtIndex(JAVA_INT, i, mib[i]);
                }
                MemorySegment buf = arena.allocate(ARGMAX);
                MemorySegment sizeSeg = arena.allocateFrom(SIZE_T, (long) ARGMAX);
                MemorySegment callState = arena.allocate(CAPTURED_STATE_LAYOUT);
                if (OpenBsdLibcFunctions.sysctl(callState, mibSeg, mib.length, buf, sizeSeg, MemorySegment.NULL,
                        0L) == 0) {
                    long actualSize = sizeSeg.get(SIZE_T, 0);
                    long baseAddr = buf.address();
                    long maxAddr = baseAddr + actualSize;
                    List<String> args = new ArrayList<>();
                    long offset = 0;
                    long argAddr = buf.get(ADDRESS, offset).address();
                    while (argAddr > baseAddr && argAddr < maxAddr) {
                        args.add(buf.getString(argAddr - baseAddr));
                        offset += ADDRESS.byteSize();
                        argAddr = buf.get(ADDRESS, offset).address();
                    }
                    return Collections.unmodifiableList(args);
                }
            } catch (Throwable e) {
                LOG.warn("Failed to get process arguments for pid {}", getProcessID(), e);
            }
        }
        return Collections.emptyList();
    }

    @Override
    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables.get();
    }

    private Map<String, String> queryEnvironmentVariables() {
        if (ARGMAX > 0) {
            int[] mib = { CTL_KERN, KERN_PROC_ARGS, getProcessID(), KERN_PROC_ENV };
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment mibSeg = arena.allocate(JAVA_INT, mib.length);
                for (int i = 0; i < mib.length; i++) {
                    mibSeg.setAtIndex(JAVA_INT, i, mib[i]);
                }
                MemorySegment buf = arena.allocate(ARGMAX);
                MemorySegment sizeSeg = arena.allocateFrom(SIZE_T, (long) ARGMAX);
                MemorySegment callState = arena.allocate(CAPTURED_STATE_LAYOUT);
                if (OpenBsdLibcFunctions.sysctl(callState, mibSeg, mib.length, buf, sizeSeg, MemorySegment.NULL,
                        0L) == 0) {
                    long actualSize = sizeSeg.get(SIZE_T, 0);
                    long baseAddr = buf.address();
                    long maxAddr = baseAddr + actualSize;
                    Map<String, String> env = new LinkedHashMap<>();
                    long offset = 0;
                    long argAddr = buf.get(ADDRESS, offset).address();
                    while (argAddr > baseAddr && argAddr < maxAddr) {
                        String envStr = buf.getString(argAddr - baseAddr);
                        int idx = envStr.indexOf('=');
                        if (idx > 0) {
                            env.put(envStr.substring(0, idx), envStr.substring(idx + 1));
                        }
                        offset += ADDRESS.byteSize();
                        argAddr = buf.get(ADDRESS, offset).address();
                    }
                    return Collections.unmodifiableMap(env);
                }
            } catch (Throwable e) {
                LOG.warn("Failed to get environment variables for pid {}", getProcessID(), e);
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
            return rlimitNofile(true);
        }
        return -1L;
    }

    @Override
    public long getHardOpenFileLimit() {
        if (getProcessID() == this.os.getProcessId()) {
            return rlimitNofile(false);
        }
        return -1L;
    }

    private long rlimitNofile(boolean soft) {
        return ForeignFunctions.callInArenaLongOrDefault(arena -> {
            MemorySegment rlim = arena.allocate(OpenBsdLibcFunctions.RLIMIT_LAYOUT);
            if (OpenBsdLibcFunctions.getrlimit(RLIMIT_NOFILE, rlim) != 0) {
                return -1L;
            }
            return soft ? OpenBsdLibcFunctions.rlimitCur(rlim) : OpenBsdLibcFunctions.rlimitMax(rlim);
        }, LOG, org.slf4j.event.Level.WARN, "Failed getrlimit", -1L);
    }

    @Override
    public int getBitness() {
        return this.bitness;
    }

    @Override
    public long getAffinityMask() {
        long bitMask = 0L;
        String cpuset = ExecutingCommand.getFirstAnswer("cpuset -gp " + getProcessID());
        String[] split = cpuset.split(":");
        if (split.length > 1) {
            String[] bits = split[1].split(",");
            for (String bit : bits) {
                int bitToSet = ParseUtil.parseIntOrDefault(bit.trim(), -1);
                if (bitToSet >= 0) {
                    bitMask |= 1L << bitToSet;
                }
            }
        }
        return bitMask;
    }

    @Override
    public List<OSThread> getThreadDetails() {
        String psCommand = "ps -aHwwxo " + PS_THREAD_COLUMNS;
        if (getProcessID() >= 0) {
            psCommand += " -p " + getProcessID();
        }
        Predicate<Map<PsThreadColumns, String>> hasColumnsArgs = threadMap -> threadMap
                .containsKey(PsThreadColumns.ARGS);
        return ExecutingCommand.runNative(psCommand).stream().skip(1)
                .map(thread -> ParseUtil.stringToEnumMap(PsThreadColumns.class, thread.trim(), ' '))
                .filter(hasColumnsArgs).map(threadMap -> new OpenBsdOSThread(getProcessID(), threadMap))
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
        String psCommand = "ps -awwxo " + OpenBsdOperatingSystemFFM.PS_COMMAND_ARGS + " -p " + getProcessID();
        List<String> procList = ExecutingCommand.runNative(psCommand);
        if (procList.size() > 1) {
            Map<PsKeywords, String> psMap = ParseUtil.stringToEnumMap(PsKeywords.class, procList.get(1).trim(), ' ');
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
        this.virtualSize = ParseUtil.parseLongOrDefault(psMap.get(PsKeywords.VSZ), 0) * 1024;
        this.residentSetSize = ParseUtil.parseLongOrDefault(psMap.get(PsKeywords.RSS), 0) * 1024;
        long elapsedTime = ParseUtil.parseDHMSOrDefault(psMap.get(PsKeywords.ETIME), 0L);
        this.upTime = elapsedTime < 1L ? 1L : elapsedTime;
        this.startTime = now - this.upTime;
        this.userTime = ParseUtil.parseDHMSOrDefault(psMap.get(PsKeywords.CPUTIME), 0L);
        this.kernelTime = 0L;
        this.path = psMap.get(PsKeywords.COMM);
        this.name = this.path.substring(this.path.lastIndexOf('/') + 1);
        this.minorFaults = ParseUtil.parseLongOrDefault(psMap.get(PsKeywords.MINFLT), 0L);
        this.majorFaults = ParseUtil.parseLongOrDefault(psMap.get(PsKeywords.MAJFLT), 0L);
        this.voluntaryContextSwitches = ParseUtil.parseLongOrDefault(psMap.get(PsKeywords.NVCSW), 0L);
        this.involuntaryContextSwitches = ParseUtil.parseLongOrDefault(psMap.get(PsKeywords.NIVCSW), 0L);
        this.commandLineBackup = psMap.get(PsKeywords.ARGS);
        return true;
    }

    private void updateThreadCount() {
        List<String> threadList = ExecutingCommand.runNative("ps -axHo tid -p " + getProcessID());
        if (!threadList.isEmpty()) {
            this.threadCount = threadList.size() - 1;
        }
        this.threadCount = Math.max(this.threadCount, 1);
    }
}
