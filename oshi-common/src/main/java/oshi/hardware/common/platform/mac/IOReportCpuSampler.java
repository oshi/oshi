/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import java.util.Map;

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
     * Samples how long each CPU core spent in each of its performance states.
     * <p>
     * The states of one core are its idle state or states followed by one state per frequency the core's cluster can
     * run at, in ascending frequency order, so the returned per-channel maps must preserve that order.
     *
     * @return a map from channel name, which identifies the core, to a map from state name to the ticks spent in that
     *         state since the previous sample, in channel state order. Null if no previous sample exists, as on the
     *         first call, or if the sample could not be taken.
     */
    @Nullable
    Map<String, Map<String, Long>> sampleCoreResidencyDelta();

    /**
     * Releases the subscription. Overridden to drop {@code AutoCloseable}'s checked exception.
     */
    @Override
    void close();
}
