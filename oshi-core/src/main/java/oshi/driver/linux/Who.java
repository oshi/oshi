/*
 * Copyright 2020-2023 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.linux;

import static oshi.jna.platform.unix.CLibrary.LOGIN_PROCESS;
import static oshi.jna.platform.unix.CLibrary.USER_PROCESS;
import static oshi.util.Util.isSessionValid;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import com.sun.jna.Native;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.platform.linux.LinuxLibc;
import oshi.jna.platform.linux.LinuxLibc.LinuxUtmpx;
import oshi.software.os.OSSession;
import oshi.util.ParseUtil;

/**
 * Utility to query logged in users.
 */
@ThreadSafe
public final class Who {

    private static final LinuxLibc LIBC = LinuxLibc.INSTANCE;

    private Who() {
    }

    /**
     * Query {@code getutxent} to get logged in users.
     *
     * @return A list of logged in user sessions
     */
    public static synchronized List<OSSession> queryUtxent() {
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
        return whoList;
    }
}
