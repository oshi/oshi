/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static oshi.util.TestFileUtil.writeFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import oshi.hardware.NetworkIF.IfOperStatus;

class LinuxNetworkIFTest {

    // -------------------------------------------------------------------------
    // queryIfModelFromUdevData
    // -------------------------------------------------------------------------

    private static String model(Path tempDir, @Nullable String ifindex, @Nullable String udevRecord)
            throws IOException {
        Path sys = Files.createDirectories(tempDir.resolve("sys"));
        Path udev = Files.createDirectories(tempDir.resolve("udev"));
        if (ifindex != null) {
            Path eth0 = Files.createDirectories(sys.resolve("eth0"));
            writeFile(eth0.resolve("ifindex"), ifindex);
            // The kernel uevent file never carries the hwdb properties, so it must not be what is read
            writeFile(eth0.resolve("uevent"),
                    "INTERFACE=eth0\nIFINDEX=" + ifindex + "\nID_MODEL_FROM_DATABASE=Not From Uevent");
        }
        if (udevRecord != null) {
            writeFile(udev.resolve("n2"), udevRecord);
        }
        return LinuxNetworkIF.queryIfModelFromUdevData("eth0", sys + "/", udev + "/");
    }

    @Test
    void testQueryIfModelVendorAndModel(@TempDir Path tempDir) throws IOException {
        assertThat(model(tempDir, "2", "I:1700000000\nE:ID_NET_NAME_PATH=enp3s0\n"
                + "E:ID_VENDOR_FROM_DATABASE=Intel Corporation\nE:ID_MODEL_FROM_DATABASE=Ethernet Controller I225-V\n"
                + "G:systemd"), is("Intel Corporation Ethernet Controller I225-V"));
    }

    @Test
    void testQueryIfModelOnlyModel(@TempDir Path tempDir) throws IOException {
        assertThat(model(tempDir, "2", "E:ID_MODEL_FROM_DATABASE=RTL8111/8168/8411"), is("RTL8111/8168/8411"));
    }

    @Test
    void testQueryIfModelNoUdevRecord(@TempDir Path tempDir) throws IOException {
        // A host without udevd, such as a container: the interface name, never the uevent text
        assertThat(model(tempDir, "2", null), is("eth0"));
    }

    @Test
    void testQueryIfModelNoIfindex(@TempDir Path tempDir) throws IOException {
        assertThat(model(tempDir, null, "E:ID_MODEL_FROM_DATABASE=RTL8111/8168/8411"), is("eth0"));
    }

    @Test
    void testQueryIfModelRecordWithoutModel(@TempDir Path tempDir) throws IOException {
        assertThat(model(tempDir, "2", "E:ID_NET_DRIVER=e1000e"), is("eth0"));
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
