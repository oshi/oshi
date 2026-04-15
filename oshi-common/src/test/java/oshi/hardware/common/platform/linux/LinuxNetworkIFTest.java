/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static oshi.hardware.common.platform.linux.TestFileUtil.writeFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import oshi.hardware.NetworkIF.IfOperStatus;

class LinuxNetworkIFTest {

    // -------------------------------------------------------------------------
    // queryIfModelFromSysfs
    // -------------------------------------------------------------------------

    @Test
    void testQueryIfModelVendorAndModel(@TempDir Path tempDir) throws IOException {
        Path eth0 = tempDir.resolve("eth0");
        Files.createDirectories(eth0);
        writeFile(eth0.resolve("uevent"),
                "ID_VENDOR_FROM_DATABASE=Intel Corporation\nID_MODEL_FROM_DATABASE=Ethernet Controller I225-V");

        String model = LinuxNetworkIF.queryIfModelFromSysfs("eth0", tempDir.toString() + "/");
        assertThat(model, is("Intel Corporation Ethernet Controller I225-V"));
    }

    @Test
    void testQueryIfModelOnlyModel(@TempDir Path tempDir) throws IOException {
        Path eth0 = tempDir.resolve("eth0");
        Files.createDirectories(eth0);
        writeFile(eth0.resolve("uevent"), "ID_MODEL_FROM_DATABASE=RTL8111/8168/8411");

        String model = LinuxNetworkIF.queryIfModelFromSysfs("eth0", tempDir.toString() + "/");
        assertThat(model, is("RTL8111/8168/8411"));
    }

    @Test
    void testQueryIfModelNoUevent(@TempDir Path tempDir) {
        // No uevent file — should return the interface name
        String model = LinuxNetworkIF.queryIfModelFromSysfs("eth0", tempDir.toString() + "/");
        assertThat(model, is("eth0"));
    }

    @Test
    void testQueryIfModelEmptyUevent(@TempDir Path tempDir) throws IOException {
        Path eth0 = tempDir.resolve("eth0");
        Files.createDirectories(eth0);
        writeFile(eth0.resolve("uevent"), "DRIVER=e1000e");

        String model = LinuxNetworkIF.queryIfModelFromSysfs("eth0", tempDir.toString() + "/");
        assertThat(model, is("eth0"));
    }

    // -------------------------------------------------------------------------
    // parseIfOperStatus — all cases
    // -------------------------------------------------------------------------

    @Test
    void testParseIfOperStatusUp() {
        assertThat(LinuxNetworkIF.parseIfOperStatus("up"), is(IfOperStatus.UP));
    }

    @Test
    void testParseIfOperStatusDown() {
        assertThat(LinuxNetworkIF.parseIfOperStatus("down"), is(IfOperStatus.DOWN));
    }

    @Test
    void testParseIfOperStatusTesting() {
        assertThat(LinuxNetworkIF.parseIfOperStatus("testing"), is(IfOperStatus.TESTING));
    }

    @Test
    void testParseIfOperStatusDormant() {
        assertThat(LinuxNetworkIF.parseIfOperStatus("dormant"), is(IfOperStatus.DORMANT));
    }

    @Test
    void testParseIfOperStatusNotPresent() {
        assertThat(LinuxNetworkIF.parseIfOperStatus("notpresent"), is(IfOperStatus.NOT_PRESENT));
    }

    @Test
    void testParseIfOperStatusLowerLayerDown() {
        assertThat(LinuxNetworkIF.parseIfOperStatus("lowerlayerdown"), is(IfOperStatus.LOWER_LAYER_DOWN));
    }

    @Test
    void testParseIfOperStatusUnknown() {
        assertThat(LinuxNetworkIF.parseIfOperStatus("unknown"), is(IfOperStatus.UNKNOWN));
    }

    @Test
    void testParseIfOperStatusUnrecognized() {
        assertThat(LinuxNetworkIF.parseIfOperStatus("somethingelse"), is(IfOperStatus.UNKNOWN));
    }
}
