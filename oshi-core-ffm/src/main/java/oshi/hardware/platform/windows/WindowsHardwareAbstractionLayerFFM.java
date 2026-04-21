/*
 * Copyright 2026 The OSHI Project Contributors
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
 * FFM-based hardware abstraction layer for Windows. Extends {@link AbstractHardwareAbstractionLayer} with pure-FFM
 * implementations for all Windows hardware components.
 */
@ThreadSafe
public final class WindowsHardwareAbstractionLayerFFM extends AbstractHardwareAbstractionLayer {

    @Override
    public CentralProcessor createProcessor() {
        return new WindowsCentralProcessorFFM();
    }

    @Override
    public ComputerSystem createComputerSystem() {
        return new WindowsComputerSystemFFM();
    }

    @Override
    public GlobalMemory createMemory() {
        return new WindowsGlobalMemoryFFM();
    }

    @Override
    public Sensors createSensors() {
        return new WindowsSensorsFFM();
    }

    @Override
    public List<PowerSource> getPowerSources() {
        return WindowsPowerSourceFFM.getPowerSources();
    }

    @Override
    public List<GraphicsCard> getGraphicsCards() {
        return WindowsGraphicsCardFFM.getGraphicsCards();
    }

    @Override
    public List<LogicalVolumeGroup> getLogicalVolumeGroups() {
        return WindowsLogicalVolumeGroupFFM.getLogicalVolumeGroups();
    }

    @Override
    public List<NetworkIF> getNetworkIFs(boolean includeLocalInterfaces) {
        return WindowsNetworkIfFFM.getNetworks(includeLocalInterfaces);
    }

    @Override
    public List<Display> getDisplays() {
        return WindowsDisplayFFM.getDisplays();
    }

    @Override
    public List<UsbDevice> getUsbDevices(boolean tree) {
        return WindowsUsbDeviceFFM.getUsbDevices(tree);
    }

    @Override
    public List<HWDiskStore> getDiskStores() {
        return WindowsHWDiskStoreFFM.getDisks();
    }

    @Override
    public List<SoundCard> getSoundCards() {
        return WindowsSoundCardFFM.getSoundCards();
    }

    @Override
    public List<Printer> getPrinters() {
        return WindowsPrinterFFM.getPrinters();
    }
}
