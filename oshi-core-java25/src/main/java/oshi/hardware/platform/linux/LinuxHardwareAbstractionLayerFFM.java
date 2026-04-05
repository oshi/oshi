/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.linux;

import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HWDiskStore;
import oshi.hardware.LogicalVolumeGroup;
import oshi.hardware.NetworkIF;
import oshi.hardware.PowerSource;
import oshi.hardware.UsbDevice;

/**
 * FFM-based hardware abstraction layer for Linux. Extends {@link LinuxHardwareAbstractionLayer}, overriding methods as
 * FFM implementations become available.
 */
@ThreadSafe
public final class LinuxHardwareAbstractionLayerFFM extends LinuxHardwareAbstractionLayer {

    @Override
    public CentralProcessor createProcessor() {
        return new LinuxCentralProcessorFFM();
    }

    @Override
    public List<LogicalVolumeGroup> getLogicalVolumeGroups() {
        return LinuxLogicalVolumeGroupFFM.getLogicalVolumeGroups();
    }

    @Override
    public List<NetworkIF> getNetworkIFs(boolean includeLocalInterfaces) {
        return LinuxNetworkIFFFM.getNetworks(includeLocalInterfaces);
    }

    @Override
    public List<HWDiskStore> getDiskStores() {
        return LinuxHWDiskStoreFFM.getDisks();
    }

    @Override
    public List<PowerSource> getPowerSources() {
        return LinuxPowerSourceFFM.getPowerSources();
    }

    @Override
    public List<UsbDevice> getUsbDevices(boolean tree) {
        return LinuxUsbDeviceFFM.getUsbDevices(tree);
    }
}
