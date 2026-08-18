/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import oshi.software.common.AbstractOSFileStore;
import oshi.software.os.OSFileStore;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Tests the state arithmetic of {@link FileSystemMetrics} against fixed values, which the live tests in
 * {@code OshiMetricsTest} cannot do: a real filesystem's space changes between samples.
 */
class FileSystemMetricsTest {

    private static final String USAGE = "system.filesystem.usage";
    private static final String UTILIZATION = "system.filesystem.utilization";
    private static final String LIMIT = "system.filesystem.limit";
    private static final String STATE = "system.filesystem.state";
    private static final String[] STATES = { "used", "free", "reserved" };
    private static final double DELTA = 1e-9;

    private MeterRegistry registry;

    /** A file store with fixed space values, so a gauge reads the same number every time it is sampled. */
    private static class FixedOSFileStore extends AbstractOSFileStore {
        FixedOSFileStore(long freeSpace, long usableSpace, long totalSpace) {
            super("fixed", "/dev/fixed", "label", "/mnt/fixed", "rw,noatime", "uuid", true, "lv", "Local Disk", "ext4",
                    freeSpace, usableSpace, totalSpace, 100L, 500L);
        }

        @Override
        public boolean updateAttributes() {
            return true;
        }
    }

    /** A file store that reports different space every time it is refreshed, as a busy one does. */
    private static class VaryingOSFileStore extends AbstractOSFileStore {
        private long free = 1000L;

        VaryingOSFileStore() {
            super("varying", "/dev/varying", "label", "/mnt/varying", "rw", "uuid", true, "lv", "Local Disk", "ext4",
                    1000L, 900L, 10000L, 100L, 500L);
        }

        @Override
        public boolean updateAttributes() {
            this.free -= 100L;
            updateSpace(this.free, this.free - 100L, 10000L);
            return true;
        }
    }

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
    }

    private void bind(long freeSpace, long usableSpace, long totalSpace) {
        OSFileStore fs = new FixedOSFileStore(freeSpace, usableSpace, totalSpace);
        new FileSystemMetrics(() -> Collections.<OSFileStore>singletonList(fs)).bindTo(registry);
    }

    private double usage(String state) {
        Gauge gauge = registry.find(USAGE).tag(STATE, state).gauge();
        assertNotNull(gauge, USAGE + "{state=" + state + "} should be registered");
        return gauge.value();
    }

    private double utilization(String state) {
        Gauge gauge = registry.find(UTILIZATION).tag(STATE, state).gauge();
        assertNotNull(gauge, UTILIZATION + "{state=" + state + "} should be registered");
        return gauge.value();
    }

    private double limit() {
        Gauge gauge = registry.find(LIMIT).gauge();
        assertNotNull(gauge, LIMIT + " should be registered");
        return gauge.value();
    }

    @Test
    void reservedSpaceIsReportedSeparatelyFromUsedAndFree() {
        // 1000 total, 200 unused, only 150 of it available to the caller: the other 50 is the superuser reserve
        bind(200L, 150L, 1000L);
        assertEquals(800d, usage("used"), "used should exclude the reserve");
        assertEquals(150d, usage("free"), "free should be the space available to the caller");
        assertEquals(50d, usage("reserved"), "reserved should be the unused space unavailable to the caller");
    }

    @Test
    void usageStatesSumToLimit() {
        // OpenTelemetry's filesystem conventions require summing usage across states to equal the limit
        bind(200L, 150L, 1000L);
        assertEquals(limit(), usage("used") + usage("free") + usage("reserved"),
                "usage states should sum to the filesystem limit");
    }

    @Test
    void utilizationStatesSumToOne() {
        bind(200L, 150L, 1000L);
        assertEquals(0.8d, utilization("used"), DELTA);
        assertEquals(0.15d, utilization("free"), DELTA);
        assertEquals(0.05d, utilization("reserved"), DELTA);
        assertEquals(1d, utilization("used") + utilization("free") + utilization("reserved"), DELTA,
                "utilization states should sum to 1.0");
    }

    @Test
    void filesystemWithoutAReserveReportsZeroReserved() {
        // ZFS and APFS reserve nothing at this layer, so statvfs reports f_bfree == f_bavail
        bind(200L, 200L, 1000L);
        assertEquals(800d, usage("used"));
        assertEquals(200d, usage("free"));
        assertEquals(0d, usage("reserved"));
    }

    @Test
    void emptyFilesystemReportsZeroUtilization() {
        // A pseudo filesystem with no capacity would otherwise divide by zero
        bind(0L, 0L, 0L);
        assertEquals(0d, limit());
        assertEquals(0d, utilization("used"));
        assertEquals(0d, utilization("free"));
        assertEquals(0d, utilization("reserved"));
    }

    @Test
    void allGaugesOfOneFilesystemShareASingleReading() {
        // A filesystem whose space changes on every refresh would break the partition rule if each gauge refreshed
        // separately: the states would be measured against four different readings. They share one memoized refresh
        // per filesystem, which holds for 300 ms by default -- far longer than sampling four gauges in process.
        OSFileStore fs = new VaryingOSFileStore();
        new FileSystemMetrics(() -> Collections.singletonList(fs)).bindTo(registry);
        assertEquals(limit(), usage("used") + usage("free") + usage("reserved"),
                "usage states should sum to the limit even while the filesystem is changing");
    }

    @Test
    void inconsistentSpaceValuesCannotProduceNegativeStates() {
        // AbstractOSFileStore clamps usable <= free <= total, so no state can go negative even when the operating
        // system reports the three values inconsistently
        bind(6000L, 5500L, 5000L);
        for (String state : STATES) {
            assertTrue(usage(state) >= 0d, USAGE + "{state=" + state + "} should not be negative");
            assertTrue(utilization(state) >= 0d, UTILIZATION + "{state=" + state + "} should not be negative");
        }
        assertEquals(limit(), usage("used") + usage("free") + usage("reserved"));
    }
}
