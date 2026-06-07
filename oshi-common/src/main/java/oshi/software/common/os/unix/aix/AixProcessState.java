/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.aix;

import static oshi.software.os.OSProcess.State.INVALID;
import static oshi.software.os.OSProcess.State.OTHER;
import static oshi.software.os.OSProcess.State.RUNNING;
import static oshi.software.os.OSProcess.State.SLEEPING;
import static oshi.software.os.OSProcess.State.STOPPED;
import static oshi.software.os.OSProcess.State.WAITING;
import static oshi.software.os.OSProcess.State.ZOMBIE;

import oshi.software.os.OSProcess;

/**
 * Maps AIX {@code pr_sname} characters (from {@code psinfo_t} / {@code lwpsinfo_t}) to OSHI's {@link OSProcess.State}
 * enum. Shared between {@code AixOSProcess} and {@code AixOSThread} so both the JNA and FFM backends classify states
 * identically.
 */
public final class AixProcessState {

    private AixProcessState() {
    }

    /**
     * Returns the {@link OSProcess.State} corresponding to the {@code pr_sname} character from an AIX
     * {@code psinfo_t}/{@code lwpsinfo_t} structure.
     *
     * @param stateValue {@code pr_sname} character ({@code 'O'}, {@code 'R'}, {@code 'A'}, {@code 'I'}, …)
     * @return the matching {@link OSProcess.State}
     */
    public static OSProcess.State getStateFromOutput(char stateValue) {
        OSProcess.State state;
        switch (stateValue) {
            case 'O':
                state = INVALID;
                break;
            case 'R':
            case 'A':
                state = RUNNING;
                break;
            case 'I':
                state = WAITING;
                break;
            case 'S':
            case 'W':
                state = SLEEPING;
                break;
            case 'Z':
                state = ZOMBIE;
                break;
            case 'T':
                state = STOPPED;
                break;
            default:
                state = OTHER;
                break;
        }
        return state;
    }
}
