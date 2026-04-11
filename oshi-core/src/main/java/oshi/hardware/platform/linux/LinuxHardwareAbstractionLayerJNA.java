/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.linux;

import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HWDiskStore;
import oshi.hardware.LogicalVolumeGroup;
import oshi.hardware.NetworkIF;
import oshi.hardware.PowerSource;
import oshi.hardware.Printer;
import oshi.hardware.UsbDevice;
import oshi.hardware.common.platform.linux.LinuxGlobalMemory;
import oshi.hardware.common.platform.linux.LinuxHardwareAbstractionLayer;
import oshi.hardware.platform.unix.UnixPrinter;
import oshi.software.os.linux.LinuxOperatingSystemJNA;

/**
 * JNA-based Linux hardware abstraction layer. Extends {@link LinuxHardwareAbstractionLayer}, overriding methods as FFM
 * equivalents are migrated to {@code LinuxHardwareAbstractionLayerFFM}.
 */
@ThreadSafe
public final class LinuxHardwareAbstractionLayerJNA extends LinuxHardwareAbstractionLayer {

    @Override
    public GlobalMemory createMemory() {
        return new LinuxGlobalMemory(LinuxOperatingSystemJNA.pageSize());
    }

    @Override
    public CentralProcessor createProcessor() {
        return new LinuxCentralProcessorJNA();
    }

    @Override
    public List<LogicalVolumeGroup> getLogicalVolumeGroups() {
        return LinuxLogicalVolumeGroupJNA.getLogicalVolumeGroups();
    }

    @Override
    public List<NetworkIF> getNetworkIFs(boolean includeLocalInterfaces) {
        return LinuxNetworkIFJNA.getNetworks(includeLocalInterfaces);
    }

    @Override
    public List<HWDiskStore> getDiskStores() {
        return LinuxHWDiskStoreJNA.getDisks();
    }

    @Override
    public List<UsbDevice> getUsbDevices(boolean tree) {
        return LinuxUsbDeviceJNA.getUsbDevices(tree);
    }

    @Override
    public List<GraphicsCard> getGraphicsCards() {
        return LinuxGraphicsCardJNA.getGraphicsCards();
    }

    @Override
    public List<Printer> getPrinters() {
        return UnixPrinter.getPrinters();
    }

    @Override
    public List<PowerSource> getPowerSources() {
        return LinuxPowerSourceJNA.getPowerSources();
    }
}
