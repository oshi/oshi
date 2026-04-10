/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.linux.proc;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.linux.ProcPath;

/**
 * Utility to read system uptime from {@code /proc/uptime}
 */
@ThreadSafe
public final class UpTime {

    private UpTime() {
    }

    /**
     * Parses the first value in {@code /proc/uptime} for seconds since boot
     *
     * @return Seconds since boot
     */
    public static double getSystemUptimeSeconds() {
        String uptime = FileUtil.getStringFromFile(ProcPath.UPTIME);
        int spaceIndex = uptime.indexOf(' ');
        if (spaceIndex < 0) {
            // No space, error
            return 0d;
        }
        return ParseUtil.parseDoubleOrDefault(uptime.substring(0, spaceIndex), 0d);
    }
}
