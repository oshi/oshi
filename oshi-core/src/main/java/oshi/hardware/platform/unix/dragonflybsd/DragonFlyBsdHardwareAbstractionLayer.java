/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.dragonflybsd;

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
import oshi.hardware.Printer;
import oshi.hardware.Sensors;
import oshi.hardware.SoundCard;
import oshi.hardware.UsbDevice;
import oshi.hardware.common.AbstractHardwareAbstractionLayer;
import oshi.hardware.common.platform.unix.UnixDisplay;
import oshi.hardware.platform.unix.BsdNetworkIF;
import oshi.hardware.platform.unix.CupsPrinterJNA;

/**
 * DragonFlyBsdHardwareAbstractionLayer class.
 */
@ThreadSafe
public final class DragonFlyBsdHardwareAbstractionLayer extends AbstractHardwareAbstractionLayer {

    @Override
    public ComputerSystem createComputerSystem() {
        return new DragonFlyBsdComputerSystem();
    }

    @Override
    public GlobalMemory createMemory() {
        return new DragonFlyBsdGlobalMemory();
    }

    @Override
    public CentralProcessor createProcessor() {
        return new DragonFlyBsdCentralProcessor();
    }

    @Override
    public Sensors createSensors() {
        return new DragonFlyBsdSensors();
    }

    @Override
    public List<PowerSource> getPowerSources() {
        return DragonFlyBsdPowerSource.getPowerSources();
    }

    @Override
    public List<HWDiskStore> getDiskStores() {
        return DragonFlyBsdHWDiskStore.getDisks();
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
        return DragonFlyBsdUsbDevice.getUsbDevices(tree);
    }

    @Override
    public List<SoundCard> getSoundCards() {
        return DragonFlyBsdSoundCard.getSoundCards();
    }

    @Override
    public List<GraphicsCard> getGraphicsCards() {
        return DragonFlyBsdGraphicsCard.getGraphicsCards();
    }

    @Override
    public List<Printer> getPrinters() {
        return CupsPrinterJNA.getPrinters();
    }
}
