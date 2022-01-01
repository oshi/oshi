/*
 * MIT License
 *
 * Copyright (c) 2020-2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
        boolean useUnix = false;
        for (String s : ExecutingCommand.runNative("who")) {
            if (useUnix || !matchLinux(whoList, s)) {
                useUnix = matchUnix(whoList, s);
            }
        }
        return whoList;
    }

    /**
     * Attempt to match Linux WHO format and add to the list
     *
     * @param whoList
     *            the list to add to
     * @param s
     *            the string to match
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
     * @param whoList
     *            the list to add to
     * @param s
     *            the string to match
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
