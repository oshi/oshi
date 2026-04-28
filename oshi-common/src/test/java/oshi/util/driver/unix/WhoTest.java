/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.unix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.software.os.OSSession;

class WhoTest {

    @Test
    void testMatchUnixStandardFormat() {
        List<OSSession> sessions = new ArrayList<>();
        boolean matched = Who.matchUnix(sessions, "oshi ttys000 May  4 23:50 (192.168.1.23)");
        assertThat(matched, is(true));
        assertThat(sessions.size(), is(1));
        assertThat(sessions.get(0).getUserName(), is("oshi"));
        assertThat(sessions.get(0).getTerminalDevice(), is("ttys000"));
        assertThat(sessions.get(0).getLoginTime(), is(greaterThan(0L)));
    }

    @Test
    void testMatchUnixNoHost() {
        List<OSSession> sessions = new ArrayList<>();
        boolean matched = Who.matchUnix(sessions, "root     console  Jan 15 08:30");
        assertThat(matched, is(true));
        assertThat(sessions.size(), is(1));
        assertThat(sessions.get(0).getUserName(), is("root"));
        assertThat(sessions.get(0).getTerminalDevice(), is("console"));
        assertThat(sessions.get(0).getHost(), is(""));
    }

    @Test
    void testMatchUnixSingleDigitDay() {
        List<OSSession> sessions = new ArrayList<>();
        boolean matched = Who.matchUnix(sessions, "user1 pts/0 Dec  3 14:22 (10.0.0.1)");
        assertThat(matched, is(true));
        assertThat(sessions.size(), is(1));
        assertThat(sessions.get(0).getUserName(), is("user1"));
        assertThat(sessions.get(0).getTerminalDevice(), is("pts/0"));
        assertThat(sessions.get(0).getHost(), is("10.0.0.1"));
    }

    @Test
    void testMatchUnixNoMatch() {
        List<OSSession> sessions = new ArrayList<>();
        boolean matched = Who.matchUnix(sessions, "not a valid who line");
        assertThat(matched, is(false));
        assertThat(sessions.size(), is(0));
    }

    @Nested
    @DisabledOnOs(OS.WINDOWS)
    class LiveTests {
        @Test
        void testQueryWho() {
            for (OSSession session : Who.queryWho()) {
                assertThat("Session login time should be greater than 0", session.getLoginTime(), is(greaterThan(0L)));
                assertThat("Session login time should be less than current time", session.getLoginTime(),
                        is(lessThan(System.currentTimeMillis())));
                assertThat("User should be non-empty", session.getUserName(), is(not(emptyOrNullString())));
                assertThat("Devices should be non-empty", session.getTerminalDevice(), is(not(emptyOrNullString())));
            }
        }
    }
}
