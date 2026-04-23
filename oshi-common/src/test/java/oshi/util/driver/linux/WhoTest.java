/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.software.os.OSSession;

@EnabledOnOs(OS.LINUX)
class WhoTest {

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
