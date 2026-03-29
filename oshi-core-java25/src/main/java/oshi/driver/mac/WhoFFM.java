/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.mac;

import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;
import static oshi.ffm.mac.MacSystem.LOGIN_PROCESS;
import static oshi.ffm.mac.MacSystem.USER_PROCESS;
import static oshi.ffm.mac.MacSystem.UTMPX;
import static oshi.ffm.mac.MacSystem.UTX_HOSTSIZE;
import static oshi.ffm.mac.MacSystem.UTX_LINESIZE;
import static oshi.ffm.mac.MacSystem.UTX_USERSIZE;
import static oshi.ffm.mac.MacSystem.UT_HOST;
import static oshi.ffm.mac.MacSystem.UT_LINE;
import static oshi.ffm.mac.MacSystem.UT_TV_SEC;
import static oshi.ffm.mac.MacSystem.UT_TV_USEC;
import static oshi.ffm.mac.MacSystem.UT_TYPE;
import static oshi.ffm.mac.MacSystem.UT_USER;
import static oshi.ffm.mac.MacSystemFunctions.endutxent;
import static oshi.ffm.mac.MacSystemFunctions.getutxent;
import static oshi.ffm.mac.MacSystemFunctions.setutxent;
import static oshi.util.Util.isSessionValid;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.os.OSSession;

/**
 * Utility to query logged-in users via {@code getutxent}, using FFM (no JNA dependency).
 */
@ThreadSafe
public final class WhoFFM {

    private WhoFFM() {
    }

    /**
     * Query {@code getutxent} to get logged-in users.
     *
     * @return A list of logged-in user sessions
     */
    public static synchronized List<OSSession> queryUtxent() {
        List<OSSession> whoList = new ArrayList<>();
        try (Arena arena = Arena.ofConfined()) {
            setutxent();
            try {
                MemorySegment utPtr;
                while ((utPtr = getutxent()) != null) {
                    MemorySegment ut = utPtr.reinterpret(UTMPX.byteSize(), arena, null);
                    short type = ut.get(JAVA_SHORT, UTMPX.byteOffset(UT_TYPE));
                    if (type == USER_PROCESS || type == LOGIN_PROCESS) {
                        String user = ut.asSlice(UTMPX.byteOffset(UT_USER), UTX_USERSIZE).getString(0);
                        String device = ut.asSlice(UTMPX.byteOffset(UT_LINE), UTX_LINESIZE).getString(0);
                        String host = ut.asSlice(UTMPX.byteOffset(UT_HOST), UTX_HOSTSIZE).getString(0);
                        long tvSec = ut.get(JAVA_LONG, UTMPX.byteOffset(UT_TV_SEC));
                        int tvUsec = ut.get(JAVA_INT, UTMPX.byteOffset(UT_TV_USEC));
                        long loginTime = tvSec * 1000L + tvUsec / 1000L;
                        if (!isSessionValid(user, device, loginTime)) {
                            return oshi.driver.unix.Who.queryWho();
                        }
                        whoList.add(new OSSession(user, device, loginTime, host));
                    }
                }
            } finally {
                endutxent();
            }
        } catch (Throwable e) {
            return oshi.driver.unix.Who.queryWho();
        }
        return whoList;
    }
}
