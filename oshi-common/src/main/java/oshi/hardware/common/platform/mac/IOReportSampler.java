/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.GpuTicks;

/**
 * A subscription to the macOS IOReport GPU channels, implemented once per binding.
 * <p>
 * The two implementations wrap genuinely different native APIs, so only this shape is shared. It exists so
 * {@link MacGpuStats} can hold the sampling logic once rather than duplicating it per backend.
 * <p>
 * IOReport GPU channels exist only on Apple Silicon, so a sampler is created only there; see
 * {@link MacGpuStats#MacGpuStats}.
 */
@ThreadSafe
public interface IOReportSampler extends AutoCloseable {

    /**
     * Samples the GPU busy and idle tick counters.
     *
     * @return the ticks since the previous sample
     */
    GpuTicks sampleGpuTicks();

    /**
     * Samples GPU utilization.
     *
     * @return utilization as a percentage, or a negative value if it could not be derived
     */
    double sampleGpuUtilization();

    /**
     * Samples GPU power draw.
     *
     * @return power in watts, or a negative value if it could not be derived
     */
    double samplePowerWatts();

    /**
     * Releases the subscription. Overridden to drop {@code AutoCloseable}'s checked exception.
     */
    @Override
    void close();
}
