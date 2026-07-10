/*
 * Copyright 2026 The OSHI Project Contributors
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
import oshi.hardware.common.platform.unix.BsdNetworkIF;
import oshi.hardware.common.platform.unix.UnixDisplay;
import oshi.hardware.common.platform.unix.freebsd.FreeBsdGraphicsCard;
import oshi.hardware.common.platform.unix.freebsd.FreeBsdSoundCard;
import oshi.hardware.common.platform.unix.freebsd.FreeBsdUsbDevice;
import oshi.hardware.platform.unix.CupsPrinterJNA;
import oshi.hardware.platform.unix.freebsd.FreeBsdComputerSystemJNA;
import oshi.hardware.platform.unix.freebsd.FreeBsdGlobalMemoryJNA;
import oshi.hardware.platform.unix.freebsd.FreeBsdHWDiskStoreJNA;
import oshi.hardware.platform.unix.freebsd.FreeBsdPowerSourceJNA;
import oshi.hardware.platform.unix.freebsd.FreeBsdSensorsJNA;

/**
 * DragonFlyBsdHardwareAbstractionLayerJNA class. Uses FreeBSD implementations where behavior is identical.
 */
@ThreadSafe
public final class DragonFlyBsdHardwareAbstractionLayerJNA extends AbstractHardwareAbstractionLayer {

    @Override
    public ComputerSystem createComputerSystem() {
        return new FreeBsdComputerSystemJNA();
    }

    @Override
    public GlobalMemory createMemory() {
        return new FreeBsdGlobalMemoryJNA();
    }

    @Override
    public CentralProcessor createProcessor() {
        return new DragonFlyBsdCentralProcessorJNA();
    }

    @Override
    public Sensors createSensors() {
        return new FreeBsdSensorsJNA();
    }

    @Override
    public List<PowerSource> getPowerSources() {
        return FreeBsdPowerSourceJNA.getPowerSources();
    }

    @Override
    public List<HWDiskStore> getDiskStores() {
        return FreeBsdHWDiskStoreJNA.getDisks();
    }

    @Override
    protected List<Display> createDisplays() {
        return UnixDisplay.getDisplays();
    }

    @Override
    public List<NetworkIF> getNetworkIFs(boolean includeLocalInterfaces) {
        return BsdNetworkIF.getNetworks(includeLocalInterfaces);
    }

    @Override
    protected List<UsbDevice> createUsbDevices() {
        return FreeBsdUsbDevice.getUsbDevices(true);
    }

    @Override
    protected List<SoundCard> createSoundCards() {
        return FreeBsdSoundCard.getSoundCards();
    }

    @Override
    protected List<GraphicsCard> createGraphicsCards() {
        return FreeBsdGraphicsCard.getGraphicsCards();
    }

    @Override
    public List<Printer> getPrinters() {
        return CupsPrinterJNA.getPrinters();
    }
}
