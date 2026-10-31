/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * Tests the shared macOS disk classification logic without a Mac. The IOKit traversal that fetches the medium type
 * cannot run in a unit test, so the classification is separated from the acquisition and tested here.
 */
class MacHWDiskStoreTest {

    @Test
    void testParseMediumTypeSolidState() {
        // The spelling IOKit actually publishes for flash media
        assertThat(MacHWDiskStore.parseMediumType("Solid State"), is("SSD"));
        assertThat(MacHWDiskStore.parseMediumType("SSD"), is("SSD"));
    }

    @Test
    void testParseMediumTypeRotational() {
        assertThat(MacHWDiskStore.parseMediumType("Rotational"), is("HDD"));
    }

    @Test
    void testParseMediumTypeMatchesSubstring() {
        // The property is matched by substring, so surrounding text must not defeat it
        assertThat(MacHWDiskStore.parseMediumType("Apple Solid State Media"), is("SSD"));
        assertThat(MacHWDiskStore.parseMediumType("Rotational Media"), is("HDD"));
    }

    @Test
    void testParseMediumTypeAbsent() {
        // A null medium type is the normal result for a device that does not publish the characteristic
        assertThat(MacHWDiskStore.parseMediumType(null), is("Unknown"));
    }

    @Test
    void testParseMediumTypeUnrecognized() {
        assertThat(MacHWDiskStore.parseMediumType(""), is("Unknown"));
        assertThat(MacHWDiskStore.parseMediumType("Optical"), is("Unknown"));
    }

    @Test
    void testParseMediumTypeIsCaseSensitive() {
        // IOKit publishes these in title case; document that no case folding is applied
        assertThat(MacHWDiskStore.parseMediumType("solid state"), is("Unknown"));
        assertThat(MacHWDiskStore.parseMediumType("rotational"), is("Unknown"));
    }

    @Test
    void testImagePath() {
        // A Time Machine sparse bundle on an SMB share, as IOKit publishes it
        assertThat(
                MacHWDiskStore.imagePath(
                        "file:///Volumes/.timemachine/host/Time%20Machine/MacBook%20Pro%20(2).sparsebundle/"),
                is("/Volumes/.timemachine/host/Time Machine/MacBook Pro (2).sparsebundle/"));
        assertThat(MacHWDiskStore.imagePath("https://example.com/a.dmg"), is(""));
        assertThat(MacHWDiskStore.imagePath("not a url"), is(""));
    }

    @Test
    void testMediaBackingStore() {
        assertThat(MacHWDiskStore.mediaBackingStore("disk0s2", false),
                is(Optional.of(Collections.singletonList("partition:disk0s2"))));
        assertThat(MacHWDiskStore.mediaBackingStore("disk4", true),
                is(Optional.of(Collections.singletonList("disk:disk4"))));
        assertThat(MacHWDiskStore.mediaBackingStore("disk0s2", null),
                is(Optional.of(Collections.singletonList("partition:disk0s2"))));
        assertThat(MacHWDiskStore.mediaBackingStore(null, true), is(Optional.empty()));
    }

    @Test
    void testImageBackingStore() {
        assertThat(MacHWDiskStore.imageBackingStore("file:///Library/a.dmg", path -> "/System/Volumes/Data"),
                is(Optional.of(Collections.singletonList("file:/System/Volumes/Data"))));
        // A file store that cannot be read, such as a protected Time Machine share, is not reported
        assertThat(MacHWDiskStore.imageBackingStore("file:///Library/a.dmg", path -> ""), is(Optional.empty()));
        assertThat(MacHWDiskStore.imageBackingStore("https://example.com/a.dmg", path -> "/"), is(Optional.empty()));
    }

    @Test
    void testLayeredDiskIsVirtual() {
        assertThat(disk("SSD", Optional.of(Collections.singletonList("partition:disk0s2"))).getDiskType(),
                is("Virtual"));
        assertThat(disk("SSD", Optional.of(Collections.emptyList())).getDiskType(), is("SSD"));
        assertThat(disk("Removable", Optional.empty()).getDiskType(), is("Removable"));
        assertThat(disk("SSD", Optional.empty()).getBackingStores(), is(Optional.empty()));
    }

    private static MacHWDiskStore disk(String diskType, Optional<List<String>> backingStores) {
        return new MacHWDiskStore("disk3", "model", "serial", 1L, diskType, backingStores) {
            @Override
            public boolean updateAttributes() {
                return false;
            }
        };
    }
}
