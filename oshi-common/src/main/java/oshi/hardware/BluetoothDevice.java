/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import oshi.annotation.PublicApi;
import oshi.annotation.concurrent.Immutable;

/**
 * Represents a Bluetooth device (paired or connected) known to the system, analogous to {@link UsbDevice} for USB
 * peripherals.
 * <p>
 * Bluetooth devices are enumerated per adapter. Each device reports its name, MAC address, major device class,
 * connection/pairing state, and battery level when available.
 * <p>
 * Example usage:
 *
 * <pre>{@code
 * for (BluetoothDevice device : hal.getBluetoothDevices()) {
 *     System.out.printf("%s [%s] %s%s (battery: %s)%n", device.getName(), device.getAddress(),
 *             device.getMajorDeviceClass(), device.isConnected() ? " *connected*" : "",
 *             device.getBatteryLevel() >= 0 ? device.getBatteryLevel() + "%" : "N/A");
 * }
 * }</pre>
 */
@PublicApi
@Immutable
public interface BluetoothDevice {

    /**
     * The user-visible name of the Bluetooth device.
     *
     * @return The device name, or an empty string if unknown.
     */
    String getName();

    /**
     * The MAC address of the Bluetooth device in colon-separated format (e.g., {@code AA:BB:CC:DD:EE:FF}).
     *
     * @return The device MAC address.
     */
    String getAddress();

    /**
     * The major Bluetooth device class (e.g., "Audio", "Phone", "Computer", "Peripheral", "Networking").
     * <p>
     * Derived from the Class of Device (CoD) field when available.
     *
     * @return The major device class string, or an empty string if unknown.
     */
    String getMajorDeviceClass();

    /**
     * Whether the device is currently connected to this system.
     *
     * @return {@code true} if connected, {@code false} otherwise.
     */
    boolean isConnected();

    /**
     * Whether the device is paired (bonded) with this system.
     *
     * @return {@code true} if paired, {@code false} otherwise.
     */
    boolean isPaired();

    /**
     * The battery level of the device as a percentage (0–100).
     *
     * @return The battery percentage, or {@code -1} if not available.
     */
    int getBatteryLevel();

    /**
     * The name of the adapter (e.g., {@code hci0}) through which this device is known.
     *
     * @return The adapter name.
     */
    String getAdapterName();
}
