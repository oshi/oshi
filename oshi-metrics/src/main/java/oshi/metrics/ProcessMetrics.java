/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.metrics;

import java.util.function.Supplier;

import oshi.software.os.OSProcess;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * {@link MeterBinder} for process-level metrics following
 * <a href="https://opentelemetry.io/docs/specs/semconv/system/process-metrics/">OpenTelemetry semantic conventions</a>.
 *
 * <p>
 * Typically used to monitor the current JVM process via {@code os.getCurrentProcess()}.
 *
 * <p>
 * Registers:
 * <ul>
 * <li>{@code process.cpu.time} — CPU seconds by mode (user, system)</li>
 * <li>{@code process.memory.usage} — physical memory (RSS) in bytes</li>
 * <li>{@code process.memory.virtual} — virtual memory in bytes</li>
 * <li>{@code process.disk.io} — disk bytes by direction (read, write)</li>
 * <li>{@code process.thread.count} — thread count</li>
 * <li>{@code process.open_file_descriptor.count} — open file descriptors</li>
 * <li>{@code process.paging.faults} — page faults by type (major, minor)</li>
 * <li>{@code process.context_switches} — context switches (total; voluntary/involuntary split unavailable)</li>
 * <li>{@code process.uptime} — process uptime in seconds</li>
 * </ul>
 *
 * <p>
 * Not implemented:
 * <ul>
 * <li>{@code process.network.io} — OSHI does not expose per-process network I/O</li>
 * </ul>
 */
public class ProcessMetrics implements MeterBinder {

    private static final String CPU_TIME = "process.cpu.time";
    private static final String MEMORY_USAGE = "process.memory.usage";
    private static final String MEMORY_VIRTUAL = "process.memory.virtual";
    private static final String DISK_IO = "process.disk.io";
    private static final String THREAD_COUNT = "process.thread.count";
    private static final String OPEN_FD = "process.open_file_descriptor.count";
    private static final String PAGING_FAULTS = "process.paging.faults";
    private static final String CONTEXT_SWITCHES = "process.context_switches";
    private static final String UPTIME = "process.uptime";
    private static final double MS_PER_SECOND = 1000.0;

    private final Supplier<OSProcess> processSupplier;

    /**
     * Creates a new {@code ProcessMetrics} binder.
     *
     * @param processSupplier supplier that returns a fresh {@link OSProcess} snapshot (e.g.,
     *                        {@code os::getCurrentProcess})
     */
    public ProcessMetrics(Supplier<OSProcess> processSupplier) {
        this.processSupplier = processSupplier;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        // process.cpu.time — Counter, unit "s", attr cpu.mode (Required)
        FunctionCounter.builder(CPU_TIME, processSupplier, s -> s.get().getUserTime() / MS_PER_SECOND)
                .tag("cpu.mode", "user").description("Total CPU seconds broken down by different CPU modes")
                .baseUnit("s").register(registry);
        FunctionCounter.builder(CPU_TIME, processSupplier, s -> s.get().getKernelTime() / MS_PER_SECOND)
                .tag("cpu.mode", "system").description("Total CPU seconds broken down by different CPU modes")
                .baseUnit("s").register(registry);

        // process.memory.usage — UpDownCounter (Gauge), unit "By"
        Gauge.builder(MEMORY_USAGE, processSupplier, s -> s.get().getResidentMemory())
                .description("The amount of physical memory in use").baseUnit("By").strongReference(true)
                .register(registry);

        // process.memory.virtual — UpDownCounter (Gauge), unit "By"
        Gauge.builder(MEMORY_VIRTUAL, processSupplier, s -> s.get().getVirtualSize())
                .description("The amount of committed virtual memory").baseUnit("By").strongReference(true)
                .register(registry);

        // process.disk.io — Counter, unit "By", attr disk.io.direction (Required)
        FunctionCounter.builder(DISK_IO, processSupplier, s -> s.get().getBytesRead()).tag("disk.io.direction", "read")
                .description("Disk bytes transferred").baseUnit("By").register(registry);
        FunctionCounter.builder(DISK_IO, processSupplier, s -> s.get().getBytesWritten())
                .tag("disk.io.direction", "write").description("Disk bytes transferred").baseUnit("By")
                .register(registry);

        // process.thread.count — UpDownCounter (Gauge), unit "{thread}"
        Gauge.builder(THREAD_COUNT, processSupplier, s -> s.get().getThreadCount()).description("Process threads count")
                .baseUnit("{thread}").strongReference(true).register(registry);

        // process.open_file_descriptor.count — UpDownCounter (Gauge), unit "{file_descriptor}"
        Gauge.builder(OPEN_FD, processSupplier, s -> s.get().getOpenFiles())
                .description("Number of file descriptors in use by the process").baseUnit("{file_descriptor}")
                .strongReference(true).register(registry);

        // process.paging.faults — Counter, unit "{fault}", attr system.paging.fault.type (Recommended)
        FunctionCounter.builder(PAGING_FAULTS, processSupplier, s -> s.get().getMinorFaults())
                .tag("system.paging.fault.type", "minor").description("Number of page faults the process has made")
                .baseUnit("{fault}").register(registry);
        FunctionCounter.builder(PAGING_FAULTS, processSupplier, s -> s.get().getMajorFaults())
                .tag("system.paging.fault.type", "major").description("Number of page faults the process has made")
                .baseUnit("{fault}").register(registry);

        // process.context_switches — Counter, unit "{context_switch}", attr process.context_switch.type (Required)
        FunctionCounter.builder(CONTEXT_SWITCHES, processSupplier, s -> s.get().getContextSwitches())
                .tag("process.context_switch.type", "total")
                .description("Number of times the process has been context switched").baseUnit("{context_switch}")
                .register(registry);

        // process.uptime — Gauge, unit "s"
        Gauge.builder(UPTIME, processSupplier, s -> s.get().getUpTime() / MS_PER_SECOND)
                .description("The time the process has been running").baseUnit("s").strongReference(true)
                .register(registry);
    }
}
