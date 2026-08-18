/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

class AbstractOSFileStoreTest {

    private static TestOSFileStore createFileStore() {
        return new TestOSFileStore("TestFS", "/dev/sda1", "MyLabel", "/mnt/data", "rw,noatime", "1234-5678", true,
                "lv0", "Local Disk", "ext4", 1000L, 900L, 5000L, 100L, 500L);
    }

    /** Test subclass exposing protected helpers. */
    private static class TestOSFileStore extends AbstractOSFileStore {
        TestOSFileStore(String name, String volume, String label, String mount, String options, String uuid,
                boolean local, String logicalVolume, String description, String fsType, long freeSpace,
                long usableSpace, long totalSpace, long freeInodes, long totalInodes) {
            super(name, volume, label, mount, options, uuid, local, logicalVolume, description, fsType, freeSpace,
                    usableSpace, totalSpace, freeInodes, totalInodes);
        }

        @Override
        public boolean updateAttributes() {
            return false;
        }

        void callUpdateFrom(oshi.software.os.OSFileStore other) {
            updateFrom(other);
        }

        void callUpdateSpaceAndInodes(long free, long usable, long total, long freeIn, long totalIn) {
            updateSpaceAndInodes(free, usable, total, freeIn, totalIn);
        }

        void callUpdateSpace(long free, long usable, long total) {
            updateSpace(free, usable, total);
        }
    }

    @Test
    void testGetters() {
        AbstractOSFileStore fs = createFileStore();
        assertThat(fs.getName(), is("TestFS"));
        assertThat(fs.getVolume(), is("/dev/sda1"));
        assertThat(fs.getLabel(), is("MyLabel"));
        assertThat(fs.getMount(), is("/mnt/data"));
        assertThat(fs.getOptions(), is("rw,noatime"));
        assertThat(fs.getUUID(), is("1234-5678"));
        assertThat(fs.isLocal(), is(true));
        assertThat(fs.getLogicalVolume(), is("lv0"));
        assertThat(fs.getDescription(), is("Local Disk"));
        assertThat(fs.getType(), is("ext4"));
        assertThat(fs.getFreeSpace(), is(1000L));
        assertThat(fs.getUsableSpace(), is(900L));
        assertThat(fs.getTotalSpace(), is(5000L));
        assertThat(fs.getFreeInodes(), is(100L));
        assertThat(fs.getTotalInodes(), is(500L));
    }

    @Test
    void testToString() {
        String s = createFileStore().toString();
        assertThat(s, containsString("name=TestFS"));
        assertThat(s, containsString("ext4"));
        assertThat(s, containsString("lv0"));
    }

    @Test
    void testUpdateSpaceAndInodes() {
        TestOSFileStore fs = createFileStore();
        fs.callUpdateSpaceAndInodes(2000L, 1800L, 10000L, 200L, 1000L);
        assertThat(fs.getFreeSpace(), is(2000L));
        assertThat(fs.getUsableSpace(), is(1800L));
        assertThat(fs.getTotalSpace(), is(10000L));
        assertThat(fs.getFreeInodes(), is(200L));
        assertThat(fs.getTotalInodes(), is(1000L));
    }

    @Test
    void testUpdateSpace() {
        TestOSFileStore fs = createFileStore();
        fs.callUpdateSpace(3000L, 2500L, 20000L);
        assertThat(fs.getFreeSpace(), is(3000L));
        assertThat(fs.getUsableSpace(), is(2500L));
        assertThat(fs.getTotalSpace(), is(20000L));
        // Inodes unchanged
        assertThat(fs.getFreeInodes(), is(100L));
        assertThat(fs.getTotalInodes(), is(500L));
    }

    @Test
    void testSpaceExceedingTotalIsClampedDown() {
        // usable > total is reachable on ZFS, where a dataset's capacity is derived from its pool, and on a
        // swap-backed tmpfs, where it is derived from free memory: total is sampled at a different instant than the
        // other two values. Total stays as the OS reported it; the other two come down to meet it.
        TestOSFileStore fs = new TestOSFileStore("TestFS", "/dev/sda1", "MyLabel", "/mnt/data", "rw", "1234-5678", true,
                "lv0", "Local Disk", "zfs", 6000L, 5500L, 5000L, 100L, 500L);
        assertThat(fs.getTotalSpace(), is(5000L));
        assertThat(fs.getFreeSpace(), is(5000L));
        assertThat(fs.getUsableSpace(), is(5000L));
    }

    @Test
    void testUsableExceedingFreeIsClampedDown() {
        // f_bavail underflowing past the superuser reserve on a full filesystem is the realistic source of this.
        TestOSFileStore fs = createFileStore();
        fs.callUpdateSpace(1000L, 4000L, 5000L);
        assertThat(fs.getTotalSpace(), is(5000L));
        assertThat(fs.getFreeSpace(), is(1000L));
        assertThat(fs.getUsableSpace(), is(1000L));
    }

    @Test
    void testNegativeSpaceIsFlooredAtZero() {
        TestOSFileStore fs = createFileStore();
        fs.callUpdateSpaceAndInodes(-1L, -2L, -3L, 200L, 1000L);
        assertThat(fs.getTotalSpace(), is(0L));
        assertThat(fs.getFreeSpace(), is(0L));
        assertThat(fs.getUsableSpace(), is(0L));
        // Inodes carry a documented -1 "unimplemented" sentinel and are not normalized
        assertThat(fs.getFreeInodes(), is(200L));
        assertThat(fs.getTotalInodes(), is(1000L));
    }

    @Test
    void testUpdateFrom() {
        TestOSFileStore fs = createFileStore();
        TestOSFileStore other = new TestOSFileStore("Other", "/dev/sdb1", "OtherLabel", "/mnt/other", "ro", "9999-0000",
                false, "lv1", "Network Disk", "nfs", 5000L, 4500L, 50000L, 400L, 2000L);
        fs.callUpdateFrom(other);
        assertThat(fs.getLogicalVolume(), is("lv1"));
        assertThat(fs.getDescription(), is("Network Disk"));
        assertThat(fs.getType(), is("nfs"));
        assertThat(fs.getFreeSpace(), is(5000L));
        assertThat(fs.getUsableSpace(), is(4500L));
        assertThat(fs.getTotalSpace(), is(50000L));
        assertThat(fs.getFreeInodes(), is(400L));
        assertThat(fs.getTotalInodes(), is(2000L));
    }
}
