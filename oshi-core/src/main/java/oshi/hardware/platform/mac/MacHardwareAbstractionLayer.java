/*
 * Copyright 2016-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

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
 * MacHardwareAbstractionLayer class.
 */
@ThreadSafe
public final class MacHardwareAbstractionLayer extends AbstractHardwareAbstractionLayer {

    @Override
    public ComputerSystem createComputerSystem() {
        return new MacComputerSystem();
    }

    @Override
    public GlobalMemory createMemory() {
        return new MacGlobalMemory();
    }

    @Override
    public CentralProcessor createProcessor() {
        return new MacCentralProcessor();
    }

    @Override
    public Sensors createSensors() {
        return new MacSensors();
    }

    @Override
    public List<PowerSource> getPowerSources() {
        return MacPowerSource.getPowerSources();
    }

    @Override
    public List<HWDiskStore> getDiskStores() {
        return MacHWDiskStore.getDisks();
    }

    @Override
    public List<LogicalVolumeGroup> getLogicalVolumeGroups() {
        return MacLogicalVolumeGroup.getLogicalVolumeGroups();
    }

    @Override
    public List<Display> getDisplays() {
        return MacDisplay.getDisplays();
    }

    @Override
    public List<NetworkIF> getNetworkIFs(boolean includeLocalInterfaces) {
        return MacNetworkIF.getNetworks(includeLocalInterfaces);
    }

    @Override
    public List<UsbDevice> getUsbDevices(boolean tree) {
        return MacUsbDevice.getUsbDevices(tree);
    }

    @Override
    public List<SoundCard> getSoundCards() {
        return MacSoundCard.getSoundCards();
    }

    @Override
    public List<GraphicsCard> getGraphicsCards() {
        return MacGraphicsCard.getGraphicsCards();
    }
}
