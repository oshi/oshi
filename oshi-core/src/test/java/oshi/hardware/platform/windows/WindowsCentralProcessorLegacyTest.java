/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;

import java.util.stream.LongStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.hardware.CentralProcessor.TickType;

/**
 * Exercises the legacy {@code GetSystemTimes} system-CPU-tick path against the real system by forcing the
 * {@code useLegacySystemCounters()} gate. Only the gate is overridden, so the genuine native call runs; a rotted legacy
 * binding (e.g. a broken native {@code GetSystemTimes}) surfaces as all-zero ticks or a total wildly inconsistent with
 * the modern per-processor sum rather than as a mere coverage number.
 */
@EnabledOnOs(OS.WINDOWS)
class WindowsCentralProcessorLegacyTest {

    /** A real {@link WindowsCentralProcessorJNA} with only the legacy system-counter gate forced. */
    private static final class ForcedLegacy extends WindowsCentralProcessorJNA {
        private final boolean legacy;

        ForcedLegacy(boolean legacy) {
            this.legacy = legacy;
        }

        @Override
        protected boolean useLegacySystemCounters() {
            return legacy;
        }
    }

    /**
     * The legacy {@code GetSystemTimes} path and the modern per-processor perfmon sum both measure cumulative CPU time
     * since boot, so their totals must agree closely. A broken legacy binding shows up here as zero idle/user ticks or
     * a total far from the modern one.
     */
    @Test
    void legacySystemCpuLoadTicksAreSaneAndMatchModernTotal() {
        long[] legacy = new ForcedLegacy(true).querySystemCpuLoadTicks();
        long[] modern = new ForcedLegacy(false).querySystemCpuLoadTicks();

        assertThat("legacy ticks array length", legacy.length, is(TickType.values().length));
        for (long tick : legacy) {
            assertThat("legacy tick non-negative", tick, greaterThanOrEqualTo(0L));
        }
        // A successful GetSystemTimes yields non-zero idle and user time on any running system
        assertThat("legacy idle ticks", legacy[TickType.IDLE.getIndex()], greaterThan(0L));
        assertThat("legacy user ticks", legacy[TickType.USER.getIndex()], greaterThan(0L));

        long legacyTotal = LongStream.of(legacy).sum();
        long modernTotal = LongStream.of(modern).sum();
        assertThat("legacy total ticks", legacyTotal, greaterThan(0L));
        // Both measure cumulative CPU time since boot; a generous tolerance catches a rotted binding without flaking
        assertThat((double) legacyTotal, closeTo(modernTotal, modernTotal * 0.5));
    }
}
