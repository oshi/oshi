/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.freebsd;

import static java.lang.foreign.ValueLayout.JAVA_INT;
import static oshi.ffm.ForeignFunctions.CAPTURED_STATE_LAYOUT;
import static oshi.ffm.ForeignFunctions.callInArenaOrDefault;
import static oshi.ffm.ForeignFunctions.getByteArrayFromNativePointer;
import static oshi.ffm.ForeignFunctions.getErrno;
import static oshi.ffm.platform.unix.freebsd.FreeBsdLibcFunctions.RLIMIT_LAYOUT;
import static oshi.ffm.platform.unix.freebsd.FreeBsdLibcFunctions.RLIMIT_NOFILE;
import static oshi.ffm.platform.unix.freebsd.FreeBsdLibcFunctions.SIZE_T;
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.platform.unix.freebsd.FreeBsdLibcFunctions;
import oshi.ffm.util.platform.unix.freebsd.BsdSysctlUtilFFM;
import oshi.software.common.os.unix.freebsd.FreeBsdOSProcess;
import oshi.software.common.os.unix.freebsd.FreeBsdOSThread;
import oshi.software.os.OSThread;
import oshi.software.os.unix.freebsd.FreeBsdOperatingSystemFFM.PsKeywords;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.common.platform.unix.freebsd.ProcstatUtil;

/**
 * FFM-backed FreeBSD OSProcess.
 */
@ThreadSafe
public class FreeBsdOSProcessFFM extends FreeBsdOSProcess {

    private static final Logger LOG = LoggerFactory.getLogger(FreeBsdOSProcessFFM.class);

    private static final int ARGMAX = BsdSysctlUtilFFM.sysctl("kern.argmax", 0);

    // CTL_KERN.KERN_PROC.<index>.<pid> sysctl MIB constants
    private static final int CTL_KERN = 1;
    private static final int KERN_PROC = 14;
    private static final int KERN_PROC_ARGS = 7;
    private static final int KERN_PROC_SV_NAME = 9;
    private static final int KERN_PROC_ENV = 35;

    private final FreeBsdOperatingSystemFFM os;

    private Supplier<Integer> bitness = memoize(this::queryBitness);
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
    private String commandLineBackup;

    public FreeBsdOSProcessFFM(int pid, Map<PsKeywords, String> psMap, FreeBsdOperatingSystemFFM os) {
        super(pid);
        this.os = os;
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
        if (ARGMAX <= 0) {
            return Collections.emptyList();
        }
        return callInArenaOrDefault(arena -> {
            MemorySegment mib = arena.allocateFrom(JAVA_INT, CTL_KERN, KERN_PROC, KERN_PROC_ARGS, getProcessID());
            MemorySegment buf = arena.allocate(ARGMAX);
            MemorySegment size = arena.allocateFrom(SIZE_T, (long) ARGMAX);
            MemorySegment callState = arena.allocate(CAPTURED_STATE_LAYOUT);
            int rc = FreeBsdLibcFunctions.sysctl(callState, mib, 4, buf, size, MemorySegment.NULL, 0L);
            if (rc != 0) {
                LOG.warn(
                        "Failed sysctl call for process arguments (kern.proc.args), process {} may not exist. Error code: {}",
                        getProcessID(), getErrno(callState));
                return Collections.<String>emptyList();
            }
            long written = size.get(SIZE_T, 0);
            byte[] bytes = getByteArrayFromNativePointer(buf, written, arena);
            return Collections.unmodifiableList(ParseUtil.parseByteArrayToStrings(bytes));
        }, LOG, org.slf4j.event.Level.WARN, "queryArguments failed", Collections.<String>emptyList());
    }

    @Override
    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables.get();
    }

    private Map<String, String> queryEnvironmentVariables() {
        if (ARGMAX <= 0) {
            return Collections.emptyMap();
        }
        return callInArenaOrDefault(arena -> {
            MemorySegment mib = arena.allocateFrom(JAVA_INT, CTL_KERN, KERN_PROC, KERN_PROC_ENV, getProcessID());
            MemorySegment buf = arena.allocate(ARGMAX);
            MemorySegment size = arena.allocateFrom(SIZE_T, (long) ARGMAX);
            MemorySegment callState = arena.allocate(CAPTURED_STATE_LAYOUT);
            int rc = FreeBsdLibcFunctions.sysctl(callState, mib, 4, buf, size, MemorySegment.NULL, 0L);
            if (rc != 0) {
                LOG.warn(
                        "Failed sysctl call for process environment variables (kern.proc.env), process {} may not exist. Error code: {}",
                        getProcessID(), getErrno(callState));
                return Collections.<String, String>emptyMap();
            }
            long written = size.get(SIZE_T, 0);
            byte[] bytes = getByteArrayFromNativePointer(buf, written, arena);
            return Collections.unmodifiableMap(ParseUtil.parseByteArrayToStringMap(bytes));
        }, LOG, org.slf4j.event.Level.WARN, "queryEnvironmentVariables failed", Collections.<String, String>emptyMap());
    }

    @Override
    public String getCurrentWorkingDirectory() {
        return ProcstatUtil.getCwd(getProcessID());
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
        return ProcstatUtil.getOpenFiles(getProcessID());
    }

    @Override
    public long getSoftOpenFileLimit() {
        if (getProcessID() == this.os.getProcessId()) {
            return rlimitNofile(true);
        }
        return getProcessOpenFileLimit(getProcessID(), 1);
    }

    @Override
    public long getHardOpenFileLimit() {
        if (getProcessID() == this.os.getProcessId()) {
            return rlimitNofile(false);
        }
        return getProcessOpenFileLimit(getProcessID(), 2);
    }

    private static long rlimitNofile(boolean soft) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment rlim = arena.allocate(RLIMIT_LAYOUT);
            if (FreeBsdLibcFunctions.getrlimit(RLIMIT_NOFILE, rlim) != 0) {
                return -1L;
            }
            return soft ? FreeBsdLibcFunctions.rlimitCur(rlim) : FreeBsdLibcFunctions.rlimitMax(rlim);
        } catch (Throwable t) {
            LOG.warn("getrlimit(RLIMIT_NOFILE) failed", t);
            return -1L;
        }
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

    private int queryBitness() {
        return callInArenaOrDefault(arena -> {
            MemorySegment mib = arena.allocateFrom(JAVA_INT, CTL_KERN, KERN_PROC, KERN_PROC_SV_NAME, getProcessID());
            MemorySegment buf = arena.allocate(32L);
            MemorySegment size = arena.allocateFrom(SIZE_T, 32L);
            MemorySegment callState = arena.allocate(CAPTURED_STATE_LAYOUT);
            if (0 != FreeBsdLibcFunctions.sysctl(callState, mib, 4, buf, size, MemorySegment.NULL, 0L)) {
                return 0;
            }
            String elf = buf.getString(0);
            if (elf.contains("ELF32")) {
                return 32;
            } else if (elf.contains("ELF64")) {
                return 64;
            }
            return 0;
        }, LOG, org.slf4j.event.Level.WARN, "queryBitness failed", 0);
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
        String psCommand = "ps -awwxo " + FreeBsdOperatingSystemFFM.PS_COMMAND_ARGS + " -p " + getProcessID();
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
        long voluntaryContextSwitches = ParseUtil.parseLongOrDefault(psMap.get(PsKeywords.NVCSW), 0L);
        long nonVoluntaryContextSwitches = ParseUtil.parseLongOrDefault(psMap.get(PsKeywords.NIVCSW), 0L);
        this.voluntaryContextSwitches = voluntaryContextSwitches;
        this.involuntaryContextSwitches = nonVoluntaryContextSwitches;
        this.commandLineBackup = psMap.get(PsKeywords.ARGS);
        return true;
    }

    private long getProcessOpenFileLimit(long processId, int index) {
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
}
