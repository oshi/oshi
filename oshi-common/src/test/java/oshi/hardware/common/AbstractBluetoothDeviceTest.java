/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

class AbstractBluetoothDeviceTest {

    @Test
    void testGetters() {
        AbstractBluetoothDevice device = new AbstractBluetoothDevice("Headphones", "AA:BB:CC:DD:EE:FF", "Audio/Video",
                true, true, 85, "hci0") {
        };
        assertThat(device.getName(), is("Headphones"));
        assertThat(device.getAddress(), is("AA:BB:CC:DD:EE:FF"));
        assertThat(device.getMajorDeviceClass(), is("Audio/Video"));
        assertThat(device.isConnected(), is(true));
        assertThat(device.isPaired(), is(true));
        assertThat(device.getBatteryLevel(), is(85));
        assertThat(device.getAdapterName(), is("hci0"));
    }

    @Test
    void testBatteryLevelNormalization() {
        AbstractBluetoothDevice overMax = new AbstractBluetoothDevice("A", "", "", false, false, 150, "") {
        };
        assertThat(overMax.getBatteryLevel(), is(100));

        AbstractBluetoothDevice negative = new AbstractBluetoothDevice("B", "", "", false, false, -5, "") {
        };
        assertThat(negative.getBatteryLevel(), is(-1));

        AbstractBluetoothDevice valid = new AbstractBluetoothDevice("C", "", "", false, false, 50, "") {
        };
        assertThat(valid.getBatteryLevel(), is(50));
    }

    @Test
    void testToString() {
        AbstractBluetoothDevice device = new AbstractBluetoothDevice("Mouse", "11:22:33:44:55:66", "Peripheral", false,
                true, -1, "hci0") {
        };
        String str = device.toString();
        assertThat(str, containsString("Mouse"));
        assertThat(str, containsString("11:22:33:44:55:66"));
        assertThat(str, containsString("Peripheral"));
        assertThat(str, containsString("connected=false"));
        assertThat(str, containsString("paired=true"));
        assertThat(str, containsString("battery=-1"));
    }
}
