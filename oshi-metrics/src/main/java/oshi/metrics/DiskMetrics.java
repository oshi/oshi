/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.metrics;

import java.util.List;
import java.util.function.Supplier;

import oshi.hardware.HWDiskStore;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * {@link MeterBinder} for system disk controller metrics following
 * <a href="https://opentelemetry.io/docs/specs/semconv/system/system-metrics/#disk-controller-metrics">OpenTelemetry
 * semantic conventions</a>.
 *
 * <p>
 * Registers per device:
 * <ul>
 * <li>{@code system.disk.io} — disk bytes transferred by direction (read, write)</li>
 * <li>{@code system.disk.operations} — disk operations count by direction (read, write)</li>
 * <li>{@code system.disk.io_time} — time disk spent activated, in seconds</li>
 * <li>{@code system.disk.limit} — total storage capacity of the disk, in bytes</li>
 * </ul>
 *
 * <p>
 * Not implemented:
 * <ul>
 * <li>{@code system.disk.operation_time} — OSHI does not expose per-direction operation time</li>
 * <li>{@code system.disk.merged} — OSHI does not expose merged operation counts</li>
 * </ul>
 */
public class DiskMetrics implements MeterBinder {

    private static final String DISK_IO = "system.disk.io";
    private static final String DISK_OPERATIONS = "system.disk.operations";
    private static final String DISK_IO_TIME = "system.disk.io_time";
    private static final String DISK_LIMIT = "system.disk.limit";
    private static final String DEVICE_KEY = "system.device";
    private static final String DIRECTION_KEY = "disk.io.direction";
    private static final double MS_PER_SECOND = 1000.0;

    private final Supplier<List<HWDiskStore>> diskStoreSupplier;
    private List<HWDiskStore> diskStores;

    /**
     * Creates a new {@code DiskMetrics} binder.
     *
     * @param diskStoreSupplier supplier that returns the current list of {@link HWDiskStore} instances
     */
    public DiskMetrics(Supplier<List<HWDiskStore>> diskStoreSupplier) {
        this.diskStoreSupplier = diskStoreSupplier;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        List<HWDiskStore> disks = diskStoreSupplier.get();
        // Hold strong references to prevent GC (FunctionCounter uses WeakReference)
        this.diskStores = disks;

        for (HWDiskStore disk : disks) {
            String device = disk.getName();

            // system.disk.io — Counter, unit "By", attrs: disk.io.direction, system.device
            FunctionCounter.builder(DISK_IO, disk, d -> {
                d.updateAttributes();
                return d.getReadBytes();
            }).tag(DEVICE_KEY, device).tag(DIRECTION_KEY, "read").description("Disk bytes transferred").baseUnit("By")
                    .register(registry);
            FunctionCounter.builder(DISK_IO, disk, d -> {
                d.updateAttributes();
                return d.getWriteBytes();
            }).tag(DEVICE_KEY, device).tag(DIRECTION_KEY, "write").description("Disk bytes transferred").baseUnit("By")
                    .register(registry);

            // system.disk.operations — Counter, unit "{operation}", attrs: disk.io.direction, system.device
            FunctionCounter.builder(DISK_OPERATIONS, disk, d -> {
                d.updateAttributes();
                return d.getReads();
            }).tag(DEVICE_KEY, device).tag(DIRECTION_KEY, "read").description("Disk operations count")
                    .baseUnit("{operation}").register(registry);
            FunctionCounter.builder(DISK_OPERATIONS, disk, d -> {
                d.updateAttributes();
                return d.getWrites();
            }).tag(DEVICE_KEY, device).tag(DIRECTION_KEY, "write").description("Disk operations count")
                    .baseUnit("{operation}").register(registry);

            // system.disk.io_time — Counter, unit "s", attr: system.device
            FunctionCounter.builder(DISK_IO_TIME, disk, d -> {
                d.updateAttributes();
                return d.getTransferTime() / MS_PER_SECOND;
            }).tag(DEVICE_KEY, device).description("Time disk spent activated").baseUnit("s").register(registry);

            // system.disk.limit — UpDownCounter (Gauge), unit "By", attr: system.device
            Gauge.builder(DISK_LIMIT, disk, HWDiskStore::getSize).tag(DEVICE_KEY, device)
                    .description("The total storage capacity of the disk").baseUnit("By").strongReference(true)
                    .register(registry);
        }
    }
}
