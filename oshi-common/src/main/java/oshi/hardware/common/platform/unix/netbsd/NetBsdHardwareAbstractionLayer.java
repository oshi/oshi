/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.netbsd;

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
import oshi.hardware.common.platform.unix.BsdGraphicsCard;
import oshi.hardware.common.platform.unix.BsdPowerSource;
import oshi.hardware.common.platform.unix.BsdSensors;
import oshi.hardware.common.platform.unix.BsdSoundCard;
import oshi.hardware.common.platform.unix.BsdUsbDevice;
import oshi.hardware.common.platform.unix.LpstatPrinter;
import oshi.hardware.common.platform.unix.UnixDisplay;

/**
 * NetBsdHardwareAbstractionLayer class.
 */
@ThreadSafe
public class NetBsdHardwareAbstractionLayer extends AbstractHardwareAbstractionLayer {

    @Override
    public ComputerSystem createComputerSystem() {
        return new NetBsdComputerSystem();
    }

    @Override
    public GlobalMemory createMemory() {
        return new NetBsdGlobalMemory();
    }

    @Override
    public CentralProcessor createProcessor() {
        return new NetBsdCentralProcessor();
    }

    @Override
    public Sensors createSensors() {
        return new BsdSensors();
    }

    @Override
    public List<PowerSource> getPowerSources() {
        return BsdPowerSource.getPowerSources();
    }

    @Override
    public List<HWDiskStore> getDiskStores() {
        return NetBsdHWDiskStore.getDisks();
    }

    @Override
    protected List<Display> createDisplays() {
        return UnixDisplay.getDisplays();
    }

    @Override
    public List<NetworkIF> getNetworkIFs(boolean includeLocalInterfaces) {
        return NetBsdNetworkIF.getNetworks(includeLocalInterfaces);
    }

    @Override
    protected List<UsbDevice> createUsbDevices() {
        return BsdUsbDevice.getUsbDevices();
    }

    @Override
    protected List<SoundCard> createSoundCards() {
        return BsdSoundCard.getSoundCards();
    }

    @Override
    protected List<GraphicsCard> createGraphicsCards() {
        return BsdGraphicsCard.getGraphicsCards();
    }

    @Override
    public List<Printer> getPrinters() {
        return LpstatPrinter.getPrinters();
    }
}
