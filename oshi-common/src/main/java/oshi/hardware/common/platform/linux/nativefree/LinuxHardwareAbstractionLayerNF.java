/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux.nativefree;

import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HWDiskStore;
import oshi.hardware.NetworkIF;
import oshi.hardware.PowerSource;
import oshi.hardware.UsbDevice;
import oshi.hardware.common.platform.linux.LinuxGlobalMemory;
import oshi.hardware.common.platform.linux.LinuxHardwareAbstractionLayer;
import oshi.hardware.common.platform.linux.LinuxPowerSource;
import oshi.software.common.os.linux.nativefree.LinuxOperatingSystemNF;

/**
 * Native-free hardware abstraction layer for Linux. Extends {@link LinuxHardwareAbstractionLayer}, providing
 * implementations that require no native access.
 */
@ThreadSafe
public final class LinuxHardwareAbstractionLayerNF extends LinuxHardwareAbstractionLayer {

    @Override
    public GlobalMemory createMemory() {
        return new LinuxGlobalMemory(LinuxOperatingSystemNF.pageSize());
    }

    @Override
    public CentralProcessor createProcessor() {
        return new LinuxCentralProcessorNF();
    }

    @Override
    public List<HWDiskStore> getDiskStores() {
        return LinuxHWDiskStoreNF.getDisks();
    }

    @Override
    public List<NetworkIF> getNetworkIFs(boolean includeLocalInterfaces) {
        return LinuxNetworkIfNF.getNetworks(includeLocalInterfaces);
    }

    @Override
    public List<PowerSource> getPowerSources() {
        return LinuxPowerSource.getPowerSources();
    }

    @Override
    protected List<UsbDevice> createUsbDevices() {
        return LinuxUsbDeviceNF.getUsbDevices();
    }

    @Override
    protected List<GraphicsCard> createGraphicsCards() {
        return LinuxGraphicsCardNF.getGraphicsCards();
    }
}
