/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

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
}
