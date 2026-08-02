/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.unix.aix;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Utility to query logged in users.
 */
@ThreadSafe
public final class Who {

    // sample format:
    // system boot 2020-06-16 09:12
    private static final Pattern BOOT_FORMAT_AIX = Pattern.compile("\\D+(\\d{4}-\\d{2}-\\d{2})\\s+(\\d{2}:\\d{2}).*");
    private static final DateTimeFormatter BOOT_DATE_FORMAT_AIX = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm",
            Locale.ROOT);

    // AIX 7.3 defaults to a month-name form that carries no year, in both the C and en_US locales:
    // system boot Feb 13 23:31
    // The day is space-padded, so the groups are rejoined with single spaces to match the pattern below.
    private static final Pattern BOOT_FORMAT_AIX_NO_YEAR = Pattern
            .compile("\\D+?([A-Z][a-z]{2})\\s+(\\d{1,2})\\s+(\\d{2}:\\d{2}).*");
    private static final String BOOT_NO_YEAR_PATTERN_AIX = "MMM d HH:mm";

    private Who() {
    }

    /**
     * Query {@code who -b} to get boot time
     *
     * @return Boot time in milliseconds since the epoch
     */
    public static long queryBootTime() {
        String s = ExecutingCommand.getFirstAnswer("who -b");
        if (s.isEmpty()) {
            s = ExecutingCommand.getFirstAnswer("/usr/bin/who -b");
        }
        return parseBootTime(s);
    }

    /**
     * Parses the {@code who -b} output line into a boot time in milliseconds since the epoch.
     *
     * @param s a line of {@code who -b} output
     * @return boot time in milliseconds since the epoch, or 0 if the line does not match either expected format
     */
    public static long parseBootTime(String s) {
        // The zone is explicit because the year resolution below and the epoch conversion must agree on it
        return parseBootTime(s, LocalDateTime.now(ZoneId.systemDefault()));
    }

    /**
     * Parses the {@code who -b} output line into a boot time in milliseconds since the epoch, resolving the year of the
     * month-name format relative to the given moment.
     *
     * @param s   a line of {@code who -b} output
     * @param now the moment to resolve a year-less timestamp against
     * @return boot time in milliseconds since the epoch, or 0 if the line does not match either expected format
     */
    static long parseBootTime(String s, LocalDateTime now) {
        Matcher m = BOOT_FORMAT_AIX.matcher(s);
        if (m.matches()) {
            try {
                return LocalDateTime.parse(m.group(1) + " " + m.group(2), BOOT_DATE_FORMAT_AIX)
                        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            } catch (DateTimeParseException | NullPointerException e) {
                // Shouldn't happen with regex matching
            }
        }
        m = BOOT_FORMAT_AIX_NO_YEAR.matcher(s);
        if (m.matches()) {
            // A system up for more than a year cannot be resolved from this format, which is why the caller prefers
            // the unambiguous uptime duration when it has one.
            return ParseUtil.parseYearlessDateToEpoch(m.group(1) + " " + m.group(2) + " " + m.group(3),
                    BOOT_NO_YEAR_PATTERN_AIX, now);
        }
        return 0L;
    }
}
