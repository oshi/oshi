/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.BluetoothDevice;

/**
 * Abstract base class for BluetoothDevice implementations.
 */
@Immutable
public abstract class AbstractBluetoothDevice implements BluetoothDevice {

    private final String name;
    private final String address;
    private final String majorDeviceClass;
    private final boolean connected;
    private final boolean paired;
    private final int batteryLevel;
    private final String adapterName;

    /**
     * Creates an AbstractBluetoothDevice with the given parameters.
     *
     * @param name             the device name
     * @param address          the MAC address
     * @param majorDeviceClass the major device class
     * @param connected        whether the device is connected
     * @param paired           whether the device is paired
     * @param batteryLevel     the battery level (0–100), or -1 if unavailable
     * @param adapterName      the adapter name
     */
    protected AbstractBluetoothDevice(String name, String address, String majorDeviceClass, boolean connected,
            boolean paired, int batteryLevel, String adapterName) {
        this.name = name;
        this.address = address;
        this.majorDeviceClass = majorDeviceClass;
        this.connected = connected;
        this.paired = paired;
        this.batteryLevel = batteryLevel < 0 ? -1 : Math.min(100, batteryLevel);
        this.adapterName = adapterName;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public String getMajorDeviceClass() {
        return majorDeviceClass;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public boolean isPaired() {
        return paired;
    }

    @Override
    public int getBatteryLevel() {
        return batteryLevel;
    }

    @Override
    public String getAdapterName() {
        return adapterName;
    }

    @Override
    public String toString() {
        return "BluetoothDevice [name=" + name + ", address=" + address + ", class=" + majorDeviceClass + ", connected="
                + connected + ", paired=" + paired + ", battery=" + batteryLevel + ", adapter=" + adapterName + "]";
    }
}
