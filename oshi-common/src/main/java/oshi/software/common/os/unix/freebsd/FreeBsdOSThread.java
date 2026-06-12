/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.freebsd;

import static oshi.software.common.os.unix.bsd.BsdPsThreadKeyword.ETIMES;
import static oshi.software.common.os.unix.bsd.BsdPsThreadKeyword.LWP;
import static oshi.software.common.os.unix.bsd.BsdPsThreadKeyword.MAJFLT;
import static oshi.software.common.os.unix.bsd.BsdPsThreadKeyword.MINFLT;
import static oshi.software.common.os.unix.bsd.BsdPsThreadKeyword.NIVCSW;
import static oshi.software.common.os.unix.bsd.BsdPsThreadKeyword.NVCSW;
import static oshi.software.common.os.unix.bsd.BsdPsThreadKeyword.PRI;
import static oshi.software.common.os.unix.bsd.BsdPsThreadKeyword.STATE;
import static oshi.software.common.os.unix.bsd.BsdPsThreadKeyword.SYSTIME;
import static oshi.software.common.os.unix.bsd.BsdPsThreadKeyword.TDADDR;
import static oshi.software.common.os.unix.bsd.BsdPsThreadKeyword.TDNAME;
import static oshi.software.common.os.unix.bsd.BsdPsThreadKeyword.TIME;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.os.unix.bsd.BsdOSThread;
import oshi.software.common.os.unix.bsd.BsdPsThreadKeyword;

/**
 * OSThread implementation
 */
@ThreadSafe
public class FreeBsdOSThread extends BsdOSThread {

    /**
     * Ordered {@code ps} thread columns. Shared with FreeBsdOSProcess's thread enumeration so the column list and
     * parsing stay in lockstep. {@code PRI} must remain last (the row-complete sentinel).
     */
    public static final List<BsdPsThreadKeyword> PS_THREAD_KEYWORDS = Collections.unmodifiableList(
            Arrays.asList(TDNAME, LWP, STATE, ETIMES, SYSTIME, TIME, TDADDR, NIVCSW, NVCSW, MAJFLT, MINFLT, PRI));

    public static final String PS_THREAD_COLUMNS = PS_THREAD_KEYWORDS.stream().map(Enum::name)
            .map(name -> name.toLowerCase(Locale.ROOT)).collect(Collectors.joining(","));

    public FreeBsdOSThread(int processId, Map<BsdPsThreadKeyword, String> threadMap) {
        super(processId);
        updateAttributes(threadMap);
    }

    public FreeBsdOSThread(int processId, int threadId) {
        super(processId);
        this.threadId = threadId;
        updateAttributes();
    }

    @Override
    protected List<BsdPsThreadKeyword> psThreadKeywords() {
        return PS_THREAD_KEYWORDS;
    }

    @Override
    protected String psThreadCommand() {
        return "ps -awwxo " + PS_THREAD_COLUMNS + " -H";
    }
}
