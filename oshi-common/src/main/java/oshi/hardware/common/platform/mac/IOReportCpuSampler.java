/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * A subscription to the macOS IOReport CPU performance state channels, implemented once per binding.
 * <p>
 * The two implementations wrap genuinely different native APIs, so only this shape is shared. It exists so
 * {@link MacCentralProcessor} can hold the frequency logic once rather than duplicating it per backend.
 * <p>
 * These channels exist only on Apple Silicon, so a sampler is created only there.
 */
@ThreadSafe
public interface IOReportCpuSampler extends AutoCloseable {

    /**
     * Samples how long each CPU core, and each cluster of cores, spent in each of its performance states.
     *
     * @return the residency of both since the previous sample, or null if no previous sample exists, as on the first
     *         call, or if the sample could not be taken
     */
    @Nullable
    CpuResidencySample sampleResidencyDelta();

    /**
     * Releases the subscription. Overridden to drop {@code AutoCloseable}'s checked exception.
     */
    @Override
    void close();
}
