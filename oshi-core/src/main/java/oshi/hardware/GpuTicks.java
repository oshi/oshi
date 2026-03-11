/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import oshi.annotation.concurrent.Immutable;

/**
 * An immutable atomic snapshot of GPU active time and wall-clock timestamp, both in 100-nanosecond units.
 *
 * <p>
 * Both values are captured at the same instant so callers can compute utilization by taking two snapshots and
 * calculating {@code (delta activeTicks) / (delta timestamp) * 100}.
 */
@Immutable
public interface GpuTicks {

    /**
     * Wall-clock time in 100-nanosecond units at which this snapshot was captured.
     *
     * @return timestamp in 100ns units
     */
    long getTimestamp();

    /**
     * Cumulative GPU active time in 100-nanosecond units. Monotonically increasing. Returns 0 if tick-level metrics are
     * not available on this platform.
     *
     * @return cumulative active ticks in 100ns units, or 0 if unavailable
     */
    long getActiveTicks();
}
