/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.util.platform.windows;

import oshi.ffm.windows.Kernel32FFM;

/**
 * FFM-based utility for Windows Kernel32 system information operations.
 */
public final class Kernel32UtilFFM {

    private Kernel32UtilFFM() {
    }

    /**
     * Queries the system uptime in seconds.
     *
     * @return the system uptime in seconds
     */
    public static long querySystemUptime() {
        return Kernel32FFM.GetTickCount().orElse(-1) / 1000L;
    }

    /**
     * Gets the NetBIOS computer name.
     *
     * @return the computer name, or empty string on failure
     */
    public static String getComputerName() {
        return Kernel32FFM.GetComputerName().orElse("");
    }

    /**
     * Gets the fully qualified DNS computer name.
     *
     * @return the DNS computer name, or empty string on failure
     */
    public static String getComputerNameEx() {
        return Kernel32FFM.GetComputerNameEx().orElse("");
    }
}
