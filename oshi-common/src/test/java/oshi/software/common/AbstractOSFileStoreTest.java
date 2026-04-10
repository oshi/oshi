/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;

class AbstractOSFileStoreTest {

    private static AbstractOSFileStore createFileStore() {
        return new AbstractOSFileStore("TestFS", "/dev/sda1", "MyLabel", "/mnt/data", "rw,noatime", "1234-5678", true) {
            @Override
            public String getLogicalVolume() {
                return "lv0";
            }

            @Override
            public String getDescription() {
                return "Local Disk";
            }

            @Override
            public String getType() {
                return "ext4";
            }

            @Override
            public long getFreeSpace() {
                return 1000L;
            }

            @Override
            public long getUsableSpace() {
                return 900L;
            }

            @Override
            public long getTotalSpace() {
                return 5000L;
            }

            @Override
            public long getFreeInodes() {
                return 100L;
            }

            @Override
            public long getTotalInodes() {
                return 500L;
            }

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
    }

    @Test
    void testDefaultConstructorNulls() {
        AbstractOSFileStore fs = new AbstractOSFileStore() {
            @Override
            public String getLogicalVolume() {
                return "";
            }

            @Override
            public String getDescription() {
                return "";
            }

            @Override
            public String getType() {
                return "";
            }

            @Override
            public long getFreeSpace() {
                return 0;
            }

            @Override
            public long getUsableSpace() {
                return 0;
            }

            @Override
            public long getTotalSpace() {
                return 0;
            }

            @Override
            public long getFreeInodes() {
                return 0;
            }

            @Override
            public long getTotalInodes() {
                return 0;
            }

            @Override
            public boolean updateAttributes() {
                return false;
            }
        };
        assertThat(fs.getName(), is(nullValue()));
        assertThat(fs.isLocal(), is(false));
    }

    @Test
    void testToString() {
        String s = createFileStore().toString();
        assertThat(s, containsString("name=TestFS"));
        assertThat(s, containsString("ext4"));
        assertThat(s, containsString("lv0"));
    }
}
