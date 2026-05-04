/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.metrics;

import java.util.EnumMap;
import java.util.Map;

import oshi.hardware.CentralProcessor;
import oshi.hardware.CentralProcessor.TickType;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * {@link MeterBinder} for system CPU metrics following
 * <a href="https://opentelemetry.io/docs/specs/semconv/system/system-metrics/#processor-metrics">OpenTelemetry semantic
 * conventions</a>.
 *
 * <p>
 * Registers:
 * <ul>
 * <li>{@code system.cpu.time} — cumulative CPU time per mode (user, system, nice, idle, iowait, interrupt, softirq,
 * steal), in seconds. Backends compute utilization via {@code rate(system.cpu.time[interval])}.</li>
 * </ul>
 */
public class CpuMetrics implements MeterBinder {

    private static final String CPU_TIME = "system.cpu.time";
    private static final String CPU_MODE_KEY = "cpu.mode";
    private static final double MS_PER_SECOND = 1000.0;

    private static final Map<TickType, String> TICK_TO_MODE = new EnumMap<>(TickType.class);

    static {
        TICK_TO_MODE.put(TickType.USER, "user");
        TICK_TO_MODE.put(TickType.NICE, "nice");
        TICK_TO_MODE.put(TickType.SYSTEM, "system");
        TICK_TO_MODE.put(TickType.IDLE, "idle");
        TICK_TO_MODE.put(TickType.IOWAIT, "iowait");
        TICK_TO_MODE.put(TickType.IRQ, "interrupt");
        TICK_TO_MODE.put(TickType.SOFTIRQ, "softirq");
        TICK_TO_MODE.put(TickType.STEAL, "steal");
    }

    private final CentralProcessor processor;

    /**
     * Creates a new {@code CpuMetrics} binder.
     *
     * @param processor the {@link CentralProcessor} instance to read from
     */
    public CpuMetrics(CentralProcessor processor) {
        this.processor = processor;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        for (TickType type : TickType.values()) {
            int index = type.getIndex();
            String mode = TICK_TO_MODE.getOrDefault(type, type.name().toLowerCase(java.util.Locale.ROOT));
            FunctionCounter.builder(CPU_TIME, processor, p -> p.getSystemCpuLoadTicks()[index] / MS_PER_SECOND)
                    .tag(CPU_MODE_KEY, mode).description("System CPU time spent in " + mode + " mode").baseUnit("s")
                    .register(registry);
        }
    }
}
