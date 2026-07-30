/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

import oshi.driver.common.windows.perfmon.PerfCounter;

/**
 * Tests the counter bookkeeping that needs no PDH query.
 * <p>
 * Adding a counter opens the query natively, so these cover only the paths before that happens: a handler that has
 * never opened one. Those paths are pure Java and so run on any OS.
 */
class PerfCounterQueryHandlerTest {

    private static PerfCounter counter(String name) {
        return new PerfCounter("Processor", "_Total", name);
    }

    /**
     * Removing a counter that was never added must report failure rather than throw. The query handle is only created
     * when the first counter is added, so it is still null here, and the cleanup that runs once the map is empty used
     * to dereference it.
     */
    @Test
    void testRemovingAnUnknownCounterFromAFreshHandler() {
        try (PerfCounterQueryHandler handler = new PerfCounterQueryHandler()) {
            assertThat("Nothing was removed, so the result is false", handler.removeCounterFromQuery(counter("A")),
                    is(false));
        }
    }

    /**
     * The same path reached twice. Each call leaves the map empty, so each reruns the cleanup branch.
     */
    @Test
    void testRemovingRepeatedlyIsSafe() {
        try (PerfCounterQueryHandler handler = new PerfCounterQueryHandler()) {
            handler.removeCounterFromQuery(counter("A"));
            assertThat(handler.removeCounterFromQuery(counter("A")), is(false));
            assertThat(handler.removeCounterFromQuery(counter("B")), is(false));
        }
    }

    /**
     * Removing all counters from a handler that never opened a query is the guarded equivalent, and was already safe.
     * Kept alongside so the two cleanup paths are covered together.
     */
    @Test
    void testRemoveAllCountersOnAFreshHandler() {
        try (PerfCounterQueryHandler handler = new PerfCounterQueryHandler()) {
            assertDoesNotThrow(handler::removeAllCounters);
        }
    }

    @Test
    void testCloseOnAFreshHandlerIsSafe() {
        assertDoesNotThrow(() -> new PerfCounterQueryHandler().close());
    }
}
