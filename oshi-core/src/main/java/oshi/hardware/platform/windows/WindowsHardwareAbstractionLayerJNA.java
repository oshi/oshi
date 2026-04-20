/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.Display;
import oshi.hardware.GlobalMemory;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HWDiskStore;
import oshi.hardware.LogicalVolumeGroup;
import oshi.hardware.NetworkIF;
import oshi.hardware.PowerSource;
import oshi.hardware.Printer;
import oshi.hardware.Sensors;
import oshi.hardware.SoundCard;
import oshi.hardware.UsbDevice;
import oshi.hardware.common.AbstractHardwareAbstractionLayer;

/**
 * WindowsHardwareAbstractionLayer class.
 */
@ThreadSafe
public class WindowsHardwareAbstractionLayerJNA extends AbstractHardwareAbstractionLayer {

    @Override
    public ComputerSystem createComputerSystem() {
        return new WindowsComputerSystemJNA();
    }

    @Override
    public GlobalMemory createMemory() {
        return new WindowsGlobalMemoryJNA();
    }

    @Override
    public CentralProcessor createProcessor() {
        return new WindowsCentralProcessorJNA();
    }

    @Override
    public Sensors createSensors() {
        return new WindowsSensorsJNA();
    }

    @Override
    public List<PowerSource> getPowerSources() {
        return WindowsPowerSourceJNA.getPowerSources();
    }

    @Override
    public List<HWDiskStore> getDiskStores() {
        return WindowsHWDiskStoreJNA.getDisks();
    }

    @Override
    public List<LogicalVolumeGroup> getLogicalVolumeGroups() {
        return WindowsLogicalVolumeGroupJNA.getLogicalVolumeGroups();
    }

    @Override
    public List<Display> getDisplays() {
        return WindowsDisplayJNA.getDisplays();
    }

    @Override
    public List<NetworkIF> getNetworkIFs(boolean includeLocalInterfaces) {
        return WindowsNetworkIfJNA.getNetworks(includeLocalInterfaces);
    }

    @Override
    public List<UsbDevice> getUsbDevices(boolean tree) {
        return WindowsUsbDeviceJNA.getUsbDevices(tree);
    }

    @Override
    public List<SoundCard> getSoundCards() {
        return WindowsSoundCardJNA.getSoundCards();
    }

    @Override
    public List<GraphicsCard> getGraphicsCards() {
        return WindowsGraphicsCardJNA.getGraphicsCards();
    }

    @Override
    public List<Printer> getPrinters() {
        return WindowsPrinterJNA.getPrinters();
    }
}
