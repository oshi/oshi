/*
 * Copyright 2016-2022 The OSHI Project Contributors
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
import oshi.hardware.Sensors;
import oshi.hardware.SoundCard;
import oshi.hardware.UsbDevice;
import oshi.hardware.common.AbstractHardwareAbstractionLayer;

/**
 * WindowsHardwareAbstractionLayer class.
 */
@ThreadSafe
public final class WindowsHardwareAbstractionLayer extends AbstractHardwareAbstractionLayer {

    @Override
    public ComputerSystem createComputerSystem() {
        return new WindowsComputerSystem();
    }

    @Override
    public GlobalMemory createMemory() {
        return new WindowsGlobalMemory();
    }

    @Override
    public CentralProcessor createProcessor() {
        return new WindowsCentralProcessor();
    }

    @Override
    public Sensors createSensors() {
        return new WindowsSensors();
    }

    @Override
    public List<PowerSource> getPowerSources() {
        return WindowsPowerSource.getPowerSources();
    }

    @Override
    public List<HWDiskStore> getDiskStores() {
        return WindowsHWDiskStore.getDisks();
    }

    @Override
    public List<LogicalVolumeGroup> getLogicalVolumeGroups() {
        return WindowsLogicalVolumeGroup.getLogicalVolumeGroups();
    }

    @Override
    public List<Display> getDisplays() {
        return WindowsDisplay.getDisplays();
    }

    @Override
    public List<NetworkIF> getNetworkIFs(boolean includeLocalInterfaces) {
        return WindowsNetworkIF.getNetworks(includeLocalInterfaces);
    }

    @Override
    public List<UsbDevice> getUsbDevices(boolean tree) {
        return WindowsUsbDevice.getUsbDevices(tree);
    }

    @Override
    public List<SoundCard> getSoundCards() {
        return WindowsSoundCard.getSoundCards();
    }

    @Override
    public List<GraphicsCard> getGraphicsCards() {
        return WindowsGraphicsCard.getGraphicsCards();
    }
}
