/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows;

import java.util.Locale;

/**
 * Immutable snapshot of a DXGI adapter's identity and dedicated video memory.
 *
 * <p>
 * Populated by {@link oshi.jna.platform.windows.WindowsDxgi#queryAdapters()} and used by
 * {@link oshi.hardware.platform.windows.WindowsGraphicsCard} to supply accurate VRAM values.
 * {@code DedicatedVideoMemory} from {@code DXGI_ADAPTER_DESC} is the authoritative Windows API source for dedicated GPU
 * memory; it is not subject to the 2 GiB cap that affects the 32-bit registry value
 * {@code HardwareInformation.MemorySize}.
 */
public final class DxgiAdapterInfo {

    private final String description;
    private final int vendorId;
    private final int deviceId;
    private final long dedicatedVideoMemory;

    /**
     * @param description          adapter description string from {@code DXGI_ADAPTER_DESC.Description}
     * @param vendorId             PCI vendor ID
     * @param deviceId             PCI device ID
     * @param dedicatedVideoMemory dedicated video memory in bytes
     */
    public DxgiAdapterInfo(String description, int vendorId, int deviceId, long dedicatedVideoMemory) {
        this.description = description;
        this.vendorId = vendorId;
        this.deviceId = deviceId;
        this.dedicatedVideoMemory = dedicatedVideoMemory;
    }

    /**
     * Returns the adapter description string.
     *
     * @return adapter description string
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the PCI vendor ID.
     *
     * @return PCI vendor ID
     */
    public int getVendorId() {
        return vendorId;
    }

    /**
     * Returns the PCI device ID.
     *
     * @return PCI device ID
     */
    public int getDeviceId() {
        return deviceId;
    }

    /**
     * Returns the dedicated video memory in bytes.
     *
     * @return dedicated video memory in bytes
     */
    public long getDedicatedVideoMemory() {
        return dedicatedVideoMemory;
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "DxgiAdapterInfo{desc='%s', vendor=0x%04X, device=0x%04X, vram=%d}",
                description, vendorId, deviceId, dedicatedVideoMemory);
    }
}
