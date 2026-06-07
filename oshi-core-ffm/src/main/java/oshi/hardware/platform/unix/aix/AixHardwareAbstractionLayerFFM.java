/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.aix;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.List;
import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.unix.aix.Lscfg;
import oshi.ffm.driver.unix.aix.perfstat.PerfstatDiskFFM;
import oshi.ffm.unix.CupsPrinterFFM;
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
import oshi.hardware.common.platform.unix.aix.AixComputerSystem;
import oshi.hardware.common.platform.unix.aix.AixGraphicsCard;
import oshi.hardware.common.platform.unix.aix.AixPowerSource;
import oshi.hardware.common.platform.unix.aix.AixSensors;
import oshi.hardware.common.platform.unix.aix.AixSoundCard;
import oshi.hardware.common.platform.unix.aix.AixUsbDevice;

/**
 * FFM-backed AIX HardwareAbstractionLayer.
 */
@ThreadSafe
public final class AixHardwareAbstractionLayerFFM extends AbstractHardwareAbstractionLayer {

    private final Supplier<List<String>> lscfg = memoize(Lscfg::queryAllDevices, defaultExpiration());
    private final Supplier<PerfstatDiskFFM.Disk[]> diskStats = memoize(PerfstatDiskFFM::queryDiskStats,
            defaultExpiration());

    @Override
    public ComputerSystem createComputerSystem() {
        return new AixComputerSystem(lscfg);
    }

    @Override
    public GlobalMemory createMemory() {
        return new AixGlobalMemoryFFM(lscfg);
    }

    @Override
    public CentralProcessor createProcessor() {
        return new AixCentralProcessorFFM();
    }

    @Override
    public Sensors createSensors() {
        return new AixSensors(lscfg);
    }

    @Override
    public List<PowerSource> getPowerSources() {
        return AixPowerSource.getPowerSources();
    }

    @Override
    public List<HWDiskStore> getDiskStores() {
        return AixHWDiskStoreFFM.getDisks(diskStats);
    }

    @Override
    public List<Display> getDisplays() {
        return UnixDisplay.getDisplays();
    }

    @Override
    public List<NetworkIF> getNetworkIFs(boolean includeLocalInterfaces) {
        return AixNetworkIFFFM.getNetworks(includeLocalInterfaces);
    }

    @Override
    public List<UsbDevice> getUsbDevices(boolean tree) {
        return AixUsbDevice.getUsbDevices(tree, lscfg);
    }

    @Override
    public List<SoundCard> getSoundCards() {
        return AixSoundCard.getSoundCards(lscfg);
    }

    @Override
    public List<GraphicsCard> getGraphicsCards() {
        return AixGraphicsCard.getGraphicsCards(lscfg);
    }

    @Override
    public List<Printer> getPrinters() {
        return CupsPrinterFFM.getPrinters();
    }
}
