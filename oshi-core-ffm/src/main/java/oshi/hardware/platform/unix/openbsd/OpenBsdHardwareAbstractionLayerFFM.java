/*
 * Copyright 2026 The OSHI Project Contributors
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
import oshi.hardware.Printer;
import oshi.hardware.Sensors;
import oshi.hardware.SoundCard;
import oshi.hardware.UsbDevice;
import oshi.hardware.common.AbstractHardwareAbstractionLayer;
import oshi.hardware.common.platform.unix.BsdGraphicsCard;
import oshi.hardware.common.platform.unix.BsdNetworkIF;
import oshi.hardware.common.platform.unix.BsdPowerSource;
import oshi.hardware.common.platform.unix.BsdSensors;
import oshi.hardware.common.platform.unix.BsdSoundCard;
import oshi.hardware.common.platform.unix.BsdUsbDevice;
import oshi.hardware.common.platform.unix.UnixDisplay;
import oshi.hardware.platform.unix.CupsPrinterFFM;

@ThreadSafe
public final class OpenBsdHardwareAbstractionLayerFFM extends AbstractHardwareAbstractionLayer {

    @Override
    public ComputerSystem createComputerSystem() {
        return new OpenBsdComputerSystemFFM();
    }

    @Override
    public GlobalMemory createMemory() {
        return new OpenBsdGlobalMemoryFFM();
    }

    @Override
    public CentralProcessor createProcessor() {
        return new OpenBsdCentralProcessorFFM();
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
        return OpenBsdHWDiskStoreFFM.getDisks();
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
        return BsdUsbDevice.getUsbDevices(true);
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
        return CupsPrinterFFM.getPrinters();
    }
}
