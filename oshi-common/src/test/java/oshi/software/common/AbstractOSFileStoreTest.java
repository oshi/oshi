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

    private static AbstractOSFileStore createFileStore() {
        return new AbstractOSFileStore("TestFS", "/dev/sda1", "MyLabel", "/mnt/data", "rw,noatime", "1234-5678", true,
                "lv0", "Local Disk", "ext4", 1000L, 900L, 5000L, 100L, 500L) {
            @Override
            public boolean updateAttributes() {
                return false;
            }
        };
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
}
