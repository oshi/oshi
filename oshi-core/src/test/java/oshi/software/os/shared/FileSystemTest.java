/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.shared;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.SystemInfo;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.util.PlatformEnum;

/**
 * Test File System
 */
class FileSystemTest {

    /**
     * Test file system.
     *
     */
    @Test
    void testFileSystem() {
        SystemInfo si = new SystemInfo();
        FileSystem filesystem = si.getOperatingSystem().getFileSystem();
        assertThat("File system open file descriptors should be 0 or higher", filesystem.getOpenFileDescriptors(),
                greaterThanOrEqualTo(0L));
        assertThat("File system max open file descriptors should be 0 or higher", filesystem.getMaxFileDescriptors(),
                greaterThanOrEqualTo(0L));
        assertThat("File system max open file descriptors per process should be 0 or higher",
                filesystem.getMaxFileDescriptorsPerProcess(), greaterThanOrEqualTo(0L));
        filesystem.getMaxFileDescriptorsPerProcess();
        List<OSFileStore> stores = filesystem.getFileStores();
        int updateCount = 0;
        for (OSFileStore store : stores) {
            assertThat("File store name shouldn't be null", store.getName(), is(notNullValue()));
            assertThat("File store volume shouldn't be null", store.getVolume(), is(notNullValue()));
            assertThat("File store label shouldn't be null", store.getLabel(), is(notNullValue()));
            assertThat("File store logical volume shouldn't be null", store.getLogicalVolume(), is(notNullValue()));
            assertThat("File store description shouldn't be null", store.getDescription(), is(notNullValue()));
            assertThat("File store type shouldn't be null", store.getType(), is(notNullValue()));
            assertThat("File store options shouldn't be empty", store.getOptions().isEmpty(), is(false));
            assertThat("File store mount shouldn't be null", store.getMount(), is(notNullValue()));
            assertThat("File store UUID shouldn't be null", store.getUUID(), is(notNullValue()));
            assertSpaceInvariant(store);
            if (PlatformEnum.getCurrentPlatform() != PlatformEnum.WINDOWS) {
                assertThat("Number of free inodes should be 0 or higher on non-Windows systems",
                        store.getFreeInodes() >= 0, is(true));
                if (PlatformEnum.getCurrentPlatform() != PlatformEnum.SOLARIS) {
                    assertThat(
                            "Total number of inodes should be greater than or equal to number of free inodes on non-Windows/Solaris systems",
                            store.getTotalInodes() >= store.getFreeInodes(), is(true));
                }
            }
            // updateAttributes should succeed and leave the values consistent. Total space is deliberately not asserted
            // to be unchanged: a ZFS dataset draws its capacity from its pool and a swap-backed tmpfs from free memory,
            // so a genuine change between two queries is correct behavior rather than a defect.
            if (++updateCount <= 10) {
                assertThat("File store updateAttributes should succeed for " + store.getMount(),
                        store.updateAttributes(), is(true));
                assertSpaceInvariant(store);
            }
        }
    }

    /*
     * Asserts the ordering OSFileStore guarantees for its three space values on every platform and filesystem,
     * including network drives and dynamically sized filesystems. AbstractOSFileStore enforces it on the way in, so
     * this needs no per-platform exemptions.
     */
    private static void assertSpaceInvariant(OSFileStore store) {
        String on = " on " + store.getMount();
        assertThat("File store total space should be 0 or higher" + on, store.getTotalSpace(),
                greaterThanOrEqualTo(0L));
        assertThat("File store free space should be 0 or higher" + on, store.getFreeSpace(), greaterThanOrEqualTo(0L));
        assertThat("File store usable space should be 0 or higher" + on, store.getUsableSpace(),
                greaterThanOrEqualTo(0L));
        assertThat("File store free space should not exceed total space" + on, store.getFreeSpace(),
                lessThanOrEqualTo(store.getTotalSpace()));
        assertThat("File store usable space should not exceed free space" + on, store.getUsableSpace(),
                lessThanOrEqualTo(store.getFreeSpace()));
    }
}
