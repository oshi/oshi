/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.metrics;

import oshi.hardware.VirtualMemory;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * {@link MeterBinder} for system paging/swap metrics following
 * <a href="https://opentelemetry.io/docs/specs/semconv/system/system-metrics/#pagingswap-metrics">OpenTelemetry
 * semantic conventions</a>.
 *
 * <p>
 * Registers:
 * <ul>
 * <li>{@code system.paging.usage} — swap/pagefile usage by state ({@code used}, {@code free}), in bytes</li>
 * <li>{@code system.paging.utilization} — fraction of swap/pagefile in use by state (0.0–1.0)</li>
 * <li>{@code system.paging.operations} — cumulative paging operations by direction ({@code in}, {@code out})</li>
 * </ul>
 *
 * <p>
 * Note: {@code system.paging.faults} is not implemented because OSHI does not expose system-level major/minor page
 * fault counters.
 */
public class PagingMetrics implements MeterBinder {

    private static final String PAGING_USAGE = "system.paging.usage";
    private static final String PAGING_UTILIZATION = "system.paging.utilization";
    private static final String PAGING_OPERATIONS = "system.paging.operations";
    private static final Tag STATE_USED = Tag.of("system.paging.state", "used");
    private static final Tag STATE_FREE = Tag.of("system.paging.state", "free");
    private static final String DIRECTION_KEY = "system.paging.direction";

    private final VirtualMemory vm;

    /**
     * Creates a new {@code PagingMetrics} binder.
     *
     * @param vm the {@link VirtualMemory} instance to read from
     */
    public PagingMetrics(VirtualMemory vm) {
        this.vm = vm;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        // system.paging.usage — UpDownCounter (Gauge), unit "By", attr system.paging.state
        Gauge.builder(PAGING_USAGE, vm, VirtualMemory::getSwapUsed).tags(Tags.of(STATE_USED))
                .description("Unix swap or Windows pagefile usage").baseUnit("By").register(registry);
        Gauge.builder(PAGING_USAGE, vm, v -> v.getSwapTotal() - v.getSwapUsed()).tags(Tags.of(STATE_FREE))
                .description("Unix swap or Windows pagefile usage").baseUnit("By").register(registry);

        // system.paging.utilization — Gauge, unit "1", attr system.paging.state
        Gauge.builder(PAGING_UTILIZATION, vm,
                v -> v.getSwapTotal() == 0 ? 0d : (double) v.getSwapUsed() / v.getSwapTotal()).tags(Tags.of(STATE_USED))
                .description("Fraction of swap/pagefile used").register(registry);
        Gauge.builder(PAGING_UTILIZATION, vm,
                v -> v.getSwapTotal() == 0 ? 0d : (double) (v.getSwapTotal() - v.getSwapUsed()) / v.getSwapTotal())
                .tags(Tags.of(STATE_FREE)).description("Fraction of swap/pagefile free").register(registry);

        // system.paging.operations — Counter, unit "{operation}", attr system.paging.direction
        FunctionCounter.builder(PAGING_OPERATIONS, vm, VirtualMemory::getSwapPagesIn).tag(DIRECTION_KEY, "in")
                .description("Paging operations").baseUnit("{operation}").register(registry);
        FunctionCounter.builder(PAGING_OPERATIONS, vm, VirtualMemory::getSwapPagesOut).tag(DIRECTION_KEY, "out")
                .description("Paging operations").baseUnit("{operation}").register(registry);
    }
}
