/*
 * Copyright 2016-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import java.util.List;

import oshi.annotation.concurrent.Immutable;

/**
 * A USB device is a device connected via a USB port, possibly internally/permanently. Hubs may contain ports to which
 * other devices connect in a recursive fashion.
 */
@Immutable
public interface UsbDevice extends Comparable<UsbDevice> {
    /**
     * Name of the USB device
     *
     * @return The device name
     */
    String getName();

    /**
     * Vendor that manufactured the USB device
     *
     * @return The vendor name
     */
    String getVendor();

    /**
     * ID of the vendor that manufactured the USB device
     *
     * @return The vendor ID, a 4-digit hex string
     */
    String getVendorId();

    /**
     * Product ID of the USB device
     *
     * @return The product ID, a 4-digit hex string
     */
    String getProductId();

    /**
     * Serial number of the USB device
     *
     * @return The serial number, if known
     */
    String getSerialNumber();

    /**
     * A Unique Device ID of the USB device, such as the PnPDeviceID (Windows), Device Node Path (Linux), Registry Entry
     * ID (macOS), or Device Node number (Unix)
     *
     * @return The Unique Device ID
     */
    String getUniqueDeviceId();

    /**
     * Other devices connected to this hub
     *
     * @return An {@code UnmodifiableList} of other devices connected to this hub, if any, or an empty list if none
     */
    List<UsbDevice> getConnectedDevices();
}
