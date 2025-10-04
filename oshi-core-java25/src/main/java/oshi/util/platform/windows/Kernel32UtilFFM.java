/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.windows;

import oshi.ffm.windows.Kernel32FFM;

public final class Kernel32UtilFFM {

    private Kernel32UtilFFM() {
    }

    public static long querySystemUptime() {
        return Kernel32FFM.GetTickCount().orElse(-1) / 1000L;
    }

    public static String getComputerName() {
        return Kernel32FFM.GetComputerName().orElse("");
    }

    public static String getComputerNameEx() {
        return Kernel32FFM.GetComputerNameEx().orElse("");
    }
}
