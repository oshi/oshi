/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * A session handle for sampling dynamic GPU metrics. Obtain an instance via {@link GraphicsCard#createStatsSession()}.
 *
 * <p>
 * Each instance may hold native resources (subscriptions, handles, etc.) that are released when {@link #close()} is
 * called. Callers should use try-with-resources or call {@link #close()} explicitly when done.
 *
 * <p>
 * All metric methods throw {@link IllegalStateException} if called after {@link #close()}. The session is safe for
 * concurrent use from multiple threads.
 */
@ThreadSafe
public interface GpuStats extends AutoCloseable {

    /**
     * Releases any native resources held by this session. Safe to call multiple times; subsequent calls after the first
     * are no-ops. Does not throw checked exceptions.
     */
    @Override
    void close();

    /**
     * Returns {@code true} if {@link #close()} has been called on this session.
     *
     * @return true if this session is closed
     */
    boolean isClosed();

    /**
     * Returns an atomic snapshot of cumulative GPU active time and a monotonic timestamp at which it was captured, both
     * in 100-nanosecond units. The timestamp is derived from {@link System#nanoTime()} and is suitable for computing
     * deltas between snapshots but is not a wall-clock time.
     *
     * @return an immutable {@link GpuTicks} snapshot; never null
     * @throws IllegalStateException if the session has been closed
     */
    GpuTicks getGpuTicks();

    /**
     * Returns the instantaneous GPU core utilization as a percentage.
     *
     * @return utilization in the range 0.0 to 100.0, or -1 if not available
     * @throws IllegalStateException if the session has been closed
     */
    double getGpuUtilization();

    /**
     * Returns the amount of dedicated VRAM currently in use.
     *
     * @return bytes of VRAM in use, or -1 if unavailable
     * @throws IllegalStateException if the session has been closed
     */
    long getVramUsed();

    /**
     * Returns the amount of shared system memory currently used by this GPU.
     *
     * @return bytes of shared memory in use, or -1 if unavailable
     * @throws IllegalStateException if the session has been closed
     */
    long getSharedMemoryUsed();

    /**
     * Returns the GPU temperature.
     *
     * @return temperature in degrees Celsius, or -1 if unavailable
     * @throws IllegalStateException if the session has been closed
     */
    double getTemperature();

    /**
     * Returns the GPU power consumption.
     *
     * @return power draw in watts, or -1 if unavailable
     * @throws IllegalStateException if the session has been closed
     */
    double getPowerDraw();

    /**
     * Returns the current GPU core clock speed.
     *
     * @return core clock in MHz, or -1 if unavailable
     * @throws IllegalStateException if the session has been closed
     */
    long getCoreClockMhz();

    /**
     * Returns the current GPU memory clock speed.
     *
     * @return memory clock in MHz, or -1 if unavailable
     * @throws IllegalStateException if the session has been closed
     */
    long getMemoryClockMhz();

    /**
     * Returns the GPU fan speed as a percentage of maximum.
     *
     * @return fan speed in the range 0.0 to 100.0, or -1 if unavailable
     * @throws IllegalStateException if the session has been closed
     */
    double getFanSpeedPercent();
}
