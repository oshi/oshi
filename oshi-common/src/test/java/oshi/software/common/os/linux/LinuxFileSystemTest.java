/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.software.os.OSFileStore;

@EnabledOnOs(OS.LINUX)
class LinuxFileSystemTest {

    /** Concrete subclass that falls back to File methods for space queries. */
    private static final class StubLinuxFileSystem extends LinuxFileSystem {
        @Override
        protected long[] queryStatvfs(String path) {
            return null;
        }
    }

    @Test
    void testGetFileStoresReturnsRootMount() {
        StubLinuxFileSystem fs = new StubLinuxFileSystem();
        List<OSFileStore> stores = fs.getFileStoreMatching(null, Collections.emptyMap(), false);
        // Every Linux system has at least a root filesystem
        assertThat(stores, hasSize(greaterThan(0)));

        // Find root mount
        OSFileStore root = null;
        for (OSFileStore store : stores) {
            if ("/".equals(store.getMount())) {
                root = store;
                break;
            }
        }
        assertThat("Root mount should be present", root != null, is(true));
        assertThat(root.getMount(), is("/"));
        assertThat(root.getType(), is(not(emptyString())));
        assertThat(root.getTotalSpace(), is(greaterThan(0L)));
    }

    @Test
    void testGetFileStoresLocalOnly() {
        StubLinuxFileSystem fs = new StubLinuxFileSystem();
        List<OSFileStore> all = fs.getFileStoreMatching(null, Collections.emptyMap(), false);
        List<OSFileStore> local = fs.getFileStoreMatching(null, Collections.emptyMap(), true);
        // Local should be a subset of all
        assertThat(local.size(), is(not(greaterThan(all.size()))));
    }
}
