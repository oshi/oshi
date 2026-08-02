/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.unix.aix;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Utility to query up time.
 */
@ThreadSafe
public final class Uptime {

    private static final long MINUTE_MS = 60L * 1000L;
    private static final long HOUR_MS = 60L * MINUTE_MS;
    private static final long DAY_MS = 24L * HOUR_MS;

    // sample format:
    // 18:36pm up 10 days 8:11, 2 users, load average: 3.14, 2.74, 2.41

    // Assembled from parts so no single line runs long. Group numbering runs straight through the concatenation, so
    // the numbers parseUpTime relies on are unchanged: 2 = days, 4 = hours, 5 = minutes.
    private static final String PREFIX = ".*\\sup\\s+";
    private static final String DAYS = "((\\d+)\\s+days?,?\\s+)?";
    private static final String HOURS_MINUTES = "\\b((\\d+):)?(\\d+)(\\s+min(s|utes?)?)?";
    private static final String USERS = ",\\s+\\d+\\s+user.+";
    private static final Pattern UPTIME_FORMAT_AIX = Pattern.compile(PREFIX + DAYS + HOURS_MINUTES + USERS);

    private Uptime() {
    }

    /**
     * Query {@code uptime} to get up time
     *
     * @return Up time in milliseconds
     */
    public static long queryUpTime() {
        String s = ExecutingCommand.getFirstAnswer("uptime");
        if (s.isEmpty()) {
            s = ExecutingCommand.getFirstAnswer("w");
        }
        if (s.isEmpty()) {
            s = ExecutingCommand.getFirstAnswer("/usr/bin/uptime");
        }
        return parseUpTime(s);
    }

    /**
     * Parses the {@code uptime} (or {@code w}) output line into an uptime in milliseconds.
     *
     * @param s a line of {@code uptime} output
     * @return up time in milliseconds, or 0 if the line does not match the expected format
     */
    public static long parseUpTime(String s) {
        long uptime = 0L;
        Matcher m = UPTIME_FORMAT_AIX.matcher(s);
        if (m.matches()) {
            if (m.group(2) != null) {
                uptime += ParseUtil.parseLongOrDefault(m.group(2), 0L) * DAY_MS;
            }
            if (m.group(4) != null) {
                uptime += ParseUtil.parseLongOrDefault(m.group(4), 0L) * HOUR_MS;
            }
            uptime += ParseUtil.parseLongOrDefault(m.group(5), 0L) * MINUTE_MS;
        }
        return uptime;
    }
}
