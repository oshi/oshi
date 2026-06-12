/*
 * Copyright 2021-2026 The OSHI Project Contributors
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
import oshi.hardware.common.platform.unix.BsdNetworkIF;
import oshi.hardware.common.platform.unix.BsdPowerSource;
import oshi.hardware.common.platform.unix.UnixDisplay;
import oshi.hardware.common.platform.unix.openbsd.OpenBsdGraphicsCard;
import oshi.hardware.common.platform.unix.openbsd.OpenBsdSensors;
import oshi.hardware.common.platform.unix.openbsd.OpenBsdSoundCard;
import oshi.hardware.common.platform.unix.openbsd.OpenBsdUsbDevice;
import oshi.hardware.platform.unix.CupsPrinterJNA;

/**
 * OpenBsdHardwareAbstractionLayerJNA class.
 */
@ThreadSafe
public final class OpenBsdHardwareAbstractionLayerJNA extends AbstractHardwareAbstractionLayer {

    @Override
    public ComputerSystem createComputerSystem() {
        return new OpenBsdComputerSystemJNA();
    }

    @Override
    public GlobalMemory createMemory() {
        return new OpenBsdGlobalMemoryJNA();
    }

    @Override
    public CentralProcessor createProcessor() {
        return new OpenBsdCentralProcessorJNA();
    }

    @Override
    public Sensors createSensors() {
        return new OpenBsdSensors();
    }

    @Override
    public List<PowerSource> getPowerSources() {
        return BsdPowerSource.getPowerSources();
    }

    @Override
    public List<HWDiskStore> getDiskStores() {
        return OpenBsdHWDiskStoreJNA.getDisks();
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

    @Override
    public List<Printer> getPrinters() {
        return CupsPrinterJNA.getPrinters();
    }
}
