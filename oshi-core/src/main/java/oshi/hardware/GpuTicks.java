/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import oshi.annotation.concurrent.Immutable;

/**
 * An immutable snapshot of cumulative GPU active and idle tick counters in opaque, platform-native units.
 *
 * <p>
 * Because {@code activeTicks + idleTicks = total elapsed ticks}, callers can compute utilization from two snapshots
 * without needing a separate timestamp:
 *
 * <pre>{@code
 * long dActive = curr.getActiveTicks() - prev.getActiveTicks();
 * long dIdle = curr.getIdleTicks() - prev.getIdleTicks();
 * long dTotal = dActive + dIdle;
 * double util = dTotal > 0 ? dActive * 100.0 / dTotal : -1d;
 * }</pre>
 *
 * <p>
 * Both counters are {@code 0} when tick-level metrics are not available on this platform. Because {@code 0} is also the
 * natural starting value of a real counter, callers cannot distinguish "not available" from "counter just started" —
 * but the {@code dTotal > 0} guard in the utilization formula handles both cases correctly.
 */
@Immutable
public final class GpuTicks {

    private final long activeTicks;
    private final long idleTicks;

    /**
     * Creates a new snapshot.
     *
     * @param activeTicks cumulative GPU active ticks in platform-native units
     * @param idleTicks   cumulative GPU idle ticks in platform-native units
     */
    public GpuTicks(long activeTicks, long idleTicks) {
        this.activeTicks = activeTicks;
        this.idleTicks = idleTicks;
    }

    /**
     * @return cumulative GPU active ticks in platform-native units
     */
    public long getActiveTicks() {
        return activeTicks;
    }

    /**
     * @return cumulative GPU idle ticks in platform-native units
     */
    public long getIdleTicks() {
        return idleTicks;
    }

    @Override
    public String toString() {
        return "GpuTicks{activeTicks=" + activeTicks + ", idleTicks=" + idleTicks + '}';
    }
}
