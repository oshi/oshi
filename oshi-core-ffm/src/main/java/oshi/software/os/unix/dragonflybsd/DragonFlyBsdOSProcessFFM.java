/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.dragonflybsd;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static oshi.ffm.ForeignFunctions.callInArenaIntOrDefault;
import static oshi.ffm.ForeignFunctions.callInArenaOrDefault;
import static oshi.software.os.OSProcess.State.INVALID;
import static oshi.software.os.OSThread.ThreadFiltering.VALID_THREAD;

import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.platform.unix.freebsd.FreeBsdLibcFunctions;
import oshi.software.common.os.unix.dragonflybsd.DragonFlyBsdOSProcess;
import oshi.software.common.os.unix.dragonflybsd.DragonFlyBsdOSThread;
import oshi.software.os.OSThread;
import oshi.software.os.unix.dragonflybsd.DragonFlyBsdOperatingSystemFFM.PsKeywords;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.common.platform.unix.dragonflybsd.ProcstatUtil;

/**
 * FFM-backed DragonFly BSD OS process.
 */
@ThreadSafe
public class DragonFlyBsdOSProcessFFM extends DragonFlyBsdOSProcess {

    private static final Logger LOG = LoggerFactory.getLogger(DragonFlyBsdOSProcessFFM.class);

    private final DragonFlyBsdOperatingSystemFFM os;

    public DragonFlyBsdOSProcessFFM(int pid, Map<PsKeywords, String> psMap, DragonFlyBsdOperatingSystemFFM os) {
        super(pid);
        this.os = os;
        updateAttributes(psMap);
    }

    @Override
    protected List<String> queryArguments() {
        // DragonFlyBSD provides command line via /proc filesystem
        byte[] cmdBytes = FileUtil.readAllBytes("/proc/" + getProcessID() + "/cmdline", false);
        if (cmdBytes != null && cmdBytes.length > 0) {
            return Collections.unmodifiableList(ParseUtil.parseByteArrayToStrings(cmdBytes));
        }
        return Collections.emptyList();
    }

    @Override
    protected Map<String, String> queryEnvironmentVariables() {
        // DragonFlyBSD's /proc does not expose environ for other processes.
        // For the current process, use Java's System.getenv().
        int self = callInArenaIntOrDefault(arena -> FreeBsdLibcFunctions.getpid(), LOG, Level.WARN, "getpid failed",
                -1);
        if (getProcessID() == self) {
            return System.getenv();
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
            return queryRlimitNofile(true);
        }
        return getProcessOpenFileLimit(getProcessID(), 1);
    }

    @Override
    public long getHardOpenFileLimit() {
        if (getProcessID() == this.os.getProcessId()) {
            return queryRlimitNofile(false);
        }
        return getProcessOpenFileLimit(getProcessID(), 2);
    }

    private long queryRlimitNofile(boolean soft) {
        return callInArenaOrDefault(arena -> {
            MemorySegment rlim = arena.allocate(FreeBsdLibcFunctions.RLIMIT_LAYOUT);
            if (FreeBsdLibcFunctions.getrlimit(FreeBsdLibcFunctions.RLIMIT_NOFILE, rlim) != 0) {
                return -1L;
            }
            return rlim.get(JAVA_LONG, soft ? 0 : Long.BYTES);
        }, LOG, Level.WARN, "getrlimit(RLIMIT_NOFILE) failed", -1L);
    }

    @Override
    protected int queryBitness() {
        return callInArenaIntOrDefault(arena -> {
            // CTL_KERN.KERN_PROC.KERN_PROC_SV_NAME.<pid>
            MemorySegment mib = arena.allocateFrom(JAVA_INT, 1, 14, 9, getProcessID());
            MemorySegment buf = arena.allocate(32);
            MemorySegment size = arena.allocate(JAVA_LONG);
            size.set(JAVA_LONG, 0, 32L);
            MemorySegment callState = arena.allocate(ADDRESS);
            int rc = FreeBsdLibcFunctions.sysctl(callState, mib, 4, buf, size, MemorySegment.NULL, 0L);
            if (rc != 0) {
                return 0;
            }
            byte[] bytes = buf.asSlice(0, Math.min(size.get(JAVA_LONG, 0), 32L)).toArray(JAVA_BYTE);
            String elf = new String(bytes, StandardCharsets.UTF_8).trim();
            if (elf.contains("ELF32")) {
                return 32;
            } else if (elf.contains("ELF64")) {
                return 64;
            }
            return 0;
        }, LOG, Level.WARN, "queryBitness failed", 0);
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
                .filter(hasColumnsPri).map(threadMap -> new DragonFlyBsdOSThread(getProcessID(), threadMap))
                .filter(VALID_THREAD).collect(Collectors.toList());
    }

    @Override
    public boolean updateAttributes() {
        String psCommand = "ps -awwxo " + DragonFlyBsdOperatingSystemFFM.PS_COMMAND_ARGS + " -p " + getProcessID();
        List<String> procList = ExecutingCommand.runNative(psCommand);
        if (procList.size() > 1) {
            // skip header row
            Map<PsKeywords, String> psMap = ParseUtil.stringToEnumMap(PsKeywords.class, procList.get(1).trim(), ' ');
            // Check if last (thus all) value populated
            if (psMap.containsKey(PsKeywords.COMMAND)) {
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
        this.group = this.user; // DragonFly ps lacks group keyword
        this.groupID = psMap.get(PsKeywords.RGID);
        this.threadCount = ParseUtil.parseIntOrDefault(psMap.get(PsKeywords.NLWP), 0);
        this.priority = ParseUtil.parseIntOrDefault(psMap.get(PsKeywords.PRI), 0);
        // These are in KB, multiply
        this.virtualSize = ParseUtil.parseLongOrDefault(psMap.get(PsKeywords.VSZ), 0) * 1024;
        this.residentSetSize = ParseUtil.parseLongOrDefault(psMap.get(PsKeywords.RSS), 0) * 1024;
        // Get start time from /proc/<pid>/status (field 8: sec,usec)
        this.startTime = queryStartTimeFromProc(getProcessID());
        this.upTime = now - this.startTime;
        if (this.upTime < 1L) {
            this.upTime = 1L;
        }
        this.userTime = ParseUtil.parseDHMSOrDefault(psMap.get(PsKeywords.TIME), 0L);
        this.path = psMap.get(PsKeywords.UCOMM);
        this.name = this.path.substring(this.path.lastIndexOf('/') + 1);
        this.minorFaults = ParseUtil.parseLongOrDefault(psMap.get(PsKeywords.MINFLT), 0L);
        this.majorFaults = ParseUtil.parseLongOrDefault(psMap.get(PsKeywords.MAJFLT), 0L);
        this.voluntaryContextSwitches = ParseUtil.parseLongOrDefault(psMap.get(PsKeywords.NVCSW), 0L);
        this.involuntaryContextSwitches = ParseUtil.parseLongOrDefault(psMap.get(PsKeywords.NIVCSW), 0L);
        this.commandLineBackup = psMap.get(PsKeywords.COMMAND);
        return true;
    }

    private static long queryStartTimeFromProc(int pid) {
        List<String> status = FileUtil.readFile("/proc/" + pid + "/status", false);
        if (!status.isEmpty()) {
            // Format: name pid ... startSec,startUsec ...
            String[] split = ParseUtil.whitespaces.split(status.get(0).trim());
            if (split.length >= 8) {
                String[] timeParts = split[7].split(",");
                long seconds = ParseUtil.parseLongOrDefault(timeParts[0], 0L);
                if (seconds > 0) {
                    return seconds * 1000L;
                }
            }
        }
        return System.currentTimeMillis();
    }
}
