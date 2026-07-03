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
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
        assertNotNull(root, "Root mount should be present");
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

    @Test
    void testParseNfsAddrIPv4() {
        assertThat(LinuxFileSystem.parseNfsAddr("rw,addr=192.168.1.10"), is("192.168.1.10"));
    }

    @Test
    void testParseNfsAddrAtStart() {
        assertThat(LinuxFileSystem.parseNfsAddr("addr=10.0.0.1,rw"), is("10.0.0.1"));
    }

    @Test
    void testParseNfsAddrMountAddr() {
        assertThat(LinuxFileSystem.parseNfsAddr("rw,mountaddr=10.0.0.1,vers=3"), is("10.0.0.1"));
    }

    @Test
    void testParseNfsAddrIPv6() {
        assertThat(LinuxFileSystem.parseNfsAddr("rw,addr=2001:db8::1,vers=4"), is("2001:db8::1"));
    }

    @Test
    void testParseNfsAddrAbsent() {
        assertThat(LinuxFileSystem.parseNfsAddr("rw,hard,intr"), is(nullValue()));
    }

    @Test
    void testIsNfsType() {
        assertThat(LinuxFileSystem.isNfsType("nfs"), is(true));
        assertThat(LinuxFileSystem.isNfsType("nfs4"), is(true));
        assertThat(LinuxFileSystem.isNfsType("cifs"), is(false));
        assertThat(LinuxFileSystem.isNfsType("ext4"), is(false));
        assertThat(LinuxFileSystem.isNfsType("tmpfs"), is(false));
    }
}
