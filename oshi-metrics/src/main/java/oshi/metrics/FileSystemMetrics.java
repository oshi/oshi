/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.metrics;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

import oshi.software.os.OSFileStore;
import oshi.util.Memoizer;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * {@link MeterBinder} for system filesystem metrics following
 * <a href="https://opentelemetry.io/docs/specs/semconv/system/system-metrics/#filesystem-metrics">OpenTelemetry
 * semantic conventions</a>.
 *
 * <p>
 * Registers per filesystem:
 * <ul>
 * <li>{@code system.filesystem.usage} — filesystem space usage by state ({@code used}, {@code free}, {@code reserved}),
 * in bytes</li>
 * <li>{@code system.filesystem.utilization} — fraction of filesystem space in use by state (0.0–1.0)</li>
 * <li>{@code system.filesystem.limit} — total capacity of the filesystem, in bytes</li>
 * </ul>
 *
 * <p>
 * The three states partition the filesystem, so the {@code usage} gauges sum to {@code limit} and the
 * {@code utilization} gauges sum to 1.0. {@code free} is the space available to the calling process
 * ({@link OSFileStore#getUsableSpace()}), and {@code reserved} is unused space that is not available to it: the
 * superuser reserve many UNIX filesystems hold back, or the caller's quota on Windows. Filesystems that reserve nothing
 * at this layer, such as ZFS and APFS, report {@code reserved} as 0.
 *
 * <p>
 * A filesystem's space is re-read as its gauges are sampled, once per filesystem per
 * {@link Memoizer#defaultExpiration()} window rather than once per gauge, so that all of its gauges within a scrape
 * report the same reading.
 */
public class FileSystemMetrics implements MeterBinder {

    private static final String FS_USAGE = "system.filesystem.usage";
    private static final String FS_UTILIZATION = "system.filesystem.utilization";
    private static final String FS_LIMIT = "system.filesystem.limit";
    private static final String DEVICE_KEY = "system.device";
    private static final String MOUNTPOINT_KEY = "system.filesystem.mountpoint";
    private static final String TYPE_KEY = "system.filesystem.type";
    private static final String MODE_KEY = "system.filesystem.mode";
    private static final String STATE_KEY = "system.filesystem.state";
    private static final Tag STATE_USED = Tag.of(STATE_KEY, "used");
    private static final Tag STATE_FREE = Tag.of(STATE_KEY, "free");
    private static final Tag STATE_RESERVED = Tag.of(STATE_KEY, "reserved");

    private final Supplier<List<OSFileStore>> fileStoreSupplier;

    /**
     * Creates a new {@code FileSystemMetrics} binder.
     *
     * <p>
     * Note: {@link #bindTo(MeterRegistry)} calls the supplier once to capture a snapshot of filesystems. Filesystems
     * mounted after binding will not be tracked; unmounted filesystems may leave stale meters. To refresh, create and
     * bind a new instance.
     *
     * @param fileStoreSupplier supplier that returns the current list of {@link OSFileStore} instances
     */
    public FileSystemMetrics(Supplier<List<OSFileStore>> fileStoreSupplier) {
        this.fileStoreSupplier = fileStoreSupplier;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        for (OSFileStore fs : fileStoreSupplier.get()) {
            String opts = fs.getOptions();
            String mode = Arrays.asList(opts.split(",")).contains("rw") ? "rw" : "ro";
            Tags tags = Tags.of(DEVICE_KEY, fs.getVolume(), MOUNTPOINT_KEY, fs.getMount(), TYPE_KEY, fs.getType(),
                    MODE_KEY, mode);
            // A bound OSFileStore is a snapshot taken when the binder was created, so it has to be refreshed as it is
            // sampled. The seven gauges below share one memoized refresh, both to spare the filesystem six redundant
            // queries per scrape and so that a single scrape reads one snapshot: the states can only be shown to
            // partition the filesystem if they were all measured against the same reading of it.
            Supplier<Boolean> refresh = Memoizer.memoize(fs::updateAttributes, Memoizer.defaultExpiration());

            // system.filesystem.usage — UpDownCounter (Gauge), unit "By", attr: state, device, mount, type, mode.
            // OSFileStore guarantees usable <= free <= total, so none of these differences can be negative and the
            // three states sum to the total.
            registerUsage(registry, fs, tags, STATE_USED, refresh, f -> f.getTotalSpace() - f.getFreeSpace());
            registerUsage(registry, fs, tags, STATE_FREE, refresh, OSFileStore::getUsableSpace);
            registerUsage(registry, fs, tags, STATE_RESERVED, refresh, f -> f.getFreeSpace() - f.getUsableSpace());

            // system.filesystem.utilization — Gauge, unit "1", attr: state, device, mount, type, mode
            registerUtilization(registry, fs, tags, STATE_USED, refresh,
                    f -> fraction(f.getTotalSpace() - f.getFreeSpace(), f.getTotalSpace()));
            registerUtilization(registry, fs, tags, STATE_FREE, refresh,
                    f -> fraction(f.getUsableSpace(), f.getTotalSpace()));
            registerUtilization(registry, fs, tags, STATE_RESERVED, refresh,
                    f -> fraction(f.getFreeSpace() - f.getUsableSpace(), f.getTotalSpace()));

            // system.filesystem.limit — UpDownCounter (Gauge), unit "By", attr: device, mount, type, mode
            Gauge.builder(FS_LIMIT, fs, refreshing(refresh, OSFileStore::getTotalSpace)).tags(tags)
                    .description("Total capacity of the filesystem").baseUnit("By").strongReference(true)
                    .register(registry);
        }
    }

    private static void registerUsage(MeterRegistry registry, OSFileStore fs, Tags tags, Tag state,
            Supplier<Boolean> refresh, ToDoubleFunction<OSFileStore> bytes) {
        Gauge.builder(FS_USAGE, fs, refreshing(refresh, bytes)).tags(tags.and(state))
                .description("Filesystem space usage").baseUnit("By").strongReference(true).register(registry);
    }

    private static void registerUtilization(MeterRegistry registry, OSFileStore fs, Tags tags, Tag state,
            Supplier<Boolean> refresh, ToDoubleFunction<OSFileStore> ratio) {
        Gauge.builder(FS_UTILIZATION, fs, refreshing(refresh, ratio)).tags(tags.and(state))
                .description("Filesystem utilization").strongReference(true).register(registry);
    }

    private static ToDoubleFunction<OSFileStore> refreshing(Supplier<Boolean> refresh,
            ToDoubleFunction<OSFileStore> value) {
        return f -> {
            refresh.get();
            return value.applyAsDouble(f);
        };
    }

    private static double fraction(long value, long total) {
        return total == 0 ? 0d : (double) value / total;
    }
}
