/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import oshi.driver.common.windows.registry.ThreadPerfCounterBlock;

/**
 * Tests the parsing seams of {@link WindowsOSProcess}.
 */
class WindowsOSProcessTest {

    private static ThreadPerfCounterBlock thread(int tid, int pid, long contextSwitches) {
        return new ThreadPerfCounterBlock("java/" + tid, tid, pid, 0L, 0L, 0L, 8, 5, 0, 0L, contextSwitches);
    }

    @Test
    void testContextSwitchesSumTheProcessThreads() {
        // A thread map queried for all processes holds other processes' threads too
        Map<Integer, ThreadPerfCounterBlock> threads = Map.of(1, thread(1, 100, 66L), 2, thread(2, 100, 11L), 3,
                thread(3, 100, 10L), 4, thread(4, 200, 1000L));
        assertThat(WindowsOSProcess.sumContextSwitches(threads, 100), is(87L));
        assertThat(WindowsOSProcess.sumContextSwitches(threads, 300), is(0L));
        assertThat(WindowsOSProcess.sumContextSwitches(Collections.emptyMap(), 100), is(0L));
    }
}
