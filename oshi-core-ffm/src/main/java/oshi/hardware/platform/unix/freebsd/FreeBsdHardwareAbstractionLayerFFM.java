/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.freebsd;

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

/**
 * FFM-backed FreeBSD hardware abstraction layer. Wires the FFM HAL concretes shipped in Phase 4/5; the pure-Java
 * BSD-shared display/network/USB/sound/graphics classes come from {@code oshi-common}.
 */
@ThreadSafe
public final class FreeBsdHardwareAbstractionLayerFFM extends AbstractHardwareAbstractionLayer {

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
        return new FreeBsdCentralProcessorFFM();
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
        return CupsPrinterFFM.getPrinters();
    }
}
