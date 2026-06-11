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
import oshi.software.os.OSThread;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;

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
     * Columns requested from {@code ps -awwxo} when enumerating threads. Shared by DragonFlyBsdOSProcess subclasses and
     * DragonFlyBsdOSThread so the column list and parsing enum stay in lockstep.
     */
    public enum PsThreadColumns {
        TID, STATE, TIME, MAJFLT, MINFLT, NVCSW, NIVCSW, PRI;
    }

    public static final String PS_THREAD_COLUMNS = Arrays.stream(PsThreadColumns.values()).map(Enum::name)
            .map(name -> name.toLowerCase(Locale.ROOT)).collect(Collectors.joining(","));

    protected DragonFlyBsdOSProcess(int pid) {
        super(pid);
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
    protected long queryStartTimeMillis() {
        return queryStartTimeFromProc(getProcessID());
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
        // Unknown start time: return the sentinel so updateAttributes() falls back to elapsed time
        return -1L;
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
}
