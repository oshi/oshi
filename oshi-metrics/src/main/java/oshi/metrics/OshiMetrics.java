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
 * Entry point for OSHI system metrics. Registers metric binders with a {@link MeterRegistry}.
 *
 * <p>
 * Metric names and attributes follow
 * <a href="https://opentelemetry.io/docs/specs/semconv/system/system-metrics/">OpenTelemetry semantic conventions</a>.
 *
 * <p>
 * Usage (all metrics):
 *
 * <pre>{@code
 * SystemInfo si = new SystemInfo();
 * OshiMetrics.bindTo(registry, si.getHardware(), si.getOperatingSystem());
 * }</pre>
 *
 * <p>
 * Usage (selective):
 *
 * <pre>{@code
 * OshiMetrics.builder(si.getHardware(), si.getOperatingSystem()).enableCpu(true).enableMemory(true).enableDisk(false)
 *         .build().bindTo(registry);
 * }</pre>
 *
 * <p>
 * Since this class accepts {@link HardwareAbstractionLayer} and {@link OperatingSystem} interfaces from
 * {@code oshi-common}, it works identically with both the JNA ({@code oshi-core}) and FFM ({@code oshi-core-ffm})
 * implementations.
 */
public final class OshiMetrics implements MeterBinder {

    private final HardwareAbstractionLayer hal;
    private final OperatingSystem os;
    private final boolean general;
    private final boolean cpu;
    private final boolean memory;
    private final boolean paging;
    private final boolean disk;
    private final boolean fileSystem;
    private final boolean network;
    private final boolean process;
    private final boolean container;

    private OshiMetrics(Builder builder) {
        this.hal = builder.hal;
        this.os = builder.os;
        this.general = builder.general;
        this.cpu = builder.cpu;
        this.memory = builder.memory;
        this.paging = builder.paging;
        this.disk = builder.disk;
        this.fileSystem = builder.fileSystem;
        this.network = builder.network;
        this.process = builder.process;
        this.container = builder.container;
    }

    /**
     * Creates a new {@code OshiMetrics} instance that registers all metrics.
     *
     * @param hal the hardware abstraction layer
     * @param os  the operating system
     */
    public OshiMetrics(HardwareAbstractionLayer hal, OperatingSystem os) {
        this.hal = java.util.Objects.requireNonNull(hal, "hal must not be null");
        this.os = java.util.Objects.requireNonNull(os, "os must not be null");
        this.general = true;
        this.cpu = true;
        this.memory = true;
        this.paging = true;
        this.disk = true;
        this.fileSystem = true;
        this.network = true;
        this.process = true;
        this.container = true;
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
        if (general) {
            new GeneralMetrics(os).bindTo(registry);
        }
        if (memory) {
            new MemoryMetrics(hal.getMemory()).bindTo(registry);
        }
        if (paging) {
            new PagingMetrics(hal.getMemory().getVirtualMemory()).bindTo(registry);
        }
        if (cpu) {
            new CpuMetrics(hal.getProcessor()).bindTo(registry);
        }
        if (disk) {
            new DiskMetrics(hal::getDiskStores).bindTo(registry);
        }
        if (fileSystem) {
            new FileSystemMetrics(os.getFileSystem()::getFileStores).bindTo(registry);
        }
        if (network) {
            new NetworkMetrics(hal::getNetworkIFs, os.getInternetProtocolStats()).bindTo(registry);
        }
        if (process) {
            new ProcessMetrics(os::getCurrentProcess).bindTo(registry);
        }
        if (container) {
            new ContainerMetrics(os, os.getCgroupInfo()).bindTo(registry);
        }
    }

    /**
     * Creates a new builder for selective metric registration.
     *
     * @param hal the hardware abstraction layer
     * @param os  the operating system
     * @return a new {@link Builder}
     */
    public static Builder builder(HardwareAbstractionLayer hal, OperatingSystem os) {
        return new Builder(hal, os);
    }

    /**
     * Builder for selective metric registration.
     *
     * <p>
     * By default all metric categories are enabled. Use the {@code enable*} methods to disable specific categories.
     */
    public static final class Builder {

        private final HardwareAbstractionLayer hal;
        private final OperatingSystem os;
        private boolean general = true;
        private boolean cpu = true;
        private boolean memory = true;
        private boolean paging = true;
        private boolean disk = true;
        private boolean fileSystem = true;
        private boolean network = true;
        private boolean process = true;
        private boolean container = true;

        private Builder(HardwareAbstractionLayer hal, OperatingSystem os) {
            this.hal = java.util.Objects.requireNonNull(hal, "hal must not be null");
            this.os = java.util.Objects.requireNonNull(os, "os must not be null");
        }

        /**
         * Enable or disable general metrics (uptime, process count).
         *
         * @param enabled whether to register general metrics
         * @return this builder
         */
        public Builder enableGeneral(boolean enabled) {
            this.general = enabled;
            return this;
        }

        /**
         * Enable or disable CPU metrics (time, frequency, counts).
         *
         * @param enabled whether to register CPU metrics
         * @return this builder
         */
        public Builder enableCpu(boolean enabled) {
            this.cpu = enabled;
            return this;
        }

        /**
         * Enable or disable memory metrics (usage, limit, utilization).
         *
         * @param enabled whether to register memory metrics
         * @return this builder
         */
        public Builder enableMemory(boolean enabled) {
            this.memory = enabled;
            return this;
        }

        /**
         * Enable or disable paging/swap metrics (usage, utilization, operations).
         *
         * @param enabled whether to register paging metrics
         * @return this builder
         */
        public Builder enablePaging(boolean enabled) {
            this.paging = enabled;
            return this;
        }

        /**
         * Enable or disable disk metrics (io, operations, io_time, limit).
         *
         * @param enabled whether to register disk metrics
         * @return this builder
         */
        public Builder enableDisk(boolean enabled) {
            this.disk = enabled;
            return this;
        }

        /**
         * Enable or disable filesystem metrics (usage, utilization, limit).
         *
         * @param enabled whether to register filesystem metrics
         * @return this builder
         */
        public Builder enableFileSystem(boolean enabled) {
            this.fileSystem = enabled;
            return this;
        }

        /**
         * Enable or disable network metrics (io, packets, errors, connections).
         *
         * @param enabled whether to register network metrics
         * @return this builder
         */
        public Builder enableNetwork(boolean enabled) {
            this.network = enabled;
            return this;
        }

        /**
         * Enable or disable current process metrics (cpu, memory, disk, threads, faults, uptime).
         *
         * @param enabled whether to register process metrics
         * @return this builder
         */
        public Builder enableProcess(boolean enabled) {
            this.process = enabled;
            return this;
        }

        /**
         * Enable or disable container metrics (cpu time, memory usage/available).
         *
         * @param enabled whether to register container metrics
         * @return this builder
         */
        public Builder enableContainer(boolean enabled) {
            this.container = enabled;
            return this;
        }

        /**
         * Builds the {@link OshiMetrics} instance with the configured settings.
         *
         * @return a new {@link OshiMetrics}
         */
        public OshiMetrics build() {
            return new OshiMetrics(this);
        }
    }
}
