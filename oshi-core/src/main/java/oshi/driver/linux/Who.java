/*
 * Copyright 2020-2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.linux;

import static oshi.jna.platform.unix.CLibrary.LOGIN_PROCESS;
import static oshi.jna.platform.unix.CLibrary.USER_PROCESS;
import static oshi.util.Util.isSessionValid;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static com.sun.jna.Pointer.nativeValue;

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
import oshi.jna.ByRef.CloseableLongByReference;
import oshi.jna.ByRef.CloseablePointerByReference;

/**
 * Utility to query logged in users.
 */
@ThreadSafe
public final class Who {

    private static final LinuxLibc LIBC = LinuxLibc.INSTANCE;

    private static boolean useSystemd = GlobalConfig.get(GlobalConfig.OSHI_OS_LINUX_ALLOWSYSTEMD, true);

    private Who() {
    }

    /**
     * Query {@code getutxent} to get logged in users.
     *
     * @return A list of logged in user sessions
     */
    public static synchronized List<OSSession> queryUtxent() {
        // Try systemd first if available
        if (useSystemd) {
            try {
                List<OSSession> systemdSessions = querySystemdNative();
                if (!systemdSessions.isEmpty()) {
                    return systemdSessions;
                }
            } catch (Throwable t) {
                // systemd failed (probably UnsatisfiedLinkError), disable it for future calls
                useSystemd = false;
            }
        }

        List<OSSession> whoList = new ArrayList<>();
        LinuxUtmpx ut;
        // Rewind
        LIBC.setutxent();
        try {
            // Iterate
            while (nonNull(ut = LIBC.getutxent())) {
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

        try (CloseablePointerByReference sessionsPtr = new CloseablePointerByReference()) {
            int count = Systemd.INSTANCE.sd_get_sessions(sessionsPtr);

            if (count > 0) {
                Pointer sessions = sessionsPtr.getValue();
                if (nativeValue(sessions) != 0) {
                    try {
                        String[] sessionIds = sessions.getStringArray(0, count);

                        for (String sessionId : sessionIds) {
                            if (isNull(sessionId)) {
                                continue;
                            }
                            try {
                                // Get username
                                Pointer usernamePointer = null;
                                try (CloseablePointerByReference usernamePtr = new CloseablePointerByReference()) {
                                    if (Systemd.INSTANCE.sd_session_get_username(sessionId, usernamePtr) != 0
                                            || nativeValue(usernamePtr.getValue()) == 0) {
                                        continue; // Skip this session
                                    }
                                    usernamePointer = usernamePtr.getValue();
                                }

                                String user;
                                try {
                                    user = usernamePointer.getString(0);
                                } finally {
                                    Native.free(nativeValue(usernamePointer));
                                }

                                // Get start time
                                long loginTime;
                                try (CloseableLongByReference startTimePtr = new CloseableLongByReference()) {
                                    if (Systemd.INSTANCE.sd_session_get_start_time(sessionId, startTimePtr) != 0) {
                                        continue; // Skip this session
                                    }
                                    loginTime = startTimePtr.getValue() / 1000L; // Convert μs to ms
                                }

                                // Get TTY (optional)
                                String tty = sessionId; // Default to session ID
                                Pointer ttyPointer = null;
                                try (CloseablePointerByReference ttyPtr = new CloseablePointerByReference()) {
                                    if (Systemd.INSTANCE.sd_session_get_tty(sessionId, ttyPtr) == 0
                                            && nativeValue(ttyPtr.getValue()) != 0) {
                                        ttyPointer = ttyPtr.getValue();
                                    }
                                }
                                if (nativeValue(ttyPointer) != 0) {
                                    try {
                                        tty = ttyPointer.getString(0);
                                    } finally {
                                        Native.free(nativeValue(ttyPointer));
                                    }
                                }

                                // Get remote host (optional)
                                String remoteHost = "";
                                Pointer remoteHostPointer = null;
                                try (CloseablePointerByReference remoteHostPtr = new CloseablePointerByReference()) {
                                    if (Systemd.INSTANCE.sd_session_get_remote_host(sessionId, remoteHostPtr) == 0
                                            && nativeValue(remoteHostPtr.getValue()) != 0) {
                                        remoteHostPointer = remoteHostPtr.getValue();
                                    }
                                }
                                if (nativeValue(remoteHostPointer) != 0) {
                                    try {
                                        remoteHost = remoteHostPointer.getString(0);
                                    } finally {
                                        Native.free(nativeValue(remoteHostPointer));
                                    }
                                }

                                if (isSessionValid(user, tty, loginTime)) {
                                    sessionList.add(new OSSession(user, tty, loginTime, remoteHost));
                                }
                            } catch (Exception e) {
                                // Skip invalid session
                            }
                        }
                    } finally {
                        // Free all strings in the array first
                        Pointer[] ptrs = sessions.getPointerArray(0, count);
                        for (Pointer stringPtr : ptrs) {
                            if (nativeValue(stringPtr) != 0) {
                                Native.free(nativeValue(stringPtr));
                            }
                        }
                        // Then free the sessions array itself
                        Native.free(nativeValue(sessions));
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

            if (nonNull(sessionFiles)) {
                for (File sessionFile : sessionFiles) {
                    try {
                        Map<String, String> sessionMap = FileUtil.getKeyValueMapFromFile(sessionFile.getPath(), "=");

                        String user = sessionMap.get("USER");
                        if (nonNull(user) && !user.isEmpty()) {
                            String tty = sessionMap.getOrDefault("TTY", sessionFile.getName());
                            String remoteHost = sessionMap.getOrDefault("REMOTE_HOST", "");

                            // Try to get login time from REALTIME field or file modification time
                            long loginTime = 0L;
                            String realtime = sessionMap.get("REALTIME");
                            if (nonNull(realtime)) {
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
