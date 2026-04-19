/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.gpu;

import java.util.Locale;

/**
 * Immutable snapshot of a DXGI adapter's identity and dedicated video memory.
 *
 * <p>
 * {@code DedicatedVideoMemory} from {@code DXGI_ADAPTER_DESC} is the authoritative Windows API source for dedicated GPU
 * memory; it is not subject to the 2 GiB cap that affects the 32-bit registry value
 * {@code HardwareInformation.MemorySize}.
 */
public final class DxgiAdapterInfo {

    private final String description;
    private final int vendorId;
    private final int deviceId;
    private final long dedicatedVideoMemory;
    private final int luidLowPart;
    private final int luidHighPart;

    /**
     * @param description          adapter description string from {@code DXGI_ADAPTER_DESC.Description}
     * @param vendorId             PCI vendor ID
     * @param deviceId             PCI device ID
     * @param dedicatedVideoMemory dedicated video memory in bytes
     * @param luidLowPart          low 32 bits of the adapter LUID
     * @param luidHighPart         high 32 bits of the adapter LUID
     */
    public DxgiAdapterInfo(String description, int vendorId, int deviceId, long dedicatedVideoMemory, int luidLowPart,
            int luidHighPart) {
        this.description = description;
        this.vendorId = vendorId;
        this.deviceId = deviceId;
        this.dedicatedVideoMemory = dedicatedVideoMemory;
        this.luidLowPart = luidLowPart;
        this.luidHighPart = luidHighPart;
    }

    public String getDescription() {
        return description;
    }

    public int getVendorId() {
        return vendorId;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public long getDedicatedVideoMemory() {
        return dedicatedVideoMemory;
    }

    public int getLuidLowPart() {
        return luidLowPart;
    }

    public int getLuidHighPart() {
        return luidHighPart;
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT,
                "DxgiAdapterInfo{desc='%s', vendor=0x%04X, device=0x%04X, vram=%d, luid=0x%08X_0x%08X}", description,
                vendorId, deviceId, dedicatedVideoMemory, luidHighPart, luidLowPart);
    }
}
