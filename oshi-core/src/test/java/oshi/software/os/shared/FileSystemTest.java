/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.shared;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.SystemInfo;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.util.PlatformEnum;

/**
 * Test File System
 */
class FileSystemTest {

    private static final Logger LOG = LoggerFactory.getLogger(FileSystemTest.class);

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
        long t = System.currentTimeMillis();
        java.util.List<OSFileStore> stores = filesystem.getFileStores();
        LOG.warn("[TIMING] getFileStores() returned {} stores in {}ms", stores.size(),
                System.currentTimeMillis() - t);
        t = System.currentTimeMillis();
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
            assertThat("File store total space should be 0 or higher", store.getTotalSpace() >= 0, is(true));
            assertThat("File store usable space should be 0 or higher", store.getUsableSpace() >= 0, is(true));
            assertThat("File store free space should be 0 or higher", store.getFreeSpace() >= 0, is(true));
            if (PlatformEnum.getCurrentPlatform() != PlatformEnum.WINDOWS) {
                assertThat("Number of free inodes should be 0 or higher on non-Windows systems",
                        store.getFreeInodes() >= 0, is(true));
                if (PlatformEnum.getCurrentPlatform() != PlatformEnum.SOLARIS) {
                    assertThat(
                            "Total number of inodes should be greater than or equal to number of free inodes on non-Windows/Solaris systems",
                            store.getTotalInodes() >= store.getFreeInodes(), is(true));
                }
            }
            if (!store.getDescription().equals("Network drive")) {
                assertThat(
                        "File store's usable space should be less than or equal to the total space on non-network drives",
                        store.getUsableSpace() <= store.getTotalSpace(), is(true));
            }
            // updateAttributes should succeed and total space should not change
            long totalBefore = store.getTotalSpace();
            assertThat("File store updateAttributes should succeed for " + store.getMount(), store.updateAttributes(),
                    is(true));
            if (PlatformEnum.getCurrentPlatform() != PlatformEnum.SOLARIS) {
                assertThat("File store total space should not change after update", store.getTotalSpace(),
                        is(totalBefore));
            }
            assertThat("File store usable space should remain valid after update", store.getUsableSpace(),
                    greaterThanOrEqualTo(0L));
        }
        LOG.warn("[TIMING] FileStore iteration+updateAttributes on {} stores: {}ms", stores.size(),
                System.currentTimeMillis() - t);
    }
}
