/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import oshi.annotation.concurrent.Immutable;

/**
 * Represents a graphics card (GPU) installed in the system.
 *
 * <p>
 * Identity fields ({@link #getName()}, {@link #getDeviceId()}, {@link #getVendor()}, {@link #getVersionInfo()},
 * {@link #getVRam()}) are static and safe to read at any time.
 *
 * <p>
 * Live metrics (utilization, temperature, power, clocks, fan speed, VRAM used) are accessed through a {@link GpuStats}
 * session obtained via {@link #createStatsSession()}. For a single snapshot, use try-with-resources:
 *
 * <pre>{@code
 * try (GpuStats stats = card.createStatsSession()) {
 *     double temp = stats.getTemperature();
 *     double power = stats.getPowerDraw();
 * }
 * }</pre>
 *
 * <p>
 * For repeated polling, hold the session open across iterations to preserve internal delta state. See {@link GpuStats}
 * for a full polling example and details on which metrics require priming.
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
     * Opens a new {@link GpuStats} session for sampling dynamic GPU metrics. The caller is responsible for closing the
     * session when done, preferably via try-with-resources.
     *
     * <p>
     * Never returns null and never throws. Platforms that do not support a native session return a no-op instance whose
     * metric methods return sentinel values.
     *
     * @return a new, open {@link GpuStats} session
     */
    GpuStats createStatsSession();
}
