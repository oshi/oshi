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
import oshi.hardware.common.platform.unix.UnixDisplay;
import oshi.hardware.platform.unix.BsdNetworkIF;
import oshi.hardware.platform.unix.CupsPrinterJNA;
import oshi.hardware.platform.unix.freebsd.FreeBsdComputerSystem;
import oshi.hardware.platform.unix.freebsd.FreeBsdGlobalMemory;
import oshi.hardware.platform.unix.freebsd.FreeBsdGraphicsCard;
import oshi.hardware.platform.unix.freebsd.FreeBsdHWDiskStore;
import oshi.hardware.platform.unix.freebsd.FreeBsdPowerSource;
import oshi.hardware.platform.unix.freebsd.FreeBsdSensors;
import oshi.hardware.platform.unix.freebsd.FreeBsdSoundCard;
import oshi.hardware.platform.unix.freebsd.FreeBsdUsbDevice;

/**
 * DragonFlyBsdHardwareAbstractionLayer class. Uses FreeBSD implementations where behavior is identical.
 */
@ThreadSafe
public final class DragonFlyBsdHardwareAbstractionLayer extends AbstractHardwareAbstractionLayer {

    @Override
    public ComputerSystem createComputerSystem() {
        return new FreeBsdComputerSystem();
    }

    @Override
    public GlobalMemory createMemory() {
        return new FreeBsdGlobalMemory();
    }

    @Override
    public CentralProcessor createProcessor() {
        return new DragonFlyBsdCentralProcessor();
    }

    @Override
    public Sensors createSensors() {
        return new FreeBsdSensors();
    }

    @Override
    public List<PowerSource> getPowerSources() {
        return FreeBsdPowerSource.getPowerSources();
    }

    @Override
    public List<HWDiskStore> getDiskStores() {
        return FreeBsdHWDiskStore.getDisks();
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
        return FreeBsdUsbDevice.getUsbDevices(tree);
    }

    @Override
    public List<SoundCard> getSoundCards() {
        return FreeBsdSoundCard.getSoundCards();
    }

    @Override
    public List<GraphicsCard> getGraphicsCards() {
        return FreeBsdGraphicsCard.getGraphicsCards();
    }

    @Override
    public List<Printer> getPrinters() {
        return CupsPrinterJNA.getPrinters();
    }
}
