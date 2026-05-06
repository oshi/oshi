/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.metrics;

import oshi.hardware.GlobalMemory;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * {@link MeterBinder} for system memory metrics following
 * <a href="https://opentelemetry.io/docs/specs/semconv/system/system-metrics/#memory-metrics">OpenTelemetry semantic
 * conventions</a>.
 *
 * <p>
 * Registers:
 * <ul>
 * <li>{@code system.memory.usage} — memory in use by state ({@code used}, {@code free}), in bytes</li>
 * <li>{@code system.memory.limit} — total memory available in the system, in bytes</li>
 * <li>{@code system.memory.utilization} — fraction of memory in use by state (0.0–1.0)</li>
 * </ul>
 */
public class MemoryMetrics implements MeterBinder {

    private static final String MEMORY_USAGE = "system.memory.usage";
    private static final String MEMORY_LIMIT = "system.memory.limit";
    private static final String MEMORY_UTILIZATION = "system.memory.utilization";
    private static final Tag STATE_USED = Tag.of("system.memory.state", "used");
    private static final Tag STATE_FREE = Tag.of("system.memory.state", "free");

    private final GlobalMemory memory;

    /**
     * Creates a new {@code MemoryMetrics} binder.
     *
     * @param memory the {@link GlobalMemory} instance to read from
     */
    public MemoryMetrics(GlobalMemory memory) {
        this.memory = memory;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder(MEMORY_USAGE, memory, mem -> mem.getTotal() - mem.getAvailable()).tags(Tags.of(STATE_USED))
                .description("Memory used in bytes").baseUnit("By").strongReference(true).register(registry);
        Gauge.builder(MEMORY_USAGE, memory, GlobalMemory::getAvailable).tags(Tags.of(STATE_FREE))
                .description("Memory available in bytes").baseUnit("By").strongReference(true).register(registry);
        Gauge.builder(MEMORY_LIMIT, memory, GlobalMemory::getTotal).description("Total memory available in the system")
                .baseUnit("By").strongReference(true).register(registry);
        Gauge.builder(MEMORY_UTILIZATION, memory,
                mem -> mem.getTotal() == 0 ? 0d : (double) (mem.getTotal() - mem.getAvailable()) / mem.getTotal())
                .tags(Tags.of(STATE_USED)).description("Fraction of memory used").strongReference(true)
                .register(registry);
        Gauge.builder(MEMORY_UTILIZATION, memory,
                mem -> mem.getTotal() == 0 ? 0d : (double) mem.getAvailable() / mem.getTotal())
                .tags(Tags.of(STATE_FREE)).description("Fraction of memory free").strongReference(true)
                .register(registry);
    }
}
