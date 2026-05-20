/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.hardware.HWPartition;

class AbstractHWDiskStoreTest {

    private static TestHWDiskStore createDisk() {
        return new TestHWDiskStore("sda", "Model X", "SN123", 500_000_000_000L);
    }

    /** Test subclass exposing protected setters. */
    private static class TestHWDiskStore extends AbstractHWDiskStore {
        TestHWDiskStore(String name, String model, String serial, long size) {
            super(name, model, serial, size);
        }

        @Override
        public boolean updateAttributes() {
            return false;
        }

        void applyStats(long reads, long readBytes, long writes, long writeBytes, long queueLen, long xferTime,
                long ts) {
            setDiskStats(reads, readBytes, writes, writeBytes, queueLen, xferTime, ts);
        }

        void applyCurrentQueueLength(long queueLen) {
            setCurrentQueueLength(queueLen);
        }

        void applyPartitions(List<HWPartition> parts) {
            setPartitionList(parts);
        }
    }

    @Test
    void testGettersAndSetDiskStats() {
        TestHWDiskStore disk = createDisk();
        assertThat(disk.getName(), is("sda"));
        assertThat(disk.getModel(), is("Model X"));
        assertThat(disk.getSerial(), is("SN123"));
        assertThat(disk.getSize(), is(500_000_000_000L));
        assertThat(disk.getDiskType(), is("Unknown"));

        disk.applyStats(100L, 2048L, 50L, 1024L, 3L, 500L, 99999L);
        assertThat(disk.getReads(), is(100L));
        assertThat(disk.getReadBytes(), is(2048L));
        assertThat(disk.getWrites(), is(50L));
        assertThat(disk.getWriteBytes(), is(1024L));
        assertThat(disk.getCurrentQueueLength(), is(3L));
        assertThat(disk.getTransferTime(), is(500L));
        assertThat(disk.getTimeStamp(), is(99999L));
    }

    @Test
    void testSetCurrentQueueLength() {
        TestHWDiskStore disk = createDisk();
        disk.applyCurrentQueueLength(7L);
        assertThat(disk.getCurrentQueueLength(), is(7L));
    }

    @Test
    void testPartitions() {
        TestHWDiskStore disk = createDisk();
        assertThat(disk.getPartitions(), is(Collections.emptyList()));
        List<HWPartition> parts = Collections
                .singletonList(new HWPartition("sda1", "sda1", "ext4", "uuid1", 100_000L, 0, 8, "/"));
        disk.applyPartitions(parts);
        assertThat(disk.getPartitions().size(), is(1));
    }

    @Test
    void testToString() {
        TestHWDiskStore disk = createDisk();
        disk.applyStats(10L, 512L, 5L, 256L, 0L, 100L, 1000L);
        String s = disk.toString();
        assertThat(s, containsString("sda"));
        assertThat(s, containsString("Model X"));
    }
}
