/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import oshi.hardware.HWDiskStore.StoreKind;

class HWDiskStoreTest {

    @Test
    void testStoreKindEntry() {
        assertThat(StoreKind.DISK.entry("/dev/sda"), is("disk:/dev/sda"));
        assertThat(StoreKind.PARTITION.entry("disk0s2"), is("partition:disk0s2"));
        assertThat(StoreKind.FILE.entry("/System/Volumes/Data"), is("file:/System/Volumes/Data"));
    }

    @Test
    void testStoreKindMatches() {
        assertThat(StoreKind.DISK.matches("disk:/dev/sda"), is(true));
        assertThat(StoreKind.PARTITION.matches("disk:/dev/sda"), is(false));
        assertThat(StoreKind.FILE.matches("disk:/dev/sda"), is(false));
        // The name of a macOS disk starts with the kind's own word, without the colon
        assertThat(StoreKind.DISK.matches("disk0s2"), is(false));
        assertThat(StoreKind.DISK.matches(""), is(false));
    }

    @Test
    void testStoreKindNameOf() {
        assertThat(StoreKind.PARTITION.nameOf("partition:/dev/sda2"), is("/dev/sda2"));
        assertThat(StoreKind.DISK.nameOf("partition:/dev/sda2"), is(""));
        // Only the first colon separates the kind; Windows names keep theirs
        assertThat(StoreKind.FILE.nameOf("file:C:\\"), is("C:\\"));
        assertThat(StoreKind.DISK.nameOf(StoreKind.DISK.entry("\\\\.\\PHYSICALDRIVE1")), is("\\\\.\\PHYSICALDRIVE1"));
    }

    @Test
    void testStoreKindPrefixes() {
        for (StoreKind kind : StoreKind.values()) {
            assertThat(kind.getPrefix().endsWith(":"), is(true));
            assertThat(kind.nameOf(kind.entry("x:y")), is("x:y"));
        }
    }
}
