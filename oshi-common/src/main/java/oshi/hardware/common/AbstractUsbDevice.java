/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.UsbDevice;
import oshi.util.ParseUtil;

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

    /**
     * Creates an AbstractUsbDevice with the given parameters.
     *
     * @param name             the device name
     * @param vendor           the vendor name, or {@code null} if the platform did not report one
     * @param vendorId         the vendor ID
     * @param productId        the product ID
     * @param serialNumber     the serial number
     * @param uniqueDeviceId   the unique device ID, or {@code null} if the platform did not report one
     * @param connectedDevices the list of connected child devices
     */
    protected AbstractUsbDevice(String name, @Nullable String vendor, String vendorId, String productId,
            String serialNumber, @Nullable String uniqueDeviceId, List<UsbDevice> connectedDevices) {
        this.name = name;
        this.vendor = ParseUtil.getStringValueOrEmpty(vendor);
        this.vendorId = vendorId;
        this.productId = productId;
        this.serialNumber = serialNumber;
        this.uniqueDeviceId = ParseUtil.getStringValueOrEmpty(uniqueDeviceId);
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

    /**
     * Compares this device to another for equality. Two devices are equal when their name, unique device ID, vendor ID,
     * product ID and serial number all match, which are the same fields {@link UsbDevice#compareTo(UsbDevice)} orders
     * by.
     * <p>
     * Only another {@link AbstractUsbDevice} can be equal to this one. Widening that to any {@link UsbDevice} would
     * make equality asymmetric, because a foreign implementation carrying the same values is free to keep the
     * identity-based {@code equals} it inherits from {@link Object}. Ordering has no such limitation, because
     * {@link UsbDevice} supplies it to every implementation.
     *
     * @param obj the object to compare with
     * @return {@code true} if the two devices carry the same identity fields
     */
    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AbstractUsbDevice)) {
            return false;
        }
        AbstractUsbDevice other = (AbstractUsbDevice) obj;
        return getName().equals(other.getName()) && getUniqueDeviceId().equals(other.getUniqueDeviceId())
                && getVendorId().equals(other.getVendorId()) && getProductId().equals(other.getProductId())
                && getSerialNumber().equals(other.getSerialNumber());
    }

    /**
     * Returns a hash code over the same identity fields {@link #equals(Object)} compares.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(getName(), getUniqueDeviceId(), getVendorId(), getProductId(), getSerialNumber());
    }

    /**
     * Recursively adds USB devices from {@code list} to {@code deviceList}, depth-first.
     *
     * @param deviceList the target list to add devices to
     * @param list       the source list of devices (and their children) to add
     */
    protected static void addDevicesToList(List<UsbDevice> deviceList, List<UsbDevice> list) {
        for (UsbDevice device : list) {
            deviceList.add(device);
            addDevicesToList(deviceList, device.getConnectedDevices());
        }
    }

    /**
     * Recursively builds a USB device and its children from attribute maps keyed by a device identifier, applying the
     * parent's vendor/product IDs as defaults and sorting children by name. Shared by the platform implementations
     * whose device model is a keyed parent/child tree (the flat/PnP-style models, e.g. AIX and Windows, build their
     * own).
     *
     * @param id           the identifier of the device to build (also used as its unique device ID)
     * @param vid          the default vendor ID, inherited from the parent when this device reports none
     * @param pid          the default product ID, inherited from the parent when this device reports none
     * @param nameMap      id to product name
     * @param vendorMap    id to vendor/manufacturer name
     * @param vendorIdMap  id to vendor ID
     * @param productIdMap id to product ID
     * @param serialMap    id to serial number
     * @param hubMap       parent id to its child ids
     * @param factory      constructs the platform-specific {@link UsbDevice} from the resolved fields
     * @return the assembled device, with its connected devices populated recursively
     */
    protected static UsbDevice buildDeviceTree(@Nullable String id, String vid, String pid, Map<String, String> nameMap,
            Map<String, String> vendorMap, Map<String, String> vendorIdMap, Map<String, String> productIdMap,
            Map<String, String> serialMap, Map<String, List<String>> hubMap, UsbDeviceFactory factory) {
        String vendorId = vendorIdMap.getOrDefault(id, vid);
        String productId = productIdMap.getOrDefault(id, pid);
        List<UsbDevice> connectedDevices = new ArrayList<>();
        for (String childId : hubMap.getOrDefault(id, Collections.emptyList())) {
            connectedDevices.add(buildDeviceTree(childId, vendorId, productId, nameMap, vendorMap, vendorIdMap,
                    productIdMap, serialMap, hubMap, factory));
        }
        Collections.sort(connectedDevices);
        return factory.create(nameMap.getOrDefault(id, vendorId + ":" + productId), vendorMap.getOrDefault(id, ""),
                vendorId, productId, serialMap.getOrDefault(id, ""), id, connectedDevices);
    }

    /**
     * Constructs a platform-specific {@link UsbDevice} from resolved fields; supplied to {@link #buildDeviceTree} so
     * the shared tree assembly can create the correct per-platform type.
     */
    @FunctionalInterface
    public interface UsbDeviceFactory {
        /**
         * Creates a {@link UsbDevice}.
         *
         * @param name             the device name
         * @param vendor           the vendor name, or an empty string if the platform did not report one
         * @param vendorId         the vendor ID
         * @param productId        the product ID
         * @param serialNumber     the serial number
         * @param uniqueDeviceId   the unique device ID, or {@code null} if the platform did not report one
         * @param connectedDevices the connected child devices
         * @return the created device
         */
        UsbDevice create(String name, String vendor, String vendorId, String productId, String serialNumber,
                @Nullable String uniqueDeviceId, List<UsbDevice> connectedDevices);
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
        String indentFmt = indent > 4 ? String.format(Locale.ROOT, "%%%ds|-- ", indent - 4)
                : String.format(Locale.ROOT, "%%%ds", indent);
        StringBuilder sb = new StringBuilder(String.format(Locale.ROOT, indentFmt, ""));
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
