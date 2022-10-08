/*
 * Copyright 2016-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.linux;

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
import oshi.hardware.platform.unix.UnixDisplay;

/**
 * LinuxHardwareAbstractionLayer class.
 */
@ThreadSafe
public final class LinuxHardwareAbstractionLayer extends AbstractHardwareAbstractionLayer {

    @Override
    public ComputerSystem createComputerSystem() {
        return new LinuxComputerSystem();
    }

    @Override
    public GlobalMemory createMemory() {
        return new LinuxGlobalMemory();
    }

    @Override
    public CentralProcessor createProcessor() {
        return new LinuxCentralProcessor();
    }

    @Override
    public Sensors createSensors() {
        return new LinuxSensors();
    }

    @Override
    public List<PowerSource> getPowerSources() {
        return LinuxPowerSource.getPowerSources();
    }

    @Override
    public List<HWDiskStore> getDiskStores() {
        return LinuxHWDiskStore.getDisks();
    }

    @Override
    public List<LogicalVolumeGroup> getLogicalVolumeGroups() {
        return LinuxLogicalVolumeGroup.getLogicalVolumeGroups();
    }

    @Override
    public List<Display> getDisplays() {
        return UnixDisplay.getDisplays();
    }

    @Override
    public List<NetworkIF> getNetworkIFs(boolean includeLocalInterfaces) {
        return LinuxNetworkIF.getNetworks(includeLocalInterfaces);
    }

    @Override
    public List<UsbDevice> getUsbDevices(boolean tree) {
        return LinuxUsbDevice.getUsbDevices(tree);
    }

    @Override
    public List<SoundCard> getSoundCards() {
        return LinuxSoundCard.getSoundCards();
    }

    @Override
    public List<GraphicsCard> getGraphicsCards() {
        return LinuxGraphicsCard.getGraphicsCards();
    }
}
