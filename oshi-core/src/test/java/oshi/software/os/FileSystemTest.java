/*
 * Copyright 2016-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os;

import org.junit.jupiter.api.Test;
import oshi.PlatformEnum;
import oshi.SystemInfo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

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
        for (OSFileStore store : filesystem.getFileStores()) {
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
            if (SystemInfo.getCurrentPlatform() != PlatformEnum.WINDOWS) {
                assertThat("Number of free inodes should be 0 or higher on non-Windows systems",
                        store.getFreeInodes() >= 0, is(true));
                if (SystemInfo.getCurrentPlatform() != PlatformEnum.SOLARIS) {
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
        }
    }
}
