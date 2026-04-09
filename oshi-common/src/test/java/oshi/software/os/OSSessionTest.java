/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

class OSSessionTest {

    @Test
    void testGetters() {
        OSSession session = new OSSession("alice", "pts/0", 1_000_000_000L, "192.168.1.1");
        assertThat(session.getUserName(), is("alice"));
        assertThat(session.getTerminalDevice(), is("pts/0"));
        assertThat(session.getLoginTime(), is(1_000_000_000L));
        assertThat(session.getHost(), is("192.168.1.1"));
    }

    @Test
    void testToStringWithHost() {
        OSSession session = new OSSession("bob", "tty1", 1_000_000_000L, "10.0.0.1");
        assertThat(session.toString(), containsString("bob"));
        assertThat(session.toString(), containsString("tty1"));
        assertThat(session.toString(), containsString("10.0.0.1"));
    }

    @Test
    void testToStringNoLoginTime() {
        OSSession session = new OSSession("charlie", "console", 0L, "");
        assertThat(session.toString(), containsString("No login"));
    }
}
