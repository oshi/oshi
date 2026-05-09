/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.mac;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import oshi.driver.common.mac.ThreadInfo.ThreadStats;
import oshi.software.os.OSProcess.State;

class ThreadInfoTest {

    @Test
    void testThreadStatsStateSleeping() {
        ThreadStats ts = new ThreadStats(0, 1.0, 'S', 100L, 200L, 31);
        assertThat(ts.getState(), is(State.SLEEPING));
        // Also test 'I' (idle/sleeping)
        ThreadStats ti = new ThreadStats(1, 0.5, 'I', 50L, 100L, 20);
        assertThat(ti.getState(), is(State.SLEEPING));
    }

    @Test
    void testThreadStatsStateRunning() {
        ThreadStats ts = new ThreadStats(0, 50.0, 'R', 1000L, 2000L, 31);
        assertThat(ts.getState(), is(State.RUNNING));
    }

    @Test
    void testThreadStatsStateWaiting() {
        ThreadStats ts = new ThreadStats(0, 0.1, 'U', 10L, 20L, 15);
        assertThat(ts.getState(), is(State.WAITING));
    }

    @Test
    void testThreadStatsStateZombie() {
        ThreadStats ts = new ThreadStats(0, 0.0, 'Z', 0L, 0L, 0);
        assertThat(ts.getState(), is(State.ZOMBIE));
    }

    @Test
    void testThreadStatsStateStopped() {
        ThreadStats ts = new ThreadStats(0, 0.0, 'T', 500L, 1000L, 10);
        assertThat(ts.getState(), is(State.STOPPED));
    }

    @Test
    void testThreadStatsStateOther() {
        ThreadStats ts = new ThreadStats(0, 0.0, 'X', 0L, 0L, 0);
        assertThat(ts.getState(), is(State.OTHER));
    }

    @Test
    void testThreadStatsAccessors() {
        ThreadStats ts = new ThreadStats(5, 25.0, 'R', 3000L, 7000L, 47);
        assertThat(ts.getThreadId(), is(5));
        assertThat(ts.getUserTime(), is(7000L));
        assertThat(ts.getSystemTime(), is(3000L));
        assertThat(ts.getPriority(), is(47));
        // upTime = (user+system) / (cpu/100 + 0.0005) = 10000 / 0.2505 ≈ 39920
        assertThat(ts.getUpTime(), is((long) ((7000L + 3000L) / (25.0 / 100d + 0.0005))));
    }
}
