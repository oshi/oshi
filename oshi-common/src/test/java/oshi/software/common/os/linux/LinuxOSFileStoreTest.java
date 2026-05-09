/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.software.os.OSFileStore;

/**
 * Tests LinuxOSFileStore, particularly the updateAttributes method.
 */
@EnabledOnOs(OS.LINUX)
class LinuxOSFileStoreTest {

    /** Subclass that returns null from queryStatvfs to test the JVM File fallback path. */
    private static final class NullStatvfsFileSystem extends LinuxFileSystem {
        @Override
        protected long[] queryStatvfs(String path) {
            return null;
        }
    }

    /** Subclass that returns zeros from queryStatvfs to test the JVM File fallback within updateAttributes. */
    private static final class ZeroStatvfsFileSystem extends LinuxFileSystem {
        @Override
        protected long[] queryStatvfs(String path) {
            // Return array with totalInodes, freeInodes, totalSpace=0, usableSpace=0, freeSpace=0
            // This triggers the JVM File fallback inside updateAttributes
            return new long[] { 1000L, 500L, 0L, 0L, 0L };
        }
    }

    @Test
    void testUpdateAttributesWithNullStatvfs() {
        NullStatvfsFileSystem fs = new NullStatvfsFileSystem();
        List<OSFileStore> stores = fs.getFileStoreMatching(null, Collections.emptyMap(), true);
        assertThat("Should have at least one file store", stores.size(), is(greaterThan(0)));

        boolean foundRoot = false;
        for (OSFileStore store : stores) {
            if ("/".equals(store.getMount())) {
                foundRoot = true;
                boolean updated = store.updateAttributes();
                assertThat("updateAttributes should succeed for root", updated, is(true));
                assertThat(store.getTotalSpace(), is(greaterThan(0L)));
                assertThat(store.getUsableSpace(), is(greaterThanOrEqualTo(0L)));
                break;
            }
        }
        assertThat("Root mount should be present", foundRoot, is(true));
    }

    @Test
    void testUpdateAttributesWithZeroStatvfs() {
        ZeroStatvfsFileSystem fs = new ZeroStatvfsFileSystem();
        List<OSFileStore> stores = fs.getFileStoreMatching(null, Collections.emptyMap(), true);
        assertThat("Should have at least one file store", stores.size(), is(greaterThan(0)));

        boolean foundRoot = false;
        for (OSFileStore store : stores) {
            if ("/".equals(store.getMount())) {
                foundRoot = true;
                boolean updated = store.updateAttributes();
                assertThat("updateAttributes should succeed for root", updated, is(true));
                assertThat(store.getTotalSpace(), is(greaterThan(0L)));
                break;
            }
        }
        assertThat("Root mount should be present", foundRoot, is(true));
    }

    @Test
    void testFileStoreProperties() {
        NullStatvfsFileSystem fs = new NullStatvfsFileSystem();
        List<OSFileStore> stores = fs.getFileStoreMatching(null, Collections.emptyMap(), true);

        boolean foundRoot = false;
        for (OSFileStore store : stores) {
            if ("/".equals(store.getMount())) {
                foundRoot = true;
                assertThat(store.getName(), is(not(emptyString())));
                assertThat(store.getVolume(), is(not(emptyString())));
                assertThat(store.getType(), is(not(emptyString())));
                assertThat(store.getTotalSpace(), is(greaterThan(0L)));
                assertThat(store.getFreeSpace(), is(greaterThanOrEqualTo(0L)));
                assertThat(store.getUsableSpace(), is(greaterThanOrEqualTo(0L)));
                break;
            }
        }
        assertThat("Root mount should be present", foundRoot, is(true));
    }
}
