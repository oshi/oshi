/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.registry;

import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static oshi.ffm.windows.WindowsForeignFunctions.readWideString;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.windows.Netapi32FFM;
import oshi.software.os.OSSession;

/**
 * Utility to read network session data via FFM.
 */
@ThreadSafe
public final class NetSessionDataFFM {

    // SESSION_INFO_10 on 64-bit: LPWSTR cname(8) + LPWSTR username(8) + DWORD time(4) + DWORD idle_time(4) = 24
    private static final long SESSION_INFO_10_SIZE = 24;
    private static final long OFFSET_CNAME = 0;
    private static final long OFFSET_USERNAME = 8;
    private static final long OFFSET_TIME = 16;

    private NetSessionDataFFM() {
    }

    public static List<OSSession> queryUserSessions() {
        List<OSSession> sessions = new ArrayList<>();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment bufptr = arena.allocate(ADDRESS);
            MemorySegment entriesRead = arena.allocate(JAVA_INT);
            MemorySegment totalEntries = arena.allocate(JAVA_INT);

            int status = Netapi32FFM.NetSessionEnum(NULL, NULL, NULL, 10, bufptr, Netapi32FFM.MAX_PREFERRED_LENGTH,
                    entriesRead, totalEntries, NULL);
            if (status == 0) {
                MemorySegment buf = bufptr.get(ADDRESS, 0);
                try {
                    int count = entriesRead.get(JAVA_INT, 0);
                    if (count > 0) {
                        buf = buf.reinterpret(SESSION_INFO_10_SIZE * count);
                        for (int i = 0; i < count; i++) {
                            long base = i * SESSION_INFO_10_SIZE;
                            MemorySegment pCname = buf.get(ADDRESS, base + OFFSET_CNAME).reinterpret(512);
                            MemorySegment pUsername = buf.get(ADDRESS, base + OFFSET_USERNAME).reinterpret(512);
                            int time = buf.get(JAVA_INT, base + OFFSET_TIME);
                            String cname = readWideString(pCname);
                            String username = readWideString(pUsername);
                            long logonTime = System.currentTimeMillis() - (1000L * time);
                            sessions.add(new OSSession(username, "Network session", logonTime, cname));
                        }
                    }
                } finally {
                    Netapi32FFM.NetApiBufferFree(buf);
                }
            }
        } catch (Throwable t) {
            // Silently return what we have
        }
        return sessions;
    }
}
