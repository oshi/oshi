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
package oshi.software.os;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import oshi.annotation.concurrent.Immutable;

/**
 * This class encapsulates information about users who are currently logged in
 * to an operating system.
 */
@Immutable
public class OSSession {
    private static final DateTimeFormatter LOGIN_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final String userName;
    private final String terminalDevice;
    private final long loginTime;
    private final String host;

    public OSSession(String userName, String terminalDevice, long loginTime, String host) {
        this.userName = userName;
        this.terminalDevice = terminalDevice;
        this.loginTime = loginTime;
        this.host = host;
    }

    /**
     * Gets the login name of the user
     *
     * @return the userName
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Gets the terminal device (such as tty, pts, etc.) the user used to log in
     *
     * @return the terminalDevice
     */
    public String getTerminalDevice() {
        return terminalDevice;
    }

    /**
     * Gets the time the user logged in
     *
     * @return the loginTime, in milliseconds since the 1970 epoch
     */
    public long getLoginTime() {
        return loginTime;
    }

    /**
     * Gets the remote host from which the user logged in
     *
     * @return the host as either an IPv4 or IPv6 representation. If the host is
     *         unspecified, may also be an empty string, depending on the platform.
     */
    public String getHost() {
        return host;
    }

    @Override
    public String toString() {
        String loginStr = loginTime == 0 ? "No login"
                : LocalDateTime.ofInstant(Instant.ofEpochMilli(loginTime), ZoneId.systemDefault()).format(LOGIN_FORMAT);
        String hostStr = "";
        if (!host.isEmpty() && !host.equals("::") && !host.equals("0.0.0.0")) {
            hostStr = ", (" + host + ")";
        }
        return String.format("%s, %s, %s%s", userName, terminalDevice, loginStr, hostStr);
    }
}