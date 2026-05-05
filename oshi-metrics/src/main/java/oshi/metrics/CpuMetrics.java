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
import io.micrometer.core.instrument.Gauge;
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
 * <li>{@code system.cpu.physical.count} — number of physical processor cores</li>
 * <li>{@code system.cpu.logical.count} — number of logical processor cores</li>
 * <li>{@code system.cpu.frequency} — operating frequency of each logical CPU in Hertz</li>
 * </ul>
 */
public class CpuMetrics implements MeterBinder {

    private static final String CPU_TIME = "system.cpu.time";
    private static final String CPU_PHYSICAL_COUNT = "system.cpu.physical.count";
    private static final String CPU_LOGICAL_COUNT = "system.cpu.logical.count";
    private static final String CPU_FREQUENCY = "system.cpu.frequency";
    private static final String CPU_MODE_KEY = "cpu.mode";
    private static final String CPU_LOGICAL_NUMBER_KEY = "cpu.logical_number";
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
        // system.cpu.time — Counter, unit "s", attr cpu.mode
        for (TickType type : TickType.values()) {
            int index = type.getIndex();
            String mode = TICK_TO_MODE.getOrDefault(type, type.name().toLowerCase(java.util.Locale.ROOT));
            FunctionCounter.builder(CPU_TIME, processor, p -> p.getSystemCpuLoadTicks()[index] / MS_PER_SECOND)
                    .tag(CPU_MODE_KEY, mode).description("Seconds spent in each CPU mode").baseUnit("s")
                    .register(registry);
        }

        // system.cpu.physical.count — UpDownCounter (Gauge), unit "{cpu}"
        Gauge.builder(CPU_PHYSICAL_COUNT, processor, CentralProcessor::getPhysicalProcessorCount)
                .description("Reports the number of actual physical processor cores on the hardware").baseUnit("{cpu}")
                .register(registry);

        // system.cpu.logical.count — UpDownCounter (Gauge), unit "{cpu}"
        Gauge.builder(CPU_LOGICAL_COUNT, processor, CentralProcessor::getLogicalProcessorCount).description(
                "Reports the number of logical (virtual) processor cores created by the operating system to manage multitasking")
                .baseUnit("{cpu}").register(registry);

        // system.cpu.frequency — Gauge, unit "Hz", attr cpu.logical_number
        int logicalCount = processor.getLogicalProcessorCount();
        for (int i = 0; i < logicalCount; i++) {
            final int cpuIndex = i;
            Gauge.builder(CPU_FREQUENCY, processor, p -> {
                long[] freqs = p.getCurrentFreq();
                return cpuIndex < freqs.length ? (double) freqs[cpuIndex] : 0d;
            }).tag(CPU_LOGICAL_NUMBER_KEY, String.valueOf(i))
                    .description("Operating frequency of the logical CPU in Hertz").baseUnit("Hz").register(registry);
        }
    }
}
