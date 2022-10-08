/*
 * Copyright 2021-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.openbsd;

import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.Display;
import oshi.hardware.GlobalMemory;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HWDiskStore;
import oshi.hardware.NetworkIF;
import oshi.hardware.PowerSource;
import oshi.hardware.Sensors;
import oshi.hardware.SoundCard;
import oshi.hardware.UsbDevice;
import oshi.hardware.common.AbstractHardwareAbstractionLayer;
import oshi.hardware.platform.unix.BsdNetworkIF;
import oshi.hardware.platform.unix.UnixDisplay;

/**
 * OpenBsdHardwareAbstractionLayer class.
 */
@ThreadSafe
public final class OpenBsdHardwareAbstractionLayer extends AbstractHardwareAbstractionLayer {

    @Override
    public ComputerSystem createComputerSystem() {
        return new OpenBsdComputerSystem();
    }

    @Override
    public GlobalMemory createMemory() {
        return new OpenBsdGlobalMemory();
    }

    @Override
    public CentralProcessor createProcessor() {
        return new OpenBsdCentralProcessor();
    }

    @Override
    public Sensors createSensors() {
        return new OpenBsdSensors();
    }

    @Override
    public List<PowerSource> getPowerSources() {
        return OpenBsdPowerSource.getPowerSources();
    }

    @Override
    public List<HWDiskStore> getDiskStores() {
        return OpenBsdHWDiskStore.getDisks();
    }

    @Override
    public List<Display> getDisplays() {
        return UnixDisplay.getDisplays();
    }

    @Override
    public List<NetworkIF> getNetworkIFs(boolean includeLocalInterfaces) {
        return BsdNetworkIF.getNetworks(includeLocalInterfaces);
    }

    @Override
    public List<UsbDevice> getUsbDevices(boolean tree) {
        return OpenBsdUsbDevice.getUsbDevices(tree);
    }

    @Override
    public List<SoundCard> getSoundCards() {
        return OpenBsdSoundCard.getSoundCards();
    }

    @Override
    public List<GraphicsCard> getGraphicsCards() {
        return OpenBsdGraphicsCard.getGraphicsCards();
    }
}
