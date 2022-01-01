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
package oshi.driver.windows.registry;

import java.util.ArrayList;
import java.util.List;

import com.sun.jna.Pointer; // NOSONAR squid:S1191
import com.sun.jna.platform.win32.Netapi32;
import com.sun.jna.platform.win32.Netapi32.SESSION_INFO_10;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.os.OSSession;

/**
 * Utility to read process data from HKEY_PERFORMANCE_DATA information with
 * backup from Performance Counters or WMI
 */
@ThreadSafe
public final class NetSessionData {

    private static final Netapi32 NET = Netapi32.INSTANCE;

    private NetSessionData() {
    }

    public static List<OSSession> queryUserSessions() {
        List<OSSession> sessions = new ArrayList<>();
        PointerByReference bufptr = new PointerByReference();
        IntByReference entriesread = new IntByReference();
        IntByReference totalentries = new IntByReference();
        if (0 == NET.NetSessionEnum(null, null, null, 10, bufptr, Netapi32.MAX_PREFERRED_LENGTH, entriesread,
                totalentries, null)) {
            Pointer buf = bufptr.getValue();
            SESSION_INFO_10 si10 = new SESSION_INFO_10(buf);
            if (entriesread.getValue() > 0) {
                SESSION_INFO_10[] sessionInfo = (SESSION_INFO_10[]) si10.toArray(entriesread.getValue());
                for (SESSION_INFO_10 si : sessionInfo) {
                    // time field is connected seconds
                    long logonTime = System.currentTimeMillis() - (1000L * si.sesi10_time);
                    sessions.add(new OSSession(si.sesi10_username, "Network session", logonTime, si.sesi10_cname));
                }
            }
            NET.NetApiBufferFree(buf);
        }
        return sessions;
    }
}
