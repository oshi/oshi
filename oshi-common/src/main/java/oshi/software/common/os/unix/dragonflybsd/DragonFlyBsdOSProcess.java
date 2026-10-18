/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.dragonflybsd;

import static oshi.software.common.os.unix.bsd.BsdPsKeyword.COMMAND;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.MAJFLT;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.MINFLT;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.NIVCSW;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.NLWP;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.NVCSW;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.PID;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.PPID;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.PRI;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.RGID;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.RSS;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.STATE;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.TIME;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.UCOMM;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.UID;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.USER;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.VSZ;
import static oshi.software.os.OSThread.ThreadFiltering.VALID_THREAD;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import oshi.software.common.os.unix.bsd.BsdOSProcess;
import oshi.software.common.os.unix.bsd.BsdPsKeyword;
import oshi.software.common.os.unix.bsd.BsdPsThreadKeyword;
import oshi.software.os.OSThread;
import oshi.software.os.OperatingSystem;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.common.platform.unix.dragonflybsd.ProcstatUtil;

/**
 * Abstract base for a DragonFly BSD OSProcess. Inherits the shared BSD {@code ps} parsing and adds the fields whose
 * column set differs on DragonFly.
 */
public abstract class DragonFlyBsdOSProcess extends BsdOSProcess {

    /**
     * Ordered {@code ps} columns queried for each process. Shared by the DragonFlyBsdOSProcess subclasses (JNA/FFM) and
     * the DragonFly OperatingSystem classes so the column list and parsing stay in lockstep. {@code COMMAND} must
     * remain last.
     */
    public static final List<BsdPsKeyword> PS_KEYWORDS = Collections.unmodifiableList(Arrays.asList(STATE, PID, PPID,
            USER, UID, RGID, NLWP, PRI, VSZ, RSS, TIME, MAJFLT, MINFLT, NVCSW, NIVCSW, UCOMM, COMMAND));

    public static final String PS_COMMAND_ARGS = PS_KEYWORDS.stream().map(Enum::name)
            .map(name -> name.toLowerCase(Locale.ROOT)).collect(Collectors.joining(","));

    /**
     * The {@code KERN_PROC} sysctl subtype that returns a process's executable path (12 on FreeBSD).
     */
    protected static final int KERN_PROC_PATHNAME = 9;

    /**
     * The size of the buffer for an executable path, the platform's {@code PATH_MAX}.
     */
    protected static final int PATH_MAX = 1024;

    private final OperatingSystem os;

    /**
     * Constructs a new {@code DragonFlyBsdOSProcess}.
     *
     * @param pid the process ID
     * @param os  the operating system this process runs on
     */
    protected DragonFlyBsdOSProcess(int pid, OperatingSystem os) {
        super(pid);
        this.os = os;
    }

    @Override
    protected List<BsdPsKeyword> psKeywords() {
        return PS_KEYWORDS;
    }

    @Override
    protected String psCommandArgs() {
        return PS_COMMAND_ARGS;
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
    protected int queryOwnProcessId() {
        return this.os.getProcessId();
    }

    @Override
    protected int queryBitness() {
        // DragonFly BSD has no KERN_PROC_SV_NAME (its KERN_PROC subtype 9 is the executable's path) and no ABI
        // compatibility layer: an x86_64 kernel runs only 64-bit programs (kern.elf64 exists, kern.elf32 does not), and
        // the i386 kernel of 3.8 and earlier only 32-bit ones. So every process has the operating system's bitness.
        return this.os.getBitness();
    }

    @Override
    protected long queryStartTimeMillis() {
        return queryStartTimeFromProc(getProcessID());
    }

    private static long queryStartTimeFromProc(int pid) {
        List<String> status = FileUtil.readFile("/proc/" + pid + "/status", false);
        if (!status.isEmpty()) {
            // Format: name pid ... startSec,startUsec ...
            String[] split = ParseUtil.whitespaces.split(status.get(0).trim(), -1);
            if (split.length >= 8) {
                String[] timeParts = split[7].split(",", -1);
                long seconds = ParseUtil.parseLongOrDefault(timeParts[0], 0L);
                if (seconds > 0) {
                    return seconds * 1000L;
                }
            }
        }
        // Unknown start time: return the sentinel so updateAttributes() falls back to elapsed time
        return -1L;
    }

    @Override
    public List<OSThread> getThreadDetails() {
        String psCommand = "ps -awwxo " + DragonFlyBsdOSThread.PS_THREAD_COLUMNS + " -H";
        if (getProcessID() >= 0) {
            psCommand += " -p " + getProcessID();
        }
        Predicate<Map<BsdPsThreadKeyword, String>> hasColumnsPri = threadMap -> threadMap
                .containsKey(BsdPsThreadKeyword.PRI);
        return ExecutingCommand.runNative(psCommand).stream().skip(1).parallel()
                .map(thread -> ParseUtil.stringToEnumMap(BsdPsThreadKeyword.class,
                        DragonFlyBsdOSThread.PS_THREAD_KEYWORDS, thread.trim(), ' '))
                .filter(hasColumnsPri).map(threadMap -> new DragonFlyBsdOSThread(getProcessID(), threadMap))
                .filter(VALID_THREAD).collect(Collectors.toList());
    }

    @Override
    public long getAffinityMask() {
        // DragonFly has no cpuset command, which the other BSDs read affinity from
        return queryAffinityMask();
    }

    /**
     * Reads this process's CPU affinity mask through {@code sched_getaffinity}, which reports the mask of its first LWP
     * limited to the active CPUs. The kernel allows it for any process.
     *
     * @return the mask of CPUs 0 to 63, or 0 if the process has exited
     */
    protected abstract long queryAffinityMask();
}
