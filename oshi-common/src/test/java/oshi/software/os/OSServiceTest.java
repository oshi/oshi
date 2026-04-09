/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import oshi.software.os.OSService.State;

class OSServiceTest {

    @Test
    void testGetters() {
        OSService svc = new OSService("httpd", 1234, State.RUNNING);
        assertThat(svc.getName(), is("httpd"));
        assertThat(svc.getProcessID(), is(1234));
        assertThat(svc.getState(), is(State.RUNNING));
    }

    @Test
    void testStoppedState() {
        OSService svc = new OSService("cron", 0, State.STOPPED);
        assertThat(svc.getState(), is(State.STOPPED));
        assertThat(svc.getProcessID(), is(0));
    }

    @Test
    void testStateEnum() {
        assertThat(State.valueOf("RUNNING"), is(State.RUNNING));
        assertThat(State.valueOf("STOPPED"), is(State.STOPPED));
        assertThat(State.valueOf("OTHER"), is(State.OTHER));
    }
}
