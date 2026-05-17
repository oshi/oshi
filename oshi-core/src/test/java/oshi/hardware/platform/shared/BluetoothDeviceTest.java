/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.shared;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.Test;

import oshi.SystemInfo;
import oshi.hardware.BluetoothDevice;

/**
 * Test Bluetooth device enumeration.
 */
class BluetoothDeviceTest {

    @Test
    void testBluetoothDevices() {
        SystemInfo si = new SystemInfo();
        for (BluetoothDevice device : si.getHardware().getBluetoothDevices()) {
            assertThat("Name should not be null", device.getName(), is(notNullValue()));
            assertThat("Address should not be null", device.getAddress(), is(notNullValue()));
            assertThat("Major device class should not be null", device.getMajorDeviceClass(), is(notNullValue()));
            assertThat("Adapter name should not be null", device.getAdapterName(), is(notNullValue()));
            int battery = device.getBatteryLevel();
            assertThat("Battery should be -1 or 0-100", battery, is(greaterThanOrEqualTo(-1)));
            assertThat("Battery should be -1 or 0-100", battery, is(lessThanOrEqualTo(100)));
            // Exercise toString
            assertThat("toString should not be null", device.toString(), is(notNullValue()));
        }
    }
}
