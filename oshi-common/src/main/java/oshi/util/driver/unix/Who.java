/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.unix;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.os.OSSession;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Utility to query logged in users using the {@code who} command with Unix date format parsing.
 */
@ThreadSafe
public final class Who {

    // oshi ttys000 May 4 23:50 (192.168.1.23)
    // middle 12 characters from Thu Nov 24 18:22:48 1986
    private static final Pattern WHO_FORMAT_UNIX = Pattern
            .compile("(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\d+)\\s+(\\d{2}:\\d{2})\\s*(?:\\((.+)\\))?");
    private static final String WHO_DATE_PATTERN_UNIX = "MMM d HH:mm";

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
            // The who output carries no year. Resolving it against the moment of the call rather than a year captured
            // when this class loaded keeps a long-running JVM correct across a new year.
            long millis = ParseUtil.parseYearlessDateToEpoch(m.group(3) + " " + m.group(4) + " " + m.group(5),
                    WHO_DATE_PATTERN_UNIX, LocalDateTime.now(ZoneId.systemDefault()));
            if (millis > 0) {
                whoList.add(new OSSession(m.group(1), m.group(2), millis, m.group(6) == null ? "" : m.group(6)));
                return true;
            }
        }
        return false;
    }
}
