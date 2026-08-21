/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import oshi.hardware.HWDiskStore;
import oshi.hardware.common.AbstractHWDiskStore;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Tests that {@link DiskMetrics} reads a disk once per scrape rather than once per meter, which the live tests in
 * {@code OshiMetricsTest} cannot do: a real disk's counters may or may not move between samples.
 */
class DiskMetricsTest {

    private static final String DISK_IO = "system.disk.io";
    private static final String DISK_OPERATIONS = "system.disk.operations";
    private static final String DISK_IO_TIME = "system.disk.io_time";
    private static final String DISK_LIMIT = "system.disk.limit";
    private static final String DEVICE = "system.device";
    private static final String DIRECTION = "disk.io.direction";

    private MeterRegistry registry;

    /** A disk whose counters advance on every refresh, and which counts how often it was refreshed. */
    private static class CountingHWDiskStore extends AbstractHWDiskStore {
        private int refreshes;

        CountingHWDiskStore() {
            super("disk0", "model", "serial", 1000L, "SSD");
        }

        @Override
        public boolean updateAttributes() {
            this.refreshes++;
            // Advance every counter by the same amount, so any two of them read from one refresh agree
            long n = this.refreshes;
            setDiskStats(n, n, n, n, 0L, n, 0L);
            return true;
        }

        int refreshes() {
            return this.refreshes;
        }
    }

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
    }

    private double counter(String name, String direction) {
        FunctionCounter counter = registry.find(name).tag(DEVICE, "disk0").tag(DIRECTION, direction).functionCounter();
        assertNotNull(counter, name + "{" + DIRECTION + "=" + direction + "} should be registered");
        return counter.count();
    }

    private double ioTime() {
        FunctionCounter counter = registry.find(DISK_IO_TIME).tag(DEVICE, "disk0").functionCounter();
        assertNotNull(counter, DISK_IO_TIME + " should be registered");
        return counter.count();
    }

    private double limit() {
        Gauge gauge = registry.find(DISK_LIMIT).tag(DEVICE, "disk0").gauge();
        assertNotNull(gauge, DISK_LIMIT + " should be registered");
        return gauge.value();
    }

    @Test
    void allMetersOfOneDiskShareASingleReading() {
        // Sampling six meters used to query the disk five times, so each meter's value came from a different reading.
        // They now share one memoized refresh per disk, which holds for 300 ms by default -- far longer than sampling
        // six meters in process.
        CountingHWDiskStore disk = new CountingHWDiskStore();
        new DiskMetrics(() -> Collections.<HWDiskStore>singletonList(disk)).bindTo(registry);

        double readBytes = counter(DISK_IO, "read");
        double writeBytes = counter(DISK_IO, "write");
        double reads = counter(DISK_OPERATIONS, "read");
        double writes = counter(DISK_OPERATIONS, "write");
        ioTime();
        limit();

        assertEquals(1, disk.refreshes(), "One scrape should read the disk once, not once per meter");
        assertEquals(readBytes, writeBytes, "Bytes read and written should come from the same reading");
        assertEquals(reads, writes, "Read and write operations should come from the same reading");
        assertEquals(readBytes, reads, "Bytes and operations should come from the same reading");
    }

    @Test
    void bindingDoesNotReadTheDisk() {
        // The refresh is memoized lazily, so binding a disk registers meters without querying it
        CountingHWDiskStore disk = new CountingHWDiskStore();
        new DiskMetrics(() -> Collections.<HWDiskStore>singletonList(disk)).bindTo(registry);
        assertEquals(0, disk.refreshes(), "Binding should not query the disk");
    }

    @Test
    void limitIsTheDiskSize() {
        CountingHWDiskStore disk = new CountingHWDiskStore();
        new DiskMetrics(() -> Collections.<HWDiskStore>singletonList(disk)).bindTo(registry);
        assertEquals(1000d, limit(), "The limit gauge should report the disk's size");
    }
}
