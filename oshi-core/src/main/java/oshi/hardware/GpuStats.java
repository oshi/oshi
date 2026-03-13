/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * A session handle for sampling dynamic GPU metrics. Obtain an instance via {@link GraphicsCard#createStatsSession()}.
 *
 * <h2>Session lifecycle</h2>
 * <p>
 * Each session may hold native resources (IOReport subscriptions, device handles, etc.) that are released when
 * {@link #close()} is called. Always close the session when done, preferably via try-with-resources:
 *
 * <pre>{@code
 * try (GpuStats stats = card.createStatsSession()) {
 *     double temp = stats.getTemperature();
 *     double power = stats.getPowerDraw();
 * }
 * }</pre>
 *
 * <h2>One-shot vs. polling</h2>
 * <p>
 * Instantaneous metrics (temperature, VRAM used, clock speeds, fan speed) can be read from a freshly opened session
 * with a single call. However, <em>delta-based</em> metrics — {@link #getGpuUtilization()}, {@link #getPowerDraw()},
 * and the tick-derived utilization from {@link #getGpuTicks()} — require at least two samples separated by a meaningful
 * time interval (typically 500 ms or more) to produce a valid result. On the first call after a session is opened these
 * methods return {@code -1} while they record the initial baseline; subsequent calls return the value computed over the
 * elapsed interval.
 *
 * <h2>Recommended polling pattern</h2>
 * <p>
 * For repeated polling, hold a single session open for the entire polling window rather than opening and closing one
 * per iteration. This preserves the internal baseline state across iterations and avoids the overhead of re-creating
 * native subscriptions on every sample:
 *
 * <pre>{@code
 * try (GpuStats stats = card.createStatsSession()) {
 *
 *     // Prime delta-based metrics so the first real iteration returns a value.
 *     GpuTicks prev = stats.getGpuTicks();
 *     stats.getPowerDraw(); // establishes power baseline; returns -1
 *     stats.getGpuUtilization(); // establishes utilization baseline; returns -1
 *
 *     for (int i = 0; i < ITERATIONS; i++) {
 *         Thread.sleep(1_000L);
 *
 *         // Tick-derived utilization: delta active / delta elapsed * 100
 *         GpuTicks curr = stats.getGpuTicks();
 *         long dtTicks = curr.getTimestamp() - prev.getTimestamp();
 *         long dActive = curr.getActiveTicks() - prev.getActiveTicks();
 *         double tickUtil = (dtTicks > 0 && dActive >= 0) ? dActive * 100.0 / dtTicks : -1d;
 *         prev = curr;
 *
 *         // API-reported utilization (delta computed internally by the session)
 *         double apiUtil = stats.getGpuUtilization();
 *
 *         // Instantaneous metrics
 *         double temp = stats.getTemperature();
 *         double power = stats.getPowerDraw();
 *         long clock = stats.getCoreClockMhz();
 *     }
 * }
 * }</pre>
 *
 * <h2>Sentinel values</h2>
 * <p>
 * All metric methods return {@code -1} (or {@code -1L} for {@code long} results) when the metric is unavailable on the
 * current platform, when the session has not yet accumulated enough samples to compute a delta, or when the underlying
 * native query fails. Callers should treat any negative return value as "not available" and display it accordingly
 * (e.g. "n/a") rather than as a meaningful measurement.
 *
 * <h2>Thread safety</h2>
 * <p>
 * All methods are safe for concurrent use from multiple threads.
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
     * Returns {@code true} if {@link #close()} has been called on this session. Does not throw.
     *
     * @return true if this session is closed
     */
    boolean isClosed();

    /**
     * Returns an atomic snapshot of cumulative GPU active time and a monotonic timestamp at which it was captured, both
     * in 100-nanosecond units. The timestamp is derived from {@link System#nanoTime()} and is suitable for computing
     * elapsed time between two snapshots; it is not a wall-clock time.
     *
     * <p>
     * This method returns a raw cumulative counter, not a percentage. To derive utilization, take two snapshots
     * separated by a known interval and compute:
     *
     * <pre>{@code
     * long dtTicks = curr.getTimestamp() - prev.getTimestamp();
     * long dActive = curr.getActiveTicks() - prev.getActiveTicks();
     * double utilPct = (dtTicks > 0 && dActive >= 0) ? dActive * 100.0 / dtTicks : -1d;
     * }</pre>
     *
     * <p>
     * {@code getActiveTicks()} returns {@code 0} on platforms where tick-level GPU metrics are not available (e.g.
     * non-Apple-Silicon macOS, Windows without PDH counters). In that case the delta will always be zero; use
     * {@link #getGpuUtilization()} as an alternative source of utilization data.
     *
     * @return an immutable {@link GpuTicks} snapshot; never null
     * @throws IllegalStateException if the session has been closed; obtain a new session via
     *                               {@link GraphicsCard#createStatsSession()}
     */
    GpuTicks getGpuTicks();

    /**
     * Returns the instantaneous GPU core utilization as a percentage, computed internally as a delta between the
     * current sample and the previous one recorded by this session.
     *
     * <p>
     * Because this metric is delta-based, the <em>first call</em> after a session is opened records the initial
     * baseline sample and returns {@code -1}. Subsequent calls return the utilization computed over the interval since
     * the previous call. To ensure the first polling iteration returns a valid value, call this method once during a
     * seeding/priming step before the polling loop begins:
     *
     * <pre>{@code
     * stats.getGpuUtilization(); // prime — discards -1 return value
     * Thread.sleep(intervalMs);
     * double util = stats.getGpuUtilization(); // now returns a valid percentage
     * }</pre>
     *
     * @return utilization in the range 0.0 to 100.0, or -1 if not available or not yet primed
     * @throws IllegalStateException if the session has been closed; obtain a new session via
     *                               {@link GraphicsCard#createStatsSession()}
     */
    double getGpuUtilization();

    /**
     * Returns the amount of dedicated VRAM currently in use.
     *
     * @return bytes of VRAM in use, or -1 if unavailable
     * @throws IllegalStateException if the session has been closed; obtain a new session via
     *                               {@link GraphicsCard#createStatsSession()}
     */
    long getVramUsed();

    /**
     * Returns the amount of shared system memory currently used by this GPU.
     *
     * @return bytes of shared memory in use, or -1 if unavailable
     * @throws IllegalStateException if the session has been closed; obtain a new session via
     *                               {@link GraphicsCard#createStatsSession()}
     */
    long getSharedMemoryUsed();

    /**
     * Returns the GPU temperature.
     *
     * @return temperature in degrees Celsius, or -1 if unavailable
     * @throws IllegalStateException if the session has been closed; obtain a new session via
     *                               {@link GraphicsCard#createStatsSession()}
     */
    double getTemperature();

    /**
     * Returns the GPU power consumption, computed internally as an energy delta between the current sample and the
     * previous one recorded by this session.
     *
     * <p>
     * Because this metric is delta-based, the <em>first call</em> after a session is opened records the initial energy
     * baseline and returns {@code -1}. Subsequent calls return the average power in watts over the interval since the
     * previous call. To ensure the first polling iteration returns a valid value, call this method once during a
     * seeding/priming step before the polling loop begins:
     *
     * <pre>{@code
     * stats.getPowerDraw(); // prime — discards -1 return value
     * Thread.sleep(intervalMs);
     * double watts = stats.getPowerDraw(); // now returns a valid wattage
     * }</pre>
     *
     * <p>
     * On platforms where power is not sourced from an energy counter (e.g. Windows NVML/ADL/LHM), the value is
     * instantaneous and does not require priming.
     *
     * @return power draw in watts, or -1 if unavailable or not yet primed
     * @throws IllegalStateException if the session has been closed; obtain a new session via
     *                               {@link GraphicsCard#createStatsSession()}
     */
    double getPowerDraw();

    /**
     * Returns the current GPU core clock speed.
     *
     * @return core clock in MHz, or -1 if unavailable
     * @throws IllegalStateException if the session has been closed; obtain a new session via
     *                               {@link GraphicsCard#createStatsSession()}
     */
    long getCoreClockMhz();

    /**
     * Returns the current GPU memory clock speed.
     *
     * @return memory clock in MHz, or -1 if unavailable
     * @throws IllegalStateException if the session has been closed; obtain a new session via
     *                               {@link GraphicsCard#createStatsSession()}
     */
    long getMemoryClockMhz();

    /**
     * Returns the GPU fan speed as a percentage of maximum.
     *
     * @return fan speed in the range 0.0 to 100.0, or -1 if unavailable
     * @throws IllegalStateException if the session has been closed; obtain a new session via
     *                               {@link GraphicsCard#createStatsSession()}
     */
    double getFanSpeedPercent();
}
