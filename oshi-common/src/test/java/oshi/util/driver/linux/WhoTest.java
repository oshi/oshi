/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.software.os.OSSession;
import oshi.util.Constants;

class WhoTest {

    @Test
    void testMatchLinuxStandardFormat() {
        List<OSSession> sessions = new ArrayList<>();
        boolean matched = Who.matchLinux(sessions, "oshi pts/0        2020-05-14 21:23 (192.168.1.23)");
        assertThat(matched, is(true));
        assertThat(sessions.size(), is(1));
        assertThat(sessions.get(0).getUserName(), is("oshi"));
        assertThat(sessions.get(0).getTerminalDevice(), is("pts/0"));
        assertThat(sessions.get(0).getLoginTime(), is(greaterThan(0L)));
        assertThat(sessions.get(0).getHost(), is("192.168.1.23"));
    }

    @Test
    void testMatchLinuxNoHost() {
        List<OSSession> sessions = new ArrayList<>();
        boolean matched = Who.matchLinux(sessions, "root     tty1         2023-01-15 08:30");
        assertThat(matched, is(true));
        assertThat(sessions.size(), is(1));
        assertThat(sessions.get(0).getUserName(), is("root"));
        assertThat(sessions.get(0).getTerminalDevice(), is("tty1"));
        assertThat(sessions.get(0).getHost(), is(Constants.UNKNOWN));
    }

    @Test
    void testMatchLinuxNoMatch() {
        List<OSSession> sessions = new ArrayList<>();
        boolean matched = Who.matchLinux(sessions, "not a valid who line");
        assertThat(matched, is(false));
        assertThat(sessions.size(), is(0));
    }

    @Nested
    @EnabledOnOs(OS.LINUX)
    class LiveTests {
        @Test
        void testQueryWho() {
            List<OSSession> sessions = Who.queryWho();
            assertThat(sessions, is(notNullValue()));
            for (OSSession session : sessions) {
                assertThat(session.getUserName(), is(notNullValue()));
                assertThat(session.getTerminalDevice(), is(notNullValue()));
            }
        }
    }
}
