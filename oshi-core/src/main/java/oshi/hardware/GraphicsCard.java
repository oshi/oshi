/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import oshi.annotation.concurrent.Immutable;

/**
 * <p>
 * GraphicsCard interface.
 * </p>
 */
@Immutable
public interface GraphicsCard {

    /**
     * Retrieves the full name of the card.
     *
     * @return The name of the card.
     */
    String getName();

    /**
     * Retrieves the card's Device ID
     *
     * @return The Device ID of the card
     */
    String getDeviceId();

    /**
     * Retrieves the card's manufacturer/vendor
     *
     * @return The vendor of the card as human-readable text if possible, or the Vendor ID (VID) otherwise
     */
    String getVendor();

    /**
     * Retrieves a list of version/revision data from the card. Users may need to further parse this list to identify
     * specific GPU capabilities.
     *
     * @return A comma-delimited list of version/revision data
     */
    String getVersionInfo();

    /**
     * Retrieves the Video RAM (VRAM) available on the GPU
     *
     * @return Total number of bytes.
     */
    long getVRam();

    /**
     * Returns an atomic snapshot of cumulative GPU active time and the wall-clock timestamp at which it was captured,
     * both in 100-nanosecond units. Callers can compute utilization by taking two snapshots and calculating
     * {@code (delta activeTicks) / (delta timestamp) * 100}.
     *
     * <p>
     * Returns a snapshot with {@code activeTicks == 0} on platforms that do not support tick-level counters. Never
     * returns null.
     *
     * @return an immutable {@link GpuTicks} snapshot
     */
    GpuTicks getGpuTicks();

    /**
     * Returns the instantaneous GPU core utilization as a percentage.
     *
     * @return utilization in the range 0.0 to 100.0, or -1 if not available on this platform
     */
    double getGpuUtilization();

    /**
     * Returns the amount of dedicated VRAM currently in use.
     *
     * @return bytes of VRAM in use, or -1 if unavailable
     */
    long getVramUsed();

    /**
     * Returns the amount of shared system memory currently used by this GPU.
     *
     * @return bytes of shared memory in use, or -1 if unavailable
     */
    long getSharedMemoryUsed();

    /**
     * Returns the GPU temperature.
     *
     * @return temperature in degrees Celsius, or -1 if unavailable
     */
    double getTemperature();

    /**
     * Returns the GPU power consumption.
     *
     * @return power draw in watts, or -1 if unavailable
     */
    double getPowerDraw();

    /**
     * Returns the current GPU core clock speed.
     *
     * @return core clock in MHz, or -1 if unavailable
     */
    long getCoreClockMhz();

    /**
     * Returns the current GPU memory clock speed.
     *
     * @return memory clock in MHz, or -1 if unavailable
     */
    long getMemoryClockMhz();

    /**
     * Returns the GPU fan speed as a percentage of maximum. Returns -1 for passively cooled GPUs or GPUs without fan
     * sensors.
     *
     * @return fan speed in the range 0.0 to 100.0, or -1 if unavailable
     */
    double getFanSpeedPercent();
}
