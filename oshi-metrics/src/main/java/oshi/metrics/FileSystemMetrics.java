/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.metrics;

import java.util.List;
import java.util.function.Supplier;

import oshi.software.os.OSFileStore;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * {@link MeterBinder} for system filesystem metrics following
 * <a href="https://opentelemetry.io/docs/specs/semconv/system/system-metrics/#filesystem-metrics">OpenTelemetry
 * semantic conventions</a>.
 *
 * <p>
 * Registers per filesystem:
 * <ul>
 * <li>{@code system.filesystem.usage} — filesystem space usage by state ({@code used}, {@code free}), in bytes</li>
 * <li>{@code system.filesystem.utilization} — fraction of filesystem space in use by state (0.0–1.0)</li>
 * <li>{@code system.filesystem.limit} — total capacity of the filesystem, in bytes</li>
 * </ul>
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
            String device = fs.getVolume();
            String mount = fs.getMount();
            String type = fs.getType();
            String opts = fs.getOptions();
            String mode = opts != null && java.util.Arrays.asList(opts.split(",")).contains("rw") ? "rw" : "ro";

            // system.filesystem.usage — UpDownCounter (Gauge), unit "By", attr: state, device, mount, type, mode
            Gauge.builder(FS_USAGE, fs, f -> {
                f.updateAttributes();
                return Math.max(0L, f.getTotalSpace() - f.getUsableSpace());
            }).tag(STATE_KEY, "used").tag(DEVICE_KEY, device).tag(MOUNTPOINT_KEY, mount).tag(TYPE_KEY, type)
                    .tag(MODE_KEY, mode).description("Filesystem space usage").baseUnit("By").strongReference(true)
                    .register(registry);
            Gauge.builder(FS_USAGE, fs, f -> {
                f.updateAttributes();
                return (double) f.getUsableSpace();
            }).tag(STATE_KEY, "free").tag(DEVICE_KEY, device).tag(MOUNTPOINT_KEY, mount).tag(TYPE_KEY, type)
                    .tag(MODE_KEY, mode).description("Filesystem space usage").baseUnit("By").strongReference(true)
                    .register(registry);

            // system.filesystem.utilization — Gauge, unit "1", attr: state, device, mount, type, mode
            Gauge.builder(FS_UTILIZATION, fs, f -> {
                f.updateAttributes();
                return f.getTotalSpace() == 0 ? 0d
                        : Math.max(0d, (double) (f.getTotalSpace() - f.getUsableSpace()) / f.getTotalSpace());
            }).tag(STATE_KEY, "used").tag(DEVICE_KEY, device).tag(MOUNTPOINT_KEY, mount).tag(TYPE_KEY, type)
                    .tag(MODE_KEY, mode).description("Filesystem utilization").strongReference(true).register(registry);
            Gauge.builder(FS_UTILIZATION, fs, f -> {
                f.updateAttributes();
                return f.getTotalSpace() == 0 ? 0d : (double) f.getUsableSpace() / f.getTotalSpace();
            }).tag(STATE_KEY, "free").tag(DEVICE_KEY, device).tag(MOUNTPOINT_KEY, mount).tag(TYPE_KEY, type)
                    .tag(MODE_KEY, mode).description("Filesystem utilization").strongReference(true).register(registry);

            // system.filesystem.limit — UpDownCounter (Gauge), unit "By", attr: device, mount, type, mode
            Gauge.builder(FS_LIMIT, fs, f -> {
                f.updateAttributes();
                return (double) f.getTotalSpace();
            }).tag(DEVICE_KEY, device).tag(MOUNTPOINT_KEY, mount).tag(TYPE_KEY, type).tag(MODE_KEY, mode)
                    .description("Total capacity of the filesystem").baseUnit("By").strongReference(true)
                    .register(registry);
        }
    }
}
