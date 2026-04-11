/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.unix;

import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.os.OSSession;
import oshi.util.ExecutingCommand;

/**
 * Utility to query logged in users using the {@code who} command with Unix date format parsing.
 */
@ThreadSafe
public final class Who {

    // oshi ttys000 May 4 23:50 (192.168.1.23)
    // middle 12 characters from Thu Nov 24 18:22:48 1986
    private static final Pattern WHO_FORMAT_UNIX = Pattern
            .compile("(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\d+)\\s+(\\d{2}:\\d{2})\\s*(?:\\((.+)\\))?");
    private static final DateTimeFormatter WHO_DATE_FORMAT_UNIX = new DateTimeFormatterBuilder()
            .appendPattern("MMM d HH:mm").parseDefaulting(ChronoField.YEAR, Year.now(ZoneId.systemDefault()).getValue())
            .toFormatter(Locale.US);

    private Who() {
    }

    /**
     * Query {@code who} to get logged in users, parsing Unix date format.
     *
     * @return A list of logged in user sessions
     */
    public static synchronized List<OSSession> queryWho() {
        List<OSSession> whoList = new ArrayList<>();
        for (String s : ExecutingCommand.runNative("who")) {
            matchUnix(whoList, s);
        }
        return whoList;
    }

    /**
     * Attempt to match Unix WHO format and add to the list.
     *
     * @param whoList the list to add to
     * @param s       the string to match
     * @return true if successful, false otherwise
     */
    public static boolean matchUnix(List<OSSession> whoList, String s) {
        Matcher m = WHO_FORMAT_UNIX.matcher(s);
        if (m.matches()) {
            try {
                // Missing year, parse date time with current year
                LocalDateTime login = LocalDateTime.parse(m.group(3) + " " + m.group(4) + " " + m.group(5),
                        WHO_DATE_FORMAT_UNIX);
                // If this date is in the future, subtract a year
                if (login.isAfter(LocalDateTime.now(ZoneId.systemDefault()))) {
                    login = login.minus(1, ChronoUnit.YEARS);
                }
                long millis = login.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                whoList.add(new OSSession(m.group(1), m.group(2), millis, m.group(6) == null ? "" : m.group(6)));
                return true;
            } catch (DateTimeParseException | NullPointerException e) {
                // shouldn't happen if regex matches and OS is producing sensible dates
            }
        }
        return false;
    }
}
