/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import oshi.hardware.BluetoothDevice;
import oshi.hardware.common.AbstractBluetoothDevice;

@EnabledOnOs(OS.LINUX)
class LinuxBluetoothDeviceTest {

    @Test
    void testNonexistentSysPath(@TempDir Path tempDir) {
        List<BluetoothDevice> devices = LinuxBluetoothDevice
                .queryBluetoothDevices(tempDir.resolve("missing").toString(), tempDir.resolve("varlib").toString());
        assertThat(devices, is(empty()));
    }

    @Test
    void testEmptyAdapterDir(@TempDir Path tempDir) throws IOException {
        Path sysDir = tempDir.resolve("sys/class/bluetooth");
        Files.createDirectories(sysDir);
        List<BluetoothDevice> devices = LinuxBluetoothDevice.queryBluetoothDevices(sysDir.toString(),
                tempDir.resolve("varlib").toString());
        assertThat(devices, is(empty()));
    }

    @Test
    void testAdapterWithNoAddress(@TempDir Path tempDir) throws IOException {
        Path sysDir = tempDir.resolve("sys/class/bluetooth");
        Path hci0 = sysDir.resolve("hci0");
        Files.createDirectories(hci0);
        List<BluetoothDevice> devices = LinuxBluetoothDevice.queryBluetoothDevices(sysDir.toString(),
                tempDir.resolve("varlib").toString());
        assertThat(devices, is(empty()));
    }

    @Test
    void testAdapterWithNoVarLibDir(@TempDir Path tempDir) throws IOException {
        Path sysDir = tempDir.resolve("sys/class/bluetooth");
        Path hci0 = sysDir.resolve("hci0");
        Files.createDirectories(hci0);
        writeFile(hci0.resolve("address"), "AA:BB:CC:DD:EE:FF");

        List<BluetoothDevice> devices = LinuxBluetoothDevice.queryBluetoothDevices(sysDir.toString(),
                tempDir.resolve("varlib").toString() + "/");
        assertThat(devices, is(empty()));
    }

    @Test
    void testSinglePairedDevice(@TempDir Path tempDir) throws IOException {
        Path sysDir = tempDir.resolve("sys/class/bluetooth");
        Path hci0 = sysDir.resolve("hci0");
        Files.createDirectories(hci0);
        writeFile(hci0.resolve("address"), "AA:BB:CC:DD:EE:FF");

        Path varLib = tempDir.resolve("varlib");
        Path deviceDir = varLib.resolve("AA:BB:CC:DD:EE:FF/11:22:33:44:55:66");
        Files.createDirectories(deviceDir);
        writeFile(deviceDir.resolve("info"),
                "[General]\nName=My Headphones\nPaired=true\nConnected=true\nClass=0x240404\n");

        List<BluetoothDevice> devices = LinuxBluetoothDevice.queryBluetoothDevices(sysDir.toString(),
                varLib.toString() + "/");
        assertThat(devices, hasSize(1));
        BluetoothDevice dev = devices.get(0);
        assertThat(dev.getName(), is("My Headphones"));
        assertThat(dev.getAddress(), is("11:22:33:44:55:66"));
        assertThat(dev.getMajorDeviceClass(), is("Audio/Video"));
        assertThat(dev.isConnected(), is(true));
        assertThat(dev.isPaired(), is(true));
        assertThat(dev.getBatteryLevel(), is(-1));
        assertThat(dev.getAdapterName(), is("hci0"));
    }

    @Test
    void testDeviceWithBattery(@TempDir Path tempDir) throws IOException {
        Path sysDir = tempDir.resolve("sys/class/bluetooth");
        Path hci0 = sysDir.resolve("hci0");
        Files.createDirectories(hci0);
        writeFile(hci0.resolve("address"), "aa:bb:cc:dd:ee:ff");

        Path varLib = tempDir.resolve("varlib");
        // Lowercase adapter address in sysfs, uppercase in varlib
        Path deviceDir = varLib.resolve("AA:BB:CC:DD:EE:FF/11:22:33:44:55:66");
        Files.createDirectories(deviceDir);
        writeFile(deviceDir.resolve("info"),
                "[General]\nName=BT Mouse\nPaired=true\nConnected=false\nClass=0x002580\nBattery=75\n");

        List<BluetoothDevice> devices = LinuxBluetoothDevice.queryBluetoothDevices(sysDir.toString(),
                varLib.toString() + "/");
        assertThat(devices, hasSize(1));
        BluetoothDevice dev = devices.get(0);
        assertThat(dev.getName(), is("BT Mouse"));
        assertThat(dev.getMajorDeviceClass(), is("Peripheral"));
        assertThat(dev.isConnected(), is(false));
        assertThat(dev.getBatteryLevel(), is(75));
    }

    @Test
    void testNonDeviceDirectoriesSkipped(@TempDir Path tempDir) throws IOException {
        Path sysDir = tempDir.resolve("sys/class/bluetooth");
        Path hci0 = sysDir.resolve("hci0");
        Files.createDirectories(hci0);
        writeFile(hci0.resolve("address"), "AA:BB:CC:DD:EE:FF");

        Path varLib = tempDir.resolve("varlib");
        Path adapterDir = varLib.resolve("AA:BB:CC:DD:EE:FF");
        // Non-MAC directory should be skipped
        Files.createDirectories(adapterDir.resolve("cache"));
        // Short name should be skipped
        Files.createDirectories(adapterDir.resolve("AB:CD"));
        // No info file should be skipped
        Files.createDirectories(adapterDir.resolve("11:22:33:44:55:66"));

        List<BluetoothDevice> devices = LinuxBluetoothDevice.queryBluetoothDevices(sysDir.toString(),
                varLib.toString() + "/");
        assertThat(devices, is(empty()));
    }

    @Test
    void testParseMajorDeviceClass() {
        assertThat(AbstractBluetoothDevice.parseMajorDeviceClass(0), is("Miscellaneous"));
        assertThat(AbstractBluetoothDevice.parseMajorDeviceClass(0x000100), is("Computer"));
        assertThat(AbstractBluetoothDevice.parseMajorDeviceClass(0x000200), is("Phone"));
        assertThat(AbstractBluetoothDevice.parseMajorDeviceClass(0x000300), is("Networking"));
        assertThat(AbstractBluetoothDevice.parseMajorDeviceClass(0x000400), is("Audio/Video"));
        assertThat(AbstractBluetoothDevice.parseMajorDeviceClass(0x000500), is("Peripheral"));
        assertThat(AbstractBluetoothDevice.parseMajorDeviceClass(0x000600), is("Imaging"));
        assertThat(AbstractBluetoothDevice.parseMajorDeviceClass(0x000700), is("Wearable"));
        assertThat(AbstractBluetoothDevice.parseMajorDeviceClass(0x000800), is("Toy"));
        assertThat(AbstractBluetoothDevice.parseMajorDeviceClass(0x000900), is("Health"));
        assertThat(AbstractBluetoothDevice.parseMajorDeviceClass(0x001F00), is("Uncategorized"));
        assertThat(AbstractBluetoothDevice.parseMajorDeviceClass(0x000A00), is(""));
    }

    @Test
    void testLowercaseVarLibFallback(@TempDir Path tempDir) throws IOException {
        Path sysDir = tempDir.resolve("sys/class/bluetooth");
        Path hci0 = sysDir.resolve("hci0");
        Files.createDirectories(hci0);
        writeFile(hci0.resolve("address"), "aa:bb:cc:dd:ee:ff");

        Path varLib = tempDir.resolve("varlib");
        // Use lowercase path (fallback case); no explicit Paired key means paired=true
        Path deviceDir = varLib.resolve("aa:bb:cc:dd:ee:ff/11:22:33:44:55:66");
        Files.createDirectories(deviceDir);
        writeFile(deviceDir.resolve("info"), "[General]\nName=Speaker\n");

        List<BluetoothDevice> devices = LinuxBluetoothDevice.queryBluetoothDevices(sysDir.toString(),
                varLib.toString() + "/");
        assertThat(devices, hasSize(1));
        assertThat(devices.get(0).getName(), is("Speaker"));
        assertThat(devices.get(0).isPaired(), is(true));
    }

    private static void writeFile(Path path, String content) throws IOException {
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }
}
