/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.linux;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.os.OSSession;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.Util;

/**
 * Utility to query logged in users using the {@code who} command with Linux date format parsing, falling back to Unix
 * format.
 */
@ThreadSafe
public final class Who {

    private static final Logger LOG = LoggerFactory.getLogger(Who.class);

    /** Where systemd records one file per active session. */
    private static final File SYSTEMD_SESSIONS_DIR = new File("/run/systemd/sessions");

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
    static boolean matchLinux(List<OSSession> whoList, String s) {
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
     * Queries logged-in sessions from the files systemd keeps under {@code /run/systemd/sessions}, as a fallback for
     * when a native systemd query is unavailable or fails.
     * <p>
     * Shared by the bindings because it reads only files: nothing here is backend-specific.
     *
     * @return the sessions systemd records, or an empty list if the directory is absent or unreadable
     */
    public static List<OSSession> querySystemdFiles() {
        return querySystemdFiles(SYSTEMD_SESSIONS_DIR);
    }

    /**
     * Queries logged-in sessions from a systemd sessions directory. Package-private so tests can supply a directory
     * instead of requiring a running systemd.
     *
     * @param sessionsDir the directory holding one file per session
     * @return the sessions it records, or an empty list if the directory is absent or unreadable
     */
    static List<OSSession> querySystemdFiles(File sessionsDir) {
        List<OSSession> sessionList = new ArrayList<>();
        if (!sessionsDir.isDirectory()) {
            return sessionList;
        }
        File[] sessionFiles = sessionsDir.listFiles(file -> Constants.DIGITS.matcher(file.getName()).matches());
        if (sessionFiles == null) {
            return sessionList;
        }
        for (File sessionFile : sessionFiles) {
            try {
                Map<String, String> sessionMap = FileUtil.getKeyValueMapFromFile(sessionFile.getPath(), "=");
                String user = sessionMap.get("USER");
                if (user == null || user.isEmpty()) {
                    continue;
                }
                String tty = sessionMap.getOrDefault("TTY", sessionFile.getName());
                String remoteHost = sessionMap.getOrDefault("REMOTE_HOST", "");
                // REALTIME is microseconds since the epoch; fall back to the file's own timestamp
                long loginTime = ParseUtil.parseLongOrDefault(sessionMap.get("REALTIME"), 0L) / 1000L;
                if (loginTime == 0L) {
                    loginTime = sessionFile.lastModified();
                }
                if (Util.isSessionValid(user, tty, loginTime)) {
                    sessionList.add(new OSSession(user, tty, loginTime, remoteHost));
                }
            } catch (Exception e) {
                LOG.debug("Skipping unreadable systemd session file {}: {}", sessionFile, e.getMessage());
            }
        }
        return sessionList;
    }
}
