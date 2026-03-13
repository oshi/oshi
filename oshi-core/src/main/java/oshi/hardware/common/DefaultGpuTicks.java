/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.GpuTicks;

/**
 * Immutable snapshot implementation of {@link GpuTicks}.
 */
@Immutable
public final class DefaultGpuTicks implements GpuTicks {

    private final long timestamp;
    private final long activeTicks;

    /**
     * Creates a new snapshot.
     *
     * @param timestamp   monotonic time in 100ns units, from {@link System#nanoTime()}
     * @param activeTicks cumulative GPU active time in 100ns units, or -1 if unavailable
     */
    public DefaultGpuTicks(long timestamp, long activeTicks) {
        this.timestamp = timestamp;
        this.activeTicks = activeTicks;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public long getActiveTicks() {
        return activeTicks;
    }

    @Override
    public String toString() {
        return "DefaultGpuTicks{timestamp=" + timestamp + ", activeTicks=" + activeTicks + '}';
    }
}
