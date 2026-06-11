/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.dragonflybsd;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import oshi.software.common.os.unix.bsd.BsdOSProcess;

public abstract class DragonFlyBsdOSProcess extends BsdOSProcess {

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
}
