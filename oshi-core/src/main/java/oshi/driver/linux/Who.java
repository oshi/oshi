/*
 * Copyright 2020-2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.linux;

import static oshi.jna.platform.unix.CLibrary.LOGIN_PROCESS;
import static oshi.jna.platform.unix.CLibrary.USER_PROCESS;
import static oshi.util.Util.isSessionValid;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sun.jna.Native;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.platform.linux.LinuxLibc;
import oshi.jna.platform.linux.LinuxLibc.LinuxUtmpx;
import oshi.jna.platform.linux.Systemd;
import oshi.software.os.OSSession;
import oshi.util.Constants;
import oshi.util.FileUtil;
import oshi.util.GlobalConfig;
import oshi.util.ParseUtil;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;

/**
 * Utility to query logged in users.
 */
@ThreadSafe
public final class Who {

    private static final LinuxLibc LIBC = LinuxLibc.INSTANCE;

    /** This static field identifies if the systemd library can be loaded. */
    public static final boolean HAS_SYSTEMD;

    static {
        boolean hasSystemd = false;
        try {
            if (GlobalConfig.get(GlobalConfig.OSHI_OS_LINUX_ALLOWSYSTEMD, true)) {
                @SuppressWarnings("unused")
                Systemd lib = Systemd.INSTANCE;
                hasSystemd = true;
            }
        } catch (UnsatisfiedLinkError e) {
            // systemd not available
        }
        HAS_SYSTEMD = hasSystemd;
    }

    private Who() {
    }

    /**
     * Query {@code getutxent} to get logged in users.
     *
     * @return A list of logged in user sessions
     */
    public static synchronized List<OSSession> queryUtxent() {
        // Try systemd first if available
        if (HAS_SYSTEMD) {
            List<OSSession> systemdSessions = querySystemdNative();
            if (!systemdSessions.isEmpty()) {
                return systemdSessions;
            }
        }

        List<OSSession> whoList = new ArrayList<>();
        LinuxUtmpx ut;
        // Rewind
        LIBC.setutxent();
        try {
            // Iterate
            while ((ut = LIBC.getutxent()) != null) {
                if (ut.ut_type == USER_PROCESS || ut.ut_type == LOGIN_PROCESS) {
                    String user = Native.toString(ut.ut_user, Charset.defaultCharset());
                    String device = Native.toString(ut.ut_line, Charset.defaultCharset());
                    String host = ParseUtil.parseUtAddrV6toIP(ut.ut_addr_v6);
                    long loginTime = ut.ut_tv.tv_sec * 1000L + ut.ut_tv.tv_usec / 1000L;
                    // Sanity check. If errors, default to who command line
                    if (!isSessionValid(user, device, loginTime)) {
                        return oshi.driver.unix.Who.queryWho();
                    }
                    whoList.add(new OSSession(user, device, loginTime, host));
                }
            }
        } finally {
            // Close
            LIBC.endutxent();
        }

        // If utmp returned no sessions, try systemd file fallback
        if (whoList.isEmpty()) {
            whoList = querySystemdFiles();
            if (whoList.isEmpty()) {
                // Final fallback to who command
                return oshi.driver.unix.Who.queryWho();
            }
        }
        return whoList;
    }

    /**
     * Query systemd sessions using native libsystemd calls.
     *
     * @return A list of logged in user sessions from systemd
     */
    private static List<OSSession> querySystemdNative() {
        List<OSSession> sessionList = new ArrayList<>();

        try {
            PointerByReference sessionsPtr = new PointerByReference();
            int count = Systemd.INSTANCE.sd_get_sessions(sessionsPtr);

            if (count > 0) {
                Pointer sessions = sessionsPtr.getValue();
                if (sessions != null) {
                    String[] sessionIds = sessions.getStringArray(0, count);

                    for (String sessionId : sessionIds) {
                        if (sessionId == null)
                            continue;
                        try {
                            PointerByReference usernamePtr = new PointerByReference();
                            PointerByReference ttyPtr = new PointerByReference();
                            PointerByReference remoteHostPtr = new PointerByReference();
                            LongByReference startTimePtr = new LongByReference();

                            if (Systemd.INSTANCE.sd_session_get_username(sessionId, usernamePtr) == 0
                                    && Systemd.INSTANCE.sd_session_get_start_time(sessionId, startTimePtr) == 0
                                    && usernamePtr.getValue() != null) {

                                String user = usernamePtr.getValue().getString(0);
                                long loginTime = startTimePtr.getValue() / 1000L; // Convert μs to ms

                                String tty = sessionId; // Default to session ID
                                if (Systemd.INSTANCE.sd_session_get_tty(sessionId, ttyPtr) == 0
                                        && ttyPtr.getValue() != null) {
                                    tty = ttyPtr.getValue().getString(0);
                                }

                                String remoteHost = "";
                                if (Systemd.INSTANCE.sd_session_get_remote_host(sessionId, remoteHostPtr) == 0
                                        && remoteHostPtr.getValue() != null) {
                                    remoteHost = remoteHostPtr.getValue().getString(0);
                                }

                                if (isSessionValid(user, tty, loginTime)) {
                                    sessionList.add(new OSSession(user, tty, loginTime, remoteHost));
                                }
                            }
                        } catch (Exception e) {
                            // Skip invalid session
                        }
                    }
                }
            }
        } catch (Exception e) {
            // systemd calls failed, return empty list
        }

        return sessionList;
    }

    /**
     * Query systemd sessions from files as fallback when native calls fail.
     *
     * @return A list of logged in user sessions from systemd
     */
    private static List<OSSession> querySystemdFiles() {
        List<OSSession> sessionList = new ArrayList<>();

        // Directly iterate /run/systemd/sessions/ directory
        File sessionsDir = new File("/run/systemd/sessions");
        if (sessionsDir.exists() && sessionsDir.isDirectory()) {
            File[] sessionFiles = sessionsDir.listFiles(file -> Constants.DIGITS.matcher(file.getName()).matches());

            if (sessionFiles != null) {
                for (File sessionFile : sessionFiles) {
                    try {
                        Map<String, String> sessionMap = FileUtil.getKeyValueMapFromFile(sessionFile.getPath(), "=");

                        String user = sessionMap.get("USER");
                        if (user != null && !user.isEmpty()) {
                            String tty = sessionMap.getOrDefault("TTY", sessionFile.getName());
                            String remoteHost = sessionMap.getOrDefault("REMOTE_HOST", "");

                            // Try to get login time from REALTIME field or file modification time
                            long loginTime = 0L;
                            String realtime = sessionMap.get("REALTIME");
                            if (realtime != null) {
                                loginTime = ParseUtil.parseLongOrDefault(realtime, 0L) / 1000L; // Convert µs to ms
                            }
                            if (loginTime == 0L) {
                                loginTime = sessionFile.lastModified(); // Fallback to file modification time
                            }

                            if (isSessionValid(user, tty, loginTime)) {
                                sessionList.add(new OSSession(user, tty, loginTime, remoteHost));
                            }
                        }
                    } catch (Exception e) {
                        // Skip invalid session files
                    }
                }
            }
        }

        return sessionList;
    }
}
