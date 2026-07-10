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
import oshi.hardware.platform.unix.CupsPrinterFFM;
import oshi.hardware.platform.unix.freebsd.FreeBsdComputerSystemFFM;
import oshi.hardware.platform.unix.freebsd.FreeBsdGlobalMemoryFFM;
import oshi.hardware.platform.unix.freebsd.FreeBsdHWDiskStoreFFM;
import oshi.hardware.platform.unix.freebsd.FreeBsdPowerSourceFFM;
import oshi.hardware.platform.unix.freebsd.FreeBsdSensorsFFM;

/**
 * FFM-backed DragonFly BSD HAL. Uses FreeBSD FFM implementations where behavior is identical; overrides
 * {@link #createProcessor()} to return the DragonFly-specific CPU implementation.
 */
@ThreadSafe
public final class DragonFlyBsdHardwareAbstractionLayerFFM extends AbstractHardwareAbstractionLayer {

    @Override
    public ComputerSystem createComputerSystem() {
        return new FreeBsdComputerSystemFFM();
    }

    @Override
    public GlobalMemory createMemory() {
        return new FreeBsdGlobalMemoryFFM();
    }

    @Override
    public CentralProcessor createProcessor() {
        return new DragonFlyBsdCentralProcessorFFM();
    }

    @Override
    public Sensors createSensors() {
        return new FreeBsdSensorsFFM();
    }

    @Override
    public List<PowerSource> getPowerSources() {
        return FreeBsdPowerSourceFFM.getPowerSources();
    }

    @Override
    public List<HWDiskStore> getDiskStores() {
        return FreeBsdHWDiskStoreFFM.getDisks();
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
        return CupsPrinterFFM.getPrinters();
    }
}
