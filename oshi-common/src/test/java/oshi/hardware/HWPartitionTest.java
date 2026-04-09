/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

class HWPartitionTest {

    @Test
    void testConstructorWithoutLabel() {
        HWPartition p = new HWPartition("sda1", "EFI", "vfat", "uuid-1", 512_000_000L, 8, 1, "/boot/efi");
        assertThat(p.getIdentification(), is("sda1"));
        assertThat(p.getName(), is("EFI"));
        assertThat(p.getType(), is("vfat"));
        assertThat(p.getUuid(), is("uuid-1"));
        assertThat(p.getLabel(), is(""));
        assertThat(p.getSize(), is(512_000_000L));
        assertThat(p.getMajor(), is(8));
        assertThat(p.getMinor(), is(1));
        assertThat(p.getMountPoint(), is("/boot/efi"));
    }

    @Test
    void testConstructorWithLabel() {
        HWPartition p = new HWPartition("sda2", "Root", "ext4", "uuid-2", "MyLabel", 10_000_000_000L, 8, 2, "/");
        assertThat(p.getLabel(), is("MyLabel"));
    }

    @Test
    void testToString() {
        HWPartition p = new HWPartition("sda1", "EFI", "vfat", "uuid-1", "EFI", 512_000_000L, 8, 1, "/boot/efi");
        String s = p.toString();
        assertThat(s, containsString("sda1"));
        assertThat(s, containsString("vfat"));
        assertThat(s, containsString("8:1"));
    }
}
