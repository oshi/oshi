/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.freebsd;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import oshi.software.common.AbstractOSProcess;

public abstract class FreeBsdOSProcess extends AbstractOSProcess {

    /**
     * Columns requested from {@code ps -awwxo} when enumerating threads. Shared by FreeBsdOSProcess subclasses and
     * FreeBsdOSThread so the column list and parsing enum stay in lockstep.
     */
    public enum PsThreadColumns {
        TDNAME, LWP, STATE, ETIMES, SYSTIME, TIME, TDADDR, NIVCSW, NVCSW, MAJFLT, MINFLT, PRI;
    }

    public static final String PS_THREAD_COLUMNS = Arrays.stream(PsThreadColumns.values()).map(Enum::name)
            .map(name -> name.toLowerCase(Locale.ROOT)).collect(Collectors.joining(","));

    protected FreeBsdOSProcess(int pid) {
        super(pid);
    }
}
