/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * Reads a sysfs block tree built in a temporary directory. Linux only, because loading {@link LinuxHWDiskStore}
 * requires {@code /proc}.
 */
@EnabledOnOs(OS.LINUX)
class LinuxHWDiskStoreBackingStoresTest {

    @TempDir
    private Path sysBlock;

    private String root;

    @BeforeEach
    void setUp() throws IOException {
        root = sysBlock.toString() + "/";
        // Whole disks: sda (partitioned), sdb and sdc (md members), cciss!c0d0 (a name containing a slash)
        for (String disk : new String[] { "sda", "sdb", "sdc", "cciss!c0d0" }) {
            Files.createDirectories(sysBlock.resolve(disk).resolve("slaves"));
        }
        // An LVM or LUKS volume on a partition, a volume stacked on it, and an md RAID on two whole disks
        slaves("dm-0", "sda2");
        slaves("dm-1", "dm-0");
        slaves("md0", "sdc", "sdb");
        slaves("dm-2", "cciss!c0d0p1");
    }

    private void slaves(String disk, String... members) throws IOException {
        Path dir = Files.createDirectories(sysBlock.resolve(disk).resolve("slaves"));
        for (String member : members) {
            Files.createFile(dir.resolve(member));
        }
    }

    private List<String> read(String name) {
        Optional<List<String>> stores = LinuxHWDiskStore.readBackingStores(root, name);
        assertTrue(stores.isPresent(), name);
        return stores.get();
    }

    @Test
    void testPhysicalDiskIsNotLayered() {
        assertThat(read("/dev/sda"), is(empty()));
    }

    @Test
    void testVolumeOnPartition() {
        assertThat(read("/dev/dm-0"), contains("partition:/dev/sda2"));
    }

    @Test
    void testStackedVolumeNamesTheDiskBelow() {
        assertThat(read("/dev/dm-1"), contains("disk:/dev/dm-0"));
    }

    @Test
    void testRaidMembersAreSorted() {
        assertThat(read("/dev/md0"), contains("disk:/dev/sdb", "disk:/dev/sdc"));
    }

    @Test
    void testSysfsBangIsASlash() {
        assertThat(read("/dev/cciss/c0d0"), is(empty()));
        assertThat(read("/dev/dm-2"), contains("partition:/dev/cciss/c0d0p1"));
    }

    @Test
    void testMissingDeviceIsNotReported() {
        assertThat(LinuxHWDiskStore.readBackingStores(root, "/dev/nvme0n1"), is(Optional.empty()));
    }

    @Test
    void testReturnedListIsUnmodifiable() {
        List<String> stores = read("/dev/md0");
        assertThrows(UnsupportedOperationException.class, () -> stores.add("x"));
    }
}
