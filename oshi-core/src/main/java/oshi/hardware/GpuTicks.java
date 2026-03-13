/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import oshi.annotation.concurrent.Immutable;

/**
 * An immutable atomic snapshot of GPU active time and a monotonic timestamp, both in 100-nanosecond units.
 *
 * <p>
 * Both values are captured at the same instant so callers can compute utilization by taking two snapshots and
 * calculating {@code (delta activeTicks) / (delta timestamp) * 100}. The first snapshot after a session is opened will
 * have {@code activeTicks = -1} (priming call); subsequent snapshots carry a valid cumulative value.
 */
@Immutable
public interface GpuTicks {

    /**
     * Monotonic timestamp in 100-nanosecond units at which this snapshot was captured, sourced from
     * {@link System#nanoTime()}. Suitable for computing elapsed time between two snapshots; not a wall-clock time.
     *
     * @return monotonic timestamp in 100ns units
     */
    long getTimestamp();

    /**
     * Cumulative GPU active time in 100-nanosecond units, accumulated from per-interval deltas. Returns {@code -1} on
     * the first call after a session is opened (priming call) or if tick-level metrics are not available on this
     * platform.
     *
     * @return cumulative active ticks in 100ns units, or -1 if unavailable or not yet primed
     */
    long getActiveTicks();
}
