/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.linux.proc;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.FileUtil;
import oshi.util.platform.linux.ProcPath;

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
        try {
            if (spaceIndex < 0) {
                // No space, error
                return 0d;
            }
            return Double.parseDouble(uptime.substring(0, spaceIndex));
        } catch (NumberFormatException nfe) {
            return 0d;
        }
    }
}
