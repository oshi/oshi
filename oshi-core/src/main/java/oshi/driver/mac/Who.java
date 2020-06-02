/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.driver.mac;

import static oshi.jna.platform.unix.CLibrary.LOGIN_PROCESS;
import static oshi.jna.platform.unix.CLibrary.USER_PROCESS;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.platform.mac.SystemB;
import oshi.jna.platform.mac.SystemB.MacUtmpx;
import oshi.software.os.OSSession;

/**
 * Utility to query logged in users.
 */
@ThreadSafe
public final class Who {

    private static final SystemB SYS = SystemB.INSTANCE;

    private Who() {
    }

    /**
     * Query {@code getutxent} to get logged in users.
     *
     * @return A list of logged in user sessions
     */
    public static synchronized List<OSSession> queryUtxent() {
        List<OSSession> whoList = new ArrayList<>();
        MacUtmpx ut;
        // Rewind
        SYS.setutxent();
        try { // Iterate
            while ((ut = SYS.getutxent()) != null) {
                if (ut.ut_type == USER_PROCESS || ut.ut_type == LOGIN_PROCESS) {
                    String user = new String(ut.ut_user, StandardCharsets.US_ASCII).trim();
                    String device = new String(ut.ut_line, StandardCharsets.US_ASCII).trim();
                    String host = new String(ut.ut_host, StandardCharsets.US_ASCII).trim();
                    long loginTime = ut.ut_tv.tv_sec.longValue() * 1000L + ut.ut_tv.tv_usec / 1000L;
                    // Sanity check. If errors, default to who command line
                    if (user.isEmpty() || device.isEmpty() || loginTime < 0 || loginTime > System.currentTimeMillis()) {
                        return oshi.driver.unix.Who.queryWho();
                    }
                    whoList.add(new OSSession(user, device, loginTime, host));
                }
            }
        } finally {
            // Close
            SYS.endutxent();
        }
        return whoList;
    }
}
