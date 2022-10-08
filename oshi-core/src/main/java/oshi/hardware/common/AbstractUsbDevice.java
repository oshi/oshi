/*
 * Copyright 2016-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import java.util.Collections;
import java.util.List;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.UsbDevice;

/**
 * A USB device
 */
@Immutable
public abstract class AbstractUsbDevice implements UsbDevice {

    private final String name;
    private final String vendor;
    private final String vendorId;
    private final String productId;
    private final String serialNumber;
    private final String uniqueDeviceId;
    private final List<UsbDevice> connectedDevices;

    protected AbstractUsbDevice(String name, String vendor, String vendorId, String productId, String serialNumber,
            String uniqueDeviceId, List<UsbDevice> connectedDevices) {
        this.name = name;
        this.vendor = vendor;
        this.vendorId = vendorId;
        this.productId = productId;
        this.serialNumber = serialNumber;
        this.uniqueDeviceId = uniqueDeviceId;
        this.connectedDevices = Collections.unmodifiableList(connectedDevices);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getVendor() {
        return this.vendor;
    }

    @Override
    public String getVendorId() {
        return this.vendorId;
    }

    @Override
    public String getProductId() {
        return this.productId;
    }

    @Override
    public String getSerialNumber() {
        return this.serialNumber;
    }

    @Override
    public String getUniqueDeviceId() {
        return this.uniqueDeviceId;
    }

    @Override
    public List<UsbDevice> getConnectedDevices() {
        return this.connectedDevices;
    }

    @Override
    public int compareTo(UsbDevice usb) {
        // Naturally sort by device name
        return getName().compareTo(usb.getName());
    }

    @Override
    public String toString() {
        return indentUsb(this, 1);
    }

    /**
     * Helper method for indenting chained USB devices
     *
     * @param usbDevice A USB device to print
     * @param indent    number of spaces to indent
     * @return The device toString, indented
     */
    private static String indentUsb(UsbDevice usbDevice, int indent) {
        String indentFmt = indent > 4 ? String.format("%%%ds|-- ", indent - 4) : String.format("%%%ds", indent);
        StringBuilder sb = new StringBuilder(String.format(indentFmt, ""));
        sb.append(usbDevice.getName());
        if (!usbDevice.getVendor().isEmpty()) {
            sb.append(" (").append(usbDevice.getVendor()).append(')');
        }
        if (!usbDevice.getSerialNumber().isEmpty()) {
            sb.append(" [s/n: ").append(usbDevice.getSerialNumber()).append(']');
        }
        for (UsbDevice connected : usbDevice.getConnectedDevices()) {
            sb.append('\n').append(indentUsb(connected, indent + 4));
        }
        return sb.toString();
    }
}
