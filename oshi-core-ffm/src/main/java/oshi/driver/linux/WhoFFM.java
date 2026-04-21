/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.linux;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static java.util.Objects.nonNull;
import static oshi.ffm.linux.LinuxLibcFunctions.LOGIN_PROCESS;
import static oshi.ffm.linux.LinuxLibcFunctions.USER_PROCESS;
import static oshi.util.Util.isSessionValid;

import java.io.File;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.linux.LinuxLibcFunctions;
import oshi.ffm.linux.SystemdFunctions;
import oshi.software.os.OSSession;
import oshi.util.Constants;
import oshi.util.FileUtil;
import oshi.util.GlobalConfig;
import oshi.util.ParseUtil;

/**
 * FFM-based utility to query logged in users on Linux.
 */
@ThreadSafe
public final class WhoFFM {

    private static boolean useSystemd = SystemdFunctions.isAvailable()
            && GlobalConfig.get(GlobalConfig.OSHI_OS_LINUX_ALLOWSYSTEMD, true);

    private WhoFFM() {
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
                useSystemd = false;
            }
        }

        List<OSSession> whoList = new ArrayList<>();
        try (Arena arena = Arena.ofConfined()) {
            LinuxLibcFunctions.setutxent();
            try {
                MemorySegment utPtr;
                while ((utPtr = LinuxLibcFunctions.getutxent()) != null) {
                    MemorySegment ut = utPtr.reinterpret(LinuxLibcFunctions.UTMPX_LAYOUT.byteSize(), arena, null);
                    short type = LinuxLibcFunctions.utmpxType(ut);
                    if (type == USER_PROCESS || type == LOGIN_PROCESS) {
                        String user = LinuxLibcFunctions.utmpxUser(ut);
                        String device = LinuxLibcFunctions.utmpxLine(ut);
                        String host = ParseUtil.parseUtAddrV6toIP(LinuxLibcFunctions.utmpxAddrV6(ut));
                        long loginTime = LinuxLibcFunctions.utmpxLoginTime(ut);
                        if (!isSessionValid(user, device, loginTime)) {
                            return oshi.util.driver.linux.Who.queryWho();
                        }
                        whoList.add(new OSSession(user, device, loginTime, host));
                    }
                }
            } finally {
                LinuxLibcFunctions.endutxent();
            }
        } catch (Throwable e) {
            return oshi.util.driver.linux.Who.queryWho();
        }

        // If utmp returned no sessions, try systemd file fallback
        if (whoList.isEmpty()) {
            whoList = querySystemdFiles();
            if (whoList.isEmpty()) {
                return oshi.util.driver.linux.Who.queryWho();
            }
        }
        return whoList;
    }

    private static List<OSSession> querySystemdNative() throws Throwable {
        List<OSSession> sessionList = new ArrayList<>();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment sessionsPtr = arena.allocate(ADDRESS);
            int count = SystemdFunctions.sdGetSessions(sessionsPtr);
            if (count <= 0) {
                return sessionList;
            }

            MemorySegment sessions = sessionsPtr.get(ADDRESS, 0);
            if (sessions.equals(MemorySegment.NULL)) {
                return sessionList;
            }

            try {
                // Read array of string pointers
                MemorySegment ptrArray = sessions.reinterpret((long) count * ADDRESS.byteSize(), arena, null);
                for (int i = 0; i < count; i++) {
                    MemorySegment sessionIdPtr = ptrArray.getAtIndex(ADDRESS, i);
                    if (sessionIdPtr.equals(MemorySegment.NULL)) {
                        continue;
                    }
                    try {
                        String sessionId = sessionIdPtr.reinterpret(256, arena, null).getString(0);
                        OSSession session = queryOneSession(sessionId, arena);
                        if (session != null) {
                            sessionList.add(session);
                        }
                    } catch (Exception e) {
                        // Skip invalid session
                    }
                }
            } finally {
                // Free each string, then the array
                MemorySegment ptrArray = sessions.reinterpret((long) count * ADDRESS.byteSize(), arena, null);
                for (int i = 0; i < count; i++) {
                    MemorySegment ptr = ptrArray.getAtIndex(ADDRESS, i);
                    if (!ptr.equals(MemorySegment.NULL)) {
                        SystemdFunctions.free(ptr);
                    }
                }
                SystemdFunctions.free(sessions);
            }
        }
        return sessionList;
    }

    private static OSSession queryOneSession(String sessionId, Arena arena) throws Throwable {
        MemorySegment sessionSeg = arena.allocateFrom(sessionId);

        // Get username (required)
        MemorySegment usernamePtr = arena.allocate(ADDRESS);
        if (SystemdFunctions.sdSessionGetUsername(sessionSeg, usernamePtr) != 0) {
            return null;
        }
        MemorySegment usernameRaw = usernamePtr.get(ADDRESS, 0);
        if (usernameRaw.equals(MemorySegment.NULL)) {
            return null;
        }
        String user = SystemdFunctions.readAndFreeString(usernameRaw, arena);
        if (user == null) {
            return null;
        }

        // Get start time (required)
        MemorySegment usecSeg = arena.allocate(JAVA_LONG);
        if (SystemdFunctions.sdSessionGetStartTime(sessionSeg, usecSeg) != 0) {
            return null;
        }
        long loginTime = usecSeg.get(JAVA_LONG, 0) / 1000L; // μs to ms

        // Get TTY (optional, default to session ID)
        String tty = sessionId;
        MemorySegment ttyPtr = arena.allocate(ADDRESS);
        if (SystemdFunctions.sdSessionGetTty(sessionSeg, ttyPtr) == 0) {
            MemorySegment ttyRaw = ttyPtr.get(ADDRESS, 0);
            if (!ttyRaw.equals(MemorySegment.NULL)) {
                String t = SystemdFunctions.readAndFreeString(ttyRaw, arena);
                if (t != null) {
                    tty = t;
                }
            }
        }

        // Get remote host (optional)
        String remoteHost = "";
        MemorySegment hostPtr = arena.allocate(ADDRESS);
        if (SystemdFunctions.sdSessionGetRemoteHost(sessionSeg, hostPtr) == 0) {
            MemorySegment hostRaw = hostPtr.get(ADDRESS, 0);
            if (!hostRaw.equals(MemorySegment.NULL)) {
                String h = SystemdFunctions.readAndFreeString(hostRaw, arena);
                if (h != null) {
                    remoteHost = h;
                }
            }
        }

        if (!isSessionValid(user, tty, loginTime)) {
            return null;
        }
        return new OSSession(user, tty, loginTime, remoteHost);
    }

    private static List<OSSession> querySystemdFiles() {
        List<OSSession> sessionList = new ArrayList<>();
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
                            long loginTime = 0L;
                            String realtime = sessionMap.get("REALTIME");
                            if (nonNull(realtime)) {
                                loginTime = ParseUtil.parseLongOrDefault(realtime, 0L) / 1000L;
                            }
                            if (loginTime == 0L) {
                                loginTime = sessionFile.lastModified();
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
