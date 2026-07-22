/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.hardware.BluetoothDevice;

class MacBluetoothDeviceTest {

    // Representative `system_profiler SPBluetoothDataType` output (real structure, sanitized names/addresses): a
    // controller block (ignored, not in a Connected/Not Connected section), then Connected and Not Connected device
    // sections. Devices carry ignored fields (Vendor ID, Product ID, Services) plus the parsed Address/Minor Type/
    // Battery Level.
    private static final List<String> SPBLUETOOTH = Arrays.asList(//
            "Bluetooth:", //
            "", //
            "      Bluetooth Controller:", //
            "          Address: 5C:E9:1E:83:67:4F", //
            "          State: On", //
            "          Chipset: BCM_4388", //
            "          Vendor ID: 0x004C (Apple)", //
            "      Connected:", //
            "          Wireless Keyboard:", //
            "              Address: 11:22:33:aa:bb:cc", //
            "              Vendor ID: 0x046D", //
            "              Product ID: 0xB359", //
            "              Minor Type: Keyboard", //
            "              Battery Level: 80%", //
            "              Services: 0x400000 < BLE >", //
            "          Wireless Headphones:", //
            "              Address: 22:33:44:55:66:77", //
            "              Minor Type: Headphones", //
            "      Not Connected:", //
            "          Wireless Mouse:", //
            "              Address: 33:44:55:66:77:88", //
            "              Major Type: Peripheral");

    @Test
    void testParseSystemProfiler() {
        List<BluetoothDevice> devices = MacBluetoothDevice.parseSystemProfiler(SPBLUETOOTH);
        assertThat(devices, hasSize(3));

        BluetoothDevice keyboard = devices.get(0);
        assertThat(keyboard.getName(), is("Wireless Keyboard"));
        // Address is upper-cased
        assertThat(keyboard.getAddress(), is("11:22:33:AA:BB:CC"));
        // Minor Type sets the majorDeviceClass field; ignored fields (Vendor/Product ID, Services) don't disturb it
        assertThat(keyboard.getMajorDeviceClass(), is("Keyboard"));
        assertThat(keyboard.isConnected(), is(true));
        assertThat(keyboard.isPaired(), is(true));
        assertThat(keyboard.getBatteryLevel(), is(80));

        BluetoothDevice headphones = devices.get(1);
        assertThat(headphones.getName(), is("Wireless Headphones"));
        assertThat(headphones.isConnected(), is(true));
        // No battery level line -> sentinel -1
        assertThat(headphones.getBatteryLevel(), is(-1));

        BluetoothDevice mouse = devices.get(2);
        assertThat(mouse.getName(), is("Wireless Mouse"));
        // In the Not Connected section
        assertThat(mouse.isConnected(), is(false));
        assertThat(mouse.getMajorDeviceClass(), is("Peripheral"));
    }

    @Test
    void testParseSystemProfilerNoDevices() {
        // Controller present but no Connected/Not Connected sections -> no devices
        assertThat(MacBluetoothDevice.parseSystemProfiler(Arrays.asList("Bluetooth:", "      Bluetooth Controller:",
                "          Address: AA:BB:CC:DD:EE:FF", "          State: On")), is(empty()));
        assertThat(MacBluetoothDevice.parseSystemProfiler(Collections.emptyList()), is(empty()));
    }
}
