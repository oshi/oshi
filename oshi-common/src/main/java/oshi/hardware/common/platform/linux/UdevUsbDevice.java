/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.Immutable;

/**
 * Raw attributes for a single Linux USB device, gathered by a backend enumeration (udev via JNA or FFM, or the
 * native-free sysfs reader). The backend only iterates and extracts these fields; {@link LinuxUsbDevice} builds the
 * device tree from a list of these carriers, so the parsing/parent-child/sorting logic lives in one place. Any field
 * may be {@code null} if the backend did not report it; {@link #getParentSyspath()} is {@code null} for a root USB
 * controller.
 */
@Immutable
public class UdevUsbDevice {

    private final String syspath;
    private final @Nullable String product;
    private final @Nullable String manufacturer;
    private final @Nullable String vendorId;
    private final @Nullable String productId;
    private final @Nullable String serial;
    private final @Nullable String parentSyspath;

    /**
     * Creates a UdevUsbDevice.
     *
     * @param syspath       the device's sysfs path (the map key)
     * @param product       the product name, or {@code null}
     * @param manufacturer  the manufacturer/vendor name, or {@code null}
     * @param vendorId      the vendor ID, or {@code null}
     * @param productId     the product ID, or {@code null}
     * @param serial        the serial number, or {@code null}
     * @param parentSyspath the parent device's syspath, or {@code null} if this is a root controller
     */
    public UdevUsbDevice(String syspath, @Nullable String product, @Nullable String manufacturer,
            @Nullable String vendorId, @Nullable String productId, @Nullable String serial,
            @Nullable String parentSyspath) {
        this.syspath = syspath;
        this.product = product;
        this.manufacturer = manufacturer;
        this.vendorId = vendorId;
        this.productId = productId;
        this.serial = serial;
        this.parentSyspath = parentSyspath;
    }

    /**
     * @return the device's sysfs path (the map key)
     */
    public String getSyspath() {
        return syspath;
    }

    /**
     * @return the product name, or {@code null}
     */
    public @Nullable String getProduct() {
        return product;
    }

    /**
     * @return the manufacturer/vendor name, or {@code null}
     */
    public @Nullable String getManufacturer() {
        return manufacturer;
    }

    /**
     * @return the vendor ID, or {@code null}
     */
    public @Nullable String getVendorId() {
        return vendorId;
    }

    /**
     * @return the product ID, or {@code null}
     */
    public @Nullable String getProductId() {
        return productId;
    }

    /**
     * @return the serial number, or {@code null}
     */
    public @Nullable String getSerial() {
        return serial;
    }

    /**
     * @return the parent device's syspath, or {@code null} if this is a root controller
     */
    public @Nullable String getParentSyspath() {
        return parentSyspath;
    }
}
