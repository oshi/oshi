/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.openbsd;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import oshi.software.common.AbstractOSProcess;

public abstract class OpenBsdOSProcess extends AbstractOSProcess {

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
}
