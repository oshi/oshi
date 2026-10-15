/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.software.os.OSFileStore;

class AbstractFileSystemTest {

    @Test
    void testGetFileStoresDelegatesToLocalOnly() {
        final List<String> calls = new ArrayList<>();
        AbstractFileSystem fs = new AbstractFileSystem() {
            @Override
            public List<OSFileStore> getFileStores(boolean localOnly) {
                calls.add("getFileStores(" + localOnly + ")");
                return Collections.emptyList();
            }

            @Override
            public long getOpenFileDescriptors() {
                return 0;
            }

            @Override
            public long getMaxFileDescriptors() {
                return 0;
            }

            @Override
            public long getMaxFileDescriptorsPerProcess() {
                return 0;
            }
        };
        fs.getFileStores();
        assertThat(calls, hasSize(1));
        assertThat(calls.get(0), is("getFileStores(false)"));
    }

    @Test
    void testCapAtSystemLimit() {
        assertThat(AbstractFileSystem.capAtSystemLimit(3404L, 10000L), is(3404L));
        // An unlimited resource limit is capped by the kernel's system-wide limit
        assertThat(AbstractFileSystem.capAtSystemLimit(Long.MAX_VALUE, 10000L), is(10000L));
        assertThat(AbstractFileSystem.capAtSystemLimit(0L, 10000L), is(10000L));
        assertThat(AbstractFileSystem.capAtSystemLimit(-1L, 10000L), is(10000L));
        assertThat(AbstractFileSystem.capAtSystemLimit(3404L, 0L), is(3404L));
        assertThat(AbstractFileSystem.capAtSystemLimit(-1L, 0L), is(0L));
    }
}
