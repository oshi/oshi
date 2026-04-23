/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.mac;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import oshi.software.os.OSProcess.State;

class ThreadInfoTest {

    @Test
    void testThreadStatsStateMapping() {
        assertThat(new ThreadInfo.ThreadStats(0, 50.0, 'S', 100L, 200L, 31).getState(), is(State.SLEEPING));
        assertThat(new ThreadInfo.ThreadStats(0, 50.0, 'I', 100L, 200L, 31).getState(), is(State.SLEEPING));
        assertThat(new ThreadInfo.ThreadStats(0, 50.0, 'R', 100L, 200L, 31).getState(), is(State.RUNNING));
        assertThat(new ThreadInfo.ThreadStats(0, 50.0, 'U', 100L, 200L, 31).getState(), is(State.WAITING));
        assertThat(new ThreadInfo.ThreadStats(0, 50.0, 'Z', 100L, 200L, 31).getState(), is(State.ZOMBIE));
        assertThat(new ThreadInfo.ThreadStats(0, 50.0, 'T', 100L, 200L, 31).getState(), is(State.STOPPED));
        assertThat(new ThreadInfo.ThreadStats(0, 50.0, 'X', 100L, 200L, 31).getState(), is(State.OTHER));
    }

    @Test
    void testThreadStatsGetters() {
        // upTime = (long) ((uTime + sTime) / (cpu / 100d + 0.0005))
        // = (long) ((2000 + 1000) / (25.0 / 100.0 + 0.0005))
        // = (long) (3000 / 0.2505) = 11976
        ThreadInfo.ThreadStats ts = new ThreadInfo.ThreadStats(5, 25.0, 'R', 1000L, 2000L, 20);
        assertThat(ts.getThreadId(), is(5));
        assertThat(ts.getSystemTime(), is(1000L));
        assertThat(ts.getUserTime(), is(2000L));
        assertThat(ts.getUpTime(), is(11976L));
        assertThat(ts.getPriority(), is(20));
    }
}
