/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.openbsd;

import static oshi.software.common.os.unix.bsd.BsdPsKeyword.ARGS;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.COMM;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.CPUTIME;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.ETIME;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.GID;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.GROUP;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.MAJFLT;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.MINFLT;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.NIVCSW;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.NVCSW;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.PID;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.PPID;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.PRI;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.RSS;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.STATE;
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
import oshi.util.ParseUtil;

public abstract class OpenBsdOSProcess extends BsdOSProcess {

    /**
     * Ordered {@code ps} columns queried for each process. Shared by the OpenBsdOSProcess subclasses (JNA/FFM) and the
     * OpenBSD OperatingSystem classes so the column list and parsing stay in lockstep. {@code ARGS} must remain last.
     */
    public static final List<BsdPsKeyword> PS_KEYWORDS = Collections.unmodifiableList(Arrays.asList(STATE, PID, PPID,
            USER, UID, GROUP, GID, PRI, VSZ, RSS, ETIME, CPUTIME, COMM, MAJFLT, MINFLT, NVCSW, NIVCSW, ARGS));

    public static final String PS_COMMAND_ARGS = PS_KEYWORDS.stream().map(Enum::name)
            .map(name -> name.toLowerCase(Locale.ROOT)).collect(Collectors.joining(","));

    /**
     * Columns requested from {@code ps -aHwwxo} when enumerating threads. Shared by OpenBsdOSProcess subclasses and
     * OpenBsdOSThread so the column list and parsing enum stay in lockstep.
     */
    public enum PsThreadColumns {
        TID, STATE, ETIME, CPUTIME, NIVCSW, NVCSW, MAJFLT, MINFLT, PRI, ARGS;
    }

    public static final String PS_THREAD_COLUMNS = Arrays.stream(PsThreadColumns.values()).map(Enum::name)
            .map(name -> name.toLowerCase(Locale.ROOT)).collect(Collectors.joining(","));

    protected OpenBsdOSProcess(int pid) {
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
    protected void updateThreadCount() {
        List<String> threadList = ExecutingCommand.runNative("ps -axHo tid -p " + getProcessID());
        if (!threadList.isEmpty()) {
            // Subtract 1 for header
            this.threadCount = threadList.size() - 1;
        }
        // A live process always has at least one thread
        this.threadCount = Math.max(this.threadCount, 1);
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
}
