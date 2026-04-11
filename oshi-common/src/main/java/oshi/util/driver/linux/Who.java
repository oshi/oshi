/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.linux;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.os.OSSession;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;

/**
 * Utility to query logged in users using the {@code who} command with Linux date format parsing, falling back to Unix
 * format.
 */
@ThreadSafe
public final class Who {

    // oshi pts/0 2020-05-14 21:23 (192.168.1.23)
    private static final Pattern WHO_FORMAT_LINUX = Pattern
            .compile("(\\S+)\\s+(\\S+)\\s+(\\d{4}-\\d{2}-\\d{2})\\s+(\\d{2}:\\d{2})\\s*(?:\\((.+)\\))?");
    private static final DateTimeFormatter WHO_DATE_FORMAT_LINUX = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm",
            Locale.ROOT);

    private Who() {
    }

    /**
     * Query {@code who} to get logged in users, trying Linux date format first, then Unix format.
     *
     * @return A list of logged in user sessions
     */
    public static synchronized List<OSSession> queryWho() {
        List<OSSession> whoList = new ArrayList<>();
        for (String s : ExecutingCommand.runNative("who")) {
            if (!matchLinux(whoList, s)) {
                oshi.util.driver.unix.Who.matchUnix(whoList, s);
            }
        }
        return whoList;
    }

    /**
     * Attempt to match Linux WHO format and add to the list.
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
}
