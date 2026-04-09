/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

class GpuTicksTest {

    @Test
    void testGetters() {
        GpuTicks ticks = new GpuTicks(100L, 200L);
        assertThat(ticks.getActiveTicks(), is(100L));
        assertThat(ticks.getIdleTicks(), is(200L));
    }

    @Test
    void testToString() {
        GpuTicks ticks = new GpuTicks(42L, 58L);
        assertThat(ticks.toString(), containsString("42"));
        assertThat(ticks.toString(), containsString("58"));
    }
}
