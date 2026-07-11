/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.solaris;

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
import oshi.hardware.common.platform.unix.solaris.SolarisComputerSystem;
import oshi.hardware.common.platform.unix.solaris.SolarisGraphicsCard;
import oshi.hardware.common.platform.unix.solaris.SolarisSensors;
import oshi.hardware.common.platform.unix.solaris.SolarisSoundCard;
import oshi.hardware.common.platform.unix.solaris.SolarisUsbDevice;
import oshi.hardware.platform.unix.CupsPrinterFFM;

@ThreadSafe
public final class SolarisHardwareAbstractionLayerFFM extends AbstractHardwareAbstractionLayer {

    @Override
    public ComputerSystem createComputerSystem() {
        return new SolarisComputerSystem();
    }

    @Override
    public GlobalMemory createMemory() {
        return new SolarisGlobalMemoryFFM();
    }

    @Override
    public CentralProcessor createProcessor() {
        return new SolarisCentralProcessorFFM();
    }

    @Override
    public Sensors createSensors() {
        return new SolarisSensors();
    }

    @Override
    public List<PowerSource> getPowerSources() {
        return SolarisPowerSourceFFM.getPowerSources();
    }

    @Override
    public List<HWDiskStore> getDiskStores() {
        return SolarisHWDiskStoreFFM.getDisks();
    }

    @Override
    protected List<Display> createDisplays() {
        return UnixDisplay.getDisplays();
    }

    @Override
    public List<NetworkIF> getNetworkIFs(boolean includeLocalInterfaces) {
        return SolarisNetworkIFFFM.getNetworks(includeLocalInterfaces);
    }

    @Override
    protected List<UsbDevice> createUsbDevices() {
        return SolarisUsbDevice.getUsbDevices();
    }

    @Override
    protected List<SoundCard> createSoundCards() {
        return SolarisSoundCard.getSoundCards();
    }

    @Override
    protected List<GraphicsCard> createGraphicsCards() {
        return SolarisGraphicsCard.getGraphicsCards();
    }

    @Override
    public List<Printer> getPrinters() {
        return CupsPrinterFFM.getPrinters();
    }
}
