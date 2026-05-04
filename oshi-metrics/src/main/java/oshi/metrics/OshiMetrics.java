/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.metrics;

import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * Entry point for OSHI system metrics. Registers all available metric binders with a {@link MeterRegistry}.
 *
 * <p>
 * Metric names and attributes follow
 * <a href="https://opentelemetry.io/docs/specs/semconv/system/system-metrics/">OpenTelemetry semantic conventions</a>.
 *
 * <p>
 * Usage:
 *
 * <pre>{@code
 * SystemInfo si = new SystemInfo();
 * OshiMetrics.bindTo(registry, si.getHardware(), si.getOperatingSystem());
 * }</pre>
 *
 * <p>
 * Since this class accepts {@link HardwareAbstractionLayer} and {@link OperatingSystem} interfaces from
 * {@code oshi-common}, it works identically with both the JNA ({@code oshi-core}) and FFM ({@code oshi-core-ffm})
 * implementations.
 */
public final class OshiMetrics implements MeterBinder {

    private final HardwareAbstractionLayer hal;
    // Reserved for future OS-level metrics (e.g., process, cgroup)
    private final OperatingSystem os;

    /**
     * Creates a new {@code OshiMetrics} instance.
     *
     * @param hal the hardware abstraction layer
     * @param os  the operating system
     */
    public OshiMetrics(HardwareAbstractionLayer hal, OperatingSystem os) {
        this.hal = hal;
        this.os = os;
    }

    /**
     * Convenience method to create and bind all OSHI metrics in one call.
     *
     * @param registry the meter registry
     * @param hal      the hardware abstraction layer
     * @param os       the operating system
     */
    public static void bindTo(MeterRegistry registry, HardwareAbstractionLayer hal, OperatingSystem os) {
        new OshiMetrics(hal, os).bindTo(registry);
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        new MemoryMetrics(hal.getMemory()).bindTo(registry);
        new CpuMetrics(hal.getProcessor()).bindTo(registry);
    }
}
