/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.metrics;

import oshi.software.os.CgroupInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * {@link MeterBinder} for container metrics following
 * <a href="https://opentelemetry.io/docs/specs/semconv/system/container-metrics/">OpenTelemetry semantic
 * conventions</a>.
 *
 * <p>
 * Only registers metrics when {@link CgroupInfo#isContainerized()} returns {@code true}.
 *
 * <p>
 * Registers:
 * <ul>
 * <li>{@code container.uptime} — time the container has been running, in seconds</li>
 * <li>{@code container.cpu.time} — total CPU time consumed, in seconds</li>
 * <li>{@code container.memory.usage} — memory usage in bytes</li>
 * <li>{@code container.memory.available} — memory available (limit - usage) in bytes</li>
 * </ul>
 *
 * <p>
 * Not implemented:
 * <ul>
 * <li>{@code container.cpu.usage} — requires polling interval</li>
 * <li>{@code container.disk.io}, {@code container.network.io} — OSHI does not expose per-cgroup I/O</li>
 * <li>{@code container.memory.rss}, {@code container.memory.working_set} — not available via CgroupInfo</li>
 * <li>{@code container.memory.paging.faults} — not available via CgroupInfo</li>
 * <li>{@code container.filesystem.*} — not available via CgroupInfo</li>
 * </ul>
 */
public class ContainerMetrics implements MeterBinder {

    private static final String CONTAINER_UPTIME = "container.uptime";
    private static final String CONTAINER_CPU_TIME = "container.cpu.time";
    private static final String CONTAINER_MEMORY_USAGE = "container.memory.usage";
    private static final String CONTAINER_MEMORY_AVAILABLE = "container.memory.available";
    private static final double NS_PER_SECOND = 1_000_000_000.0;

    private final OperatingSystem os;
    private final CgroupInfo cgroup;

    /**
     * Creates a new {@code ContainerMetrics} binder.
     *
     * @param os     the {@link OperatingSystem} instance
     * @param cgroup the {@link CgroupInfo} instance
     */
    public ContainerMetrics(OperatingSystem os, CgroupInfo cgroup) {
        this.os = os;
        this.cgroup = cgroup;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        if (!cgroup.isContainerized()) {
            return;
        }

        // container.uptime — Gauge, unit "s" (time since PID 1 started)
        OSProcess init = os.getProcess(1);
        final long initStartTime = (init != null && init.getStartTime() > 0) ? init.getStartTime() : -1L;
        Gauge.builder(CONTAINER_UPTIME, os,
                o -> initStartTime > 0 ? (System.currentTimeMillis() - initStartTime) / 1000.0
                        : (double) o.getSystemUptime())
                .description("The time the container has been running").baseUnit("s").strongReference(true)
                .register(registry);

        // container.cpu.time — Counter, unit "s" (cgroup reports nanoseconds)
        FunctionCounter.builder(CONTAINER_CPU_TIME, cgroup, c -> c.getCpuUsage() / NS_PER_SECOND)
                .description("Total CPU time consumed").baseUnit("s").register(registry);

        // container.memory.usage — Gauge, unit "By"
        Gauge.builder(CONTAINER_MEMORY_USAGE, cgroup, CgroupInfo::getMemoryUsage)
                .description("Memory usage of the container").baseUnit("By").strongReference(true).register(registry);

        // container.memory.available — UpDownCounter (Gauge), unit "By"
        // Only meaningful when a memory limit is set
        if (cgroup.getMemoryLimit() != CgroupInfo.UNLIMITED_MEMORY) {
            Gauge.builder(CONTAINER_MEMORY_AVAILABLE, cgroup,
                    c -> Math.max(0L, c.getMemoryLimit() - c.getMemoryUsage()))
                    .description("Container memory available").baseUnit("By").strongReference(true).register(registry);
        }
    }
}
