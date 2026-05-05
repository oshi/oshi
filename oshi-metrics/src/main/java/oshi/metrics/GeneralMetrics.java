/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.metrics;

import oshi.software.os.OperatingSystem;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * {@link MeterBinder} for general system metrics following
 * <a href="https://opentelemetry.io/docs/specs/semconv/system/system-metrics/#general-metrics">OpenTelemetry semantic
 * conventions</a>.
 *
 * <p>
 * Registers:
 * <ul>
 * <li>{@code system.uptime} — the time the system has been running, in seconds</li>
 * <li>{@code system.process.count} — total number of processes on the system</li>
 * </ul>
 */
public class GeneralMetrics implements MeterBinder {

    private static final String SYSTEM_UPTIME = "system.uptime";
    private static final String PROCESS_COUNT = "system.process.count";

    private final OperatingSystem os;

    /**
     * Creates a new {@code GeneralMetrics} binder.
     *
     * @param os the {@link OperatingSystem} instance to read from
     */
    public GeneralMetrics(OperatingSystem os) {
        this.os = os;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder(SYSTEM_UPTIME, os, o -> (double) o.getSystemUptime())
                .description("The time the system has been running").baseUnit("s").register(registry);
        Gauge.builder(PROCESS_COUNT, os, OperatingSystem::getProcessCount)
                .description("Total number of processes on the system").baseUnit("{process}").register(registry);
    }
}
