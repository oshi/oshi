/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux.nativefree;

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

import oshi.hardware.UsbDevice;

@EnabledOnOs(OS.LINUX)
class LinuxUsbDeviceNFTest {

    @Test
    void testQueryUsbDevicesNonexistentPath(@TempDir Path tempDir) {
        List<UsbDevice> devices = LinuxUsbDeviceNF.queryUsbDevices(tempDir.resolve("missing").toString() + "/");
        assertThat(devices, is(empty()));
    }

    @Test
    void testQueryUsbDevicesEmptyDir(@TempDir Path tempDir) throws IOException {
        Path usbDir = tempDir.resolve("usb");
        Files.createDirectories(usbDir);
        List<UsbDevice> devices = LinuxUsbDeviceNF.queryUsbDevices(usbDir.toString() + "/");
        assertThat(devices, is(empty()));
    }

    @Test
    void testQueryUsbDevicesNoDevnum(@TempDir Path tempDir) throws IOException {
        Path usbDir = tempDir.resolve("usb");
        Path dev = usbDir.resolve("usb1");
        Files.createDirectories(dev);
        List<UsbDevice> devices = LinuxUsbDeviceNF.queryUsbDevices(usbDir.toString() + "/");
        assertThat(devices, is(empty()));
    }

    @Test
    void testQueryUsbDevicesSingleController(@TempDir Path tempDir) throws IOException {
        Path usbDir = tempDir.resolve("usb");
        Path usb1 = usbDir.resolve("usb1");
        Files.createDirectories(usb1);
        writeFile(usb1.resolve("devnum"), "1");
        writeFile(usb1.resolve("product"), "xHCI Host Controller");
        writeFile(usb1.resolve("manufacturer"), "Linux Foundation");
        writeFile(usb1.resolve("idVendor"), "1d6b");
        writeFile(usb1.resolve("idProduct"), "0003");
        writeFile(usb1.resolve("serial"), "0000:00:14.0");

        List<UsbDevice> devices = LinuxUsbDeviceNF.queryUsbDevices(usbDir.toString() + "/");
        assertThat(devices, hasSize(1));
        assertThat(devices.get(0).getName(), is("xHCI Host Controller"));
        assertThat(devices.get(0).getVendor(), is("Linux Foundation"));
        assertThat(devices.get(0).getVendorId(), is("1d6b"));
        assertThat(devices.get(0).getProductId(), is("0003"));
        assertThat(devices.get(0).getSerialNumber(), is("0000:00:14.0"));
    }

    @Test
    void testQueryUsbDevicesWithChild(@TempDir Path tempDir) throws IOException {
        Path usbDir = tempDir.resolve("usb");

        Path usb1 = usbDir.resolve("usb1");
        Files.createDirectories(usb1);
        writeFile(usb1.resolve("devnum"), "1");
        writeFile(usb1.resolve("product"), "Root Hub");
        writeFile(usb1.resolve("idVendor"), "1d6b");
        writeFile(usb1.resolve("idProduct"), "0002");

        Path child = usbDir.resolve("1-1");
        Files.createDirectories(child);
        writeFile(child.resolve("devnum"), "2");
        writeFile(child.resolve("product"), "USB Mouse");
        writeFile(child.resolve("manufacturer"), "Logitech");
        writeFile(child.resolve("idVendor"), "046d");
        writeFile(child.resolve("idProduct"), "c077");

        List<UsbDevice> devices = LinuxUsbDeviceNF.queryUsbDevices(usbDir.toString() + "/");
        assertThat(devices, hasSize(1));
        assertThat(devices.get(0).getConnectedDevices(), hasSize(1));
        assertThat(devices.get(0).getConnectedDevices().get(0).getName(), is("USB Mouse"));
    }

    @Test
    void testQueryUsbDevicesWithNestedChild(@TempDir Path tempDir) throws IOException {
        Path usbDir = tempDir.resolve("usb");

        Path usb1 = usbDir.resolve("usb1");
        Files.createDirectories(usb1);
        writeFile(usb1.resolve("devnum"), "1");
        writeFile(usb1.resolve("idVendor"), "1d6b");
        writeFile(usb1.resolve("idProduct"), "0002");

        Path hub = usbDir.resolve("1-1");
        Files.createDirectories(hub);
        writeFile(hub.resolve("devnum"), "2");
        writeFile(hub.resolve("product"), "USB Hub");
        writeFile(hub.resolve("idVendor"), "0424");
        writeFile(hub.resolve("idProduct"), "2514");

        Path device = usbDir.resolve("1-1.2");
        Files.createDirectories(device);
        writeFile(device.resolve("devnum"), "3");
        writeFile(device.resolve("product"), "Keyboard");
        writeFile(device.resolve("idVendor"), "04f2");
        writeFile(device.resolve("idProduct"), "0112");

        List<UsbDevice> devices = LinuxUsbDeviceNF.queryUsbDevices(usbDir.toString() + "/");
        assertThat(devices, hasSize(1));
        UsbDevice root = devices.get(0);
        assertThat(root.getConnectedDevices(), hasSize(1));
        UsbDevice hubDev = root.getConnectedDevices().get(0);
        assertThat(hubDev.getName(), is("USB Hub"));
        assertThat(hubDev.getConnectedDevices(), hasSize(1));
        assertThat(hubDev.getConnectedDevices().get(0).getName(), is("Keyboard"));
    }

    private static void writeFile(Path path, String content) throws IOException {
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }
}
