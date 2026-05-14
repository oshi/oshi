/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static oshi.util.TestFileUtil.writeFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

@EnabledOnOs(OS.LINUX)
class DrmEdidTest {

    // Minimal valid 128-byte EDID with standard header
    private static final byte[] VALID_EDID = createValidEdid();

    private static byte[] createValidEdid() {
        byte[] edid = new byte[128];
        // EDID magic header: 00 FF FF FF FF FF FF 00
        edid[0] = 0x00;
        edid[1] = (byte) 0xFF;
        edid[2] = (byte) 0xFF;
        edid[3] = (byte) 0xFF;
        edid[4] = (byte) 0xFF;
        edid[5] = (byte) 0xFF;
        edid[6] = (byte) 0xFF;
        edid[7] = 0x00;
        return edid;
    }

    @Test
    void testConnectedDisplayWithEdid(@TempDir Path tempDir) throws IOException {
        Path connector = Files.createDirectories(tempDir.resolve("card0-HDMI-1"));
        writeFile(connector.resolve("status"), "connected\n");
        Files.write(connector.resolve("edid"), VALID_EDID);

        List<byte[]> edids = DrmEdid.getEdidArrays(tempDir.toFile());
        assertThat(edids, hasSize(1));
        assertThat(edids.get(0).length, greaterThanOrEqualTo(128));
        assertThat(edids.get(0)[0], is((byte) 0x00));
        assertThat(edids.get(0)[1], is((byte) 0xFF));
    }

    @Test
    void testDisconnectedDisplaySkipped(@TempDir Path tempDir) throws IOException {
        Path connector = Files.createDirectories(tempDir.resolve("card0-DP-1"));
        writeFile(connector.resolve("status"), "disconnected\n");
        Files.write(connector.resolve("edid"), VALID_EDID);

        assertThat(DrmEdid.getEdidArrays(tempDir.toFile()), is(empty()));
    }

    @Test
    void testEmptyEdidSkipped(@TempDir Path tempDir) throws IOException {
        Path connector = Files.createDirectories(tempDir.resolve("card0-HDMI-1"));
        writeFile(connector.resolve("status"), "connected\n");
        Files.write(connector.resolve("edid"), new byte[0]);

        assertThat(DrmEdid.getEdidArrays(tempDir.toFile()), is(empty()));
    }

    @Test
    void testMultipleConnectors(@TempDir Path tempDir) throws IOException {
        Path hdmi = Files.createDirectories(tempDir.resolve("card0-HDMI-1"));
        writeFile(hdmi.resolve("status"), "connected\n");
        Files.write(hdmi.resolve("edid"), VALID_EDID);

        Path dp = Files.createDirectories(tempDir.resolve("card0-DP-1"));
        writeFile(dp.resolve("status"), "connected\n");
        Files.write(dp.resolve("edid"), VALID_EDID);

        Path disconnected = Files.createDirectories(tempDir.resolve("card0-DP-2"));
        writeFile(disconnected.resolve("status"), "disconnected\n");
        Files.write(disconnected.resolve("edid"), VALID_EDID);

        assertThat(DrmEdid.getEdidArrays(tempDir.toFile()), hasSize(2));
    }

    @Test
    void testNonExistentDirectory(@TempDir Path tempDir) {
        assertThat(DrmEdid.getEdidArrays(tempDir.resolve("nonexistent").toFile()), is(empty()));
    }

    @Test
    void testIgnoresNonConnectorDirectories(@TempDir Path tempDir) throws IOException {
        // "card0" alone (no dash-connector suffix) should be ignored
        Path card = Files.createDirectories(tempDir.resolve("card0"));
        writeFile(card.resolve("status"), "connected\n");
        Files.write(card.resolve("edid"), VALID_EDID);

        assertThat(DrmEdid.getEdidArrays(tempDir.toFile()), is(empty()));
    }

    @Test
    void testMissingStatusFileStillReadsEdid(@TempDir Path tempDir) throws IOException {
        Path connector = Files.createDirectories(tempDir.resolve("card0-HDMI-1"));
        // No status file created - should still read EDID
        Files.write(connector.resolve("edid"), VALID_EDID);

        List<byte[]> edids = DrmEdid.getEdidArrays(tempDir.toFile());
        assertThat(edids, hasSize(1));
        assertThat(edids.get(0).length, greaterThanOrEqualTo(128));
    }
}
