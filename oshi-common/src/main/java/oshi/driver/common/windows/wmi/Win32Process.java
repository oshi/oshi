/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.wmi;

import java.util.Collection;
import java.util.stream.Collectors;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Constants, property enums, and WHERE clause builder for WMI class {@code Win32_Process}.
 */
@ThreadSafe
public class Win32Process {

    /**
     * The WMI class name.
     */
    public static final String WIN32_PROCESS = "Win32_Process";

    /**
     * Process command lines.
     */
    public enum CommandLineProperty {
        PROCESSID, COMMANDLINE;
    }

    /**
     * Process properties accessible from WTSEnumerateProcesses in Vista+.
     */
    public enum ProcessXPProperty {
        PROCESSID, NAME, KERNELMODETIME, USERMODETIME, THREADCOUNT, PAGEFILEUSAGE, HANDLECOUNT, EXECUTABLEPATH;
    }

    protected Win32Process() {
    }

    /**
     * Builds the WMI class name with optional WHERE clause filtering by process IDs.
     *
     * @param pids Process IDs to filter, or {@code null} to query all processes
     * @return the WMI class name with WHERE clause appended if pids is non-null
     */
    public static String buildWmiClassNameWithPidFilter(Collection<Integer> pids) {
        if (pids == null || pids.isEmpty()) {
            return WIN32_PROCESS;
        }
        return WIN32_PROCESS + " WHERE ProcessID="
                + pids.stream().map(String::valueOf).collect(Collectors.joining(" OR PROCESSID="));
    }
}
