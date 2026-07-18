/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.unix.solaris;

import static oshi.ffm.platform.unix.solaris.SolarisLibcFunctions.LOGIN_PROCESS;
import static oshi.ffm.platform.unix.solaris.SolarisLibcFunctions.USER_PROCESS;
import static oshi.ffm.platform.unix.solaris.SolarisLibcFunctions.UTMPX_LAYOUT;
import static oshi.ffm.platform.unix.solaris.SolarisLibcFunctions.endutxent;
import static oshi.ffm.platform.unix.solaris.SolarisLibcFunctions.getutxent;
import static oshi.ffm.platform.unix.solaris.SolarisLibcFunctions.setutxent;
import static oshi.ffm.platform.unix.solaris.SolarisLibcFunctions.utmpxHost;
import static oshi.ffm.platform.unix.solaris.SolarisLibcFunctions.utmpxLine;
import static oshi.ffm.platform.unix.solaris.SolarisLibcFunctions.utmpxLoginTime;
import static oshi.ffm.platform.unix.solaris.SolarisLibcFunctions.utmpxType;
import static oshi.ffm.platform.unix.solaris.SolarisLibcFunctions.utmpxUser;
import static oshi.util.Util.isSessionValid;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.os.OSSession;

/**
 * FFM-based utility to query logged in users on Solaris/illumos via {@code getutxent}.
 */
@ThreadSafe
public final class WhoFFM {

    private WhoFFM() {
    }

    /**
     * Walks the utmpx database and returns the active user sessions.
     *
     * @return a list of logged in user sessions
     */
    public static synchronized List<OSSession> queryUtxent() {
        List<OSSession> whoList = new ArrayList<>();
        try (Arena arena = Arena.ofConfined()) {
            setutxent();
            try {
                MemorySegment utPtr;
                while ((utPtr = getutxent()) != null) {
                    MemorySegment ut = utPtr.reinterpret(UTMPX_LAYOUT.byteSize(), arena, null);
                    short type = utmpxType(ut);
                    if (type == USER_PROCESS || type == LOGIN_PROCESS) {
                        String user = utmpxUser(ut);
                        String device = utmpxLine(ut);
                        String host = utmpxHost(ut);
                        long loginTime = utmpxLoginTime(ut);
                        // Sanity check. If errors, default to who command line
                        if (!isSessionValid(user, device, loginTime)) {
                            return oshi.util.driver.unix.Who.queryWho();
                        }
                        whoList.add(new OSSession(user, device, loginTime, host));
                    }
                }
            } finally {
                endutxent();
            }
        } catch (Throwable _) {
            return oshi.util.driver.unix.Who.queryWho();
        }
        return whoList;
    }
}
