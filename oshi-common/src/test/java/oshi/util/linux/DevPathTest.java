/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

// Loading DevPath validates that the configured /dev path exists, which fails on Windows
@DisabledOnOs(OS.WINDOWS)
class DevPathTest {

    private static final String HOST_DEV = "/host/dev/";

    @Test
    void testNamesUseTheNodeRoot() {
        assertThat(DevPath.NODE_ROOT, is("/dev/"));
        assertThat(DevPath.DM, is("/dev/dm"));
        assertThat(DevPath.LOOP, is("/dev/loop"));
        assertThat(DevPath.MAPPER, is("/dev/mapper/"));
        assertThat(DevPath.RAM, is("/dev/ram"));
    }

    @Test
    void testCustomRootToNodeName() {
        assertThat(DevPath.reroot("/host/dev/sda1", HOST_DEV, DevPath.NODE_ROOT), is("/dev/sda1"));
        assertThat(DevPath.reroot("/host/dev/mapper/vg-root", HOST_DEV, DevPath.NODE_ROOT), is("/dev/mapper/vg-root"));
    }

    @Test
    void testNodeNameToCustomRoot() {
        assertThat(DevPath.reroot("/dev/mapper/vg-root", DevPath.NODE_ROOT, HOST_DEV), is("/host/dev/mapper/vg-root"));
    }

    @Test
    void testPathOutsideTheRootIsUnchanged() {
        assertThat(DevPath.reroot("tmpfs", HOST_DEV, DevPath.NODE_ROOT), is("tmpfs"));
        assertThat(DevPath.reroot("/device/sda", DevPath.NODE_ROOT, HOST_DEV), is("/device/sda"));
        assertThat(DevPath.reroot("/dev/sda1", HOST_DEV, DevPath.NODE_ROOT), is("/dev/sda1"));
    }

    @Test
    void testConfiguredRootRoundTrip() {
        assertThat(DevPath.toFilePath("/dev/sda1"), is(DevPath.DEV + "sda1"));
        assertThat(DevPath.toNodeName(DevPath.DEV + "sda1"), is("/dev/sda1"));
    }
}
