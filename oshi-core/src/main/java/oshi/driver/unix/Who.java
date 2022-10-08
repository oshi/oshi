/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.unix;

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

import com.sun.jna.Platform;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.os.OSSession;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;

/**
 * Utility to query logged in users.
 */
@ThreadSafe
public final class Who {

    // sample format:
    // oshi pts/0 2020-05-14 21:23 (192.168.1.23)
    private static final Pattern WHO_FORMAT_LINUX = Pattern
            .compile("(\\S+)\\s+(\\S+)\\s+(\\d{4}-\\d{2}-\\d{2})\\s+(\\d{2}:\\d{2})\\s*(?:\\((.+)\\))?");
    private static final DateTimeFormatter WHO_DATE_FORMAT_LINUX = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    // oshi ttys000 May 4 23:50 (192.168.1.23)
    // middle 12 characters from Thu Nov 24 18:22:48 1986
    private static final Pattern WHO_FORMAT_UNIX = Pattern
            .compile("(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\d+)\\s+(\\d{2}:\\d{2})\\s*(?:\\((.+)\\))?");
    private static final DateTimeFormatter WHO_DATE_FORMAT_UNIX = new DateTimeFormatterBuilder()
            .appendPattern("MMM d HH:mm").parseDefaulting(ChronoField.YEAR, Year.now().getValue())
            .toFormatter(Locale.US);

    private Who() {
    }

    /**
     * Query {@code who} to get logged in users
     *
     * @return A list of logged in user sessions
     */
    public static synchronized List<OSSession> queryWho() {
        List<OSSession> whoList = new ArrayList<>();
        List<String> who = ExecutingCommand.runNative("who");
        for (String s : who) {
            boolean matched = false;
            if (Platform.isLinux()) {
                matched = matchLinux(whoList, s);
            }
            if (!matched) {
                matchUnix(whoList, s);
            }
        }
        return whoList;
    }

    /**
     * Attempt to match Linux WHO format and add to the list
     *
     * @param whoList the list to add to
     * @param s       the string to match
     * @return true if successful, false otherwise
     */
    private static boolean matchLinux(List<OSSession> whoList, String s) {
        Matcher m = WHO_FORMAT_LINUX.matcher(s);
        if (m.matches()) {
            try {
                whoList.add(new OSSession(m.group(1), m.group(2),
                        LocalDateTime.parse(m.group(3) + " " + m.group(4), WHO_DATE_FORMAT_LINUX)
                                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        m.group(5) == null ? Constants.UNKNOWN : m.group(5)));
                return true;
            } catch (DateTimeParseException | NullPointerException e) {
                // shouldn't happen if regex matches and OS is producing sensible dates
            }
        }
        return false;
    }

    /**
     * Attempt to match Unix WHO format and add to the list
     *
     * @param whoList the list to add to
     * @param s       the string to match
     * @return true if successful, false otherwise
     */
    private static boolean matchUnix(List<OSSession> whoList, String s) {
        Matcher m = WHO_FORMAT_UNIX.matcher(s);
        if (m.matches()) {
            try {
                // Missing year, parse date time with current year
                LocalDateTime login = LocalDateTime.parse(m.group(3) + " " + m.group(4) + " " + m.group(5),
                        WHO_DATE_FORMAT_UNIX);
                // If this date is in the future, subtract a year
                if (login.isAfter(LocalDateTime.now())) {
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
