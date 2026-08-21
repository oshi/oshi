/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

import oshi.hardware.HWDiskStore;
import oshi.util.Memoizer;

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
 *
 * <p>
 * A disk's counters are re-read as its meters are sampled, once per disk per {@link Memoizer#defaultExpiration()}
 * window rather than once per meter, so that all of a disk's meters within a scrape report the same reading.
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
    // Intentionally retained though never read: holds a strong reference to the refreshing suppliers, and through
    // them the disk stores they close over, so the GC cannot clear the WeakReferences that Micrometer's
    // FunctionCounter keeps to them (see bindTo). Removing this would silently break the disk metrics after a
    // garbage collection. FunctionCounter has no strongReference() of its own, as Gauge does, so this is the only
    // way to keep the measured object reachable.
    @SuppressWarnings({ "java:S1068", "UnusedVariable" }) // intentionally unused field — see above
    private List<Supplier<HWDiskStore>> refreshedDisks;

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
        List<Supplier<HWDiskStore>> refreshing = new ArrayList<>(disks.size());

        for (HWDiskStore disk : disks) {
            String device = disk.getName();
            // A bound HWDiskStore holds the counters read when the binder was created, so it has to be refreshed as
            // it is sampled. The six meters below read it through one memoized supplier, both to spare the disk five
            // redundant queries per scrape and so that a single scrape reads one snapshot: bytes and operations are
            // otherwise counted from different moments, and a rate computed across the pair is not comparable.
            Supplier<HWDiskStore> refreshed = Memoizer.memoize(() -> {
                disk.updateAttributes();
                return disk;
            }, Memoizer.defaultExpiration());
            refreshing.add(refreshed);

            // system.disk.io — Counter, unit "By", attrs: disk.io.direction, system.device
            registerDiskCounter(registry, refreshed, device, "read", DISK_IO, "Disk bytes transferred", "By",
                    HWDiskStore::getReadBytes);
            registerDiskCounter(registry, refreshed, device, "write", DISK_IO, "Disk bytes transferred", "By",
                    HWDiskStore::getWriteBytes);

            // system.disk.operations — Counter, unit "{operation}", attrs: disk.io.direction, system.device
            registerDiskCounter(registry, refreshed, device, "read", DISK_OPERATIONS, "Disk operations count",
                    "{operation}", HWDiskStore::getReads);
            registerDiskCounter(registry, refreshed, device, "write", DISK_OPERATIONS, "Disk operations count",
                    "{operation}", HWDiskStore::getWrites);

            // system.disk.io_time — Counter, unit "s", attr: system.device
            FunctionCounter.builder(DISK_IO_TIME, refreshed, s -> s.get().getTransferTime() / MS_PER_SECOND)
                    .tag(DEVICE_KEY, device).description("Time disk spent activated").baseUnit("s").register(registry);

            // system.disk.limit — UpDownCounter (Gauge), unit "By", attr: system.device
            Gauge.builder(DISK_LIMIT, refreshed, s -> s.get().getSize()).tag(DEVICE_KEY, device)
                    .description("The total storage capacity of the disk").baseUnit("By").strongReference(true)
                    .register(registry);
        }

        // Hold strong references to prevent GC (FunctionCounter uses WeakReference)
        this.refreshedDisks = refreshing;
    }

    private static void registerDiskCounter(MeterRegistry registry, Supplier<HWDiskStore> refreshed, String device,
            String direction, String name, String description, String baseUnit, ToDoubleFunction<HWDiskStore> value) {
        FunctionCounter.builder(name, refreshed, s -> value.applyAsDouble(s.get())).tag(DEVICE_KEY, device)
                .tag(DIRECTION_KEY, direction).description(description).baseUnit(baseUnit).register(registry);
    }
}
