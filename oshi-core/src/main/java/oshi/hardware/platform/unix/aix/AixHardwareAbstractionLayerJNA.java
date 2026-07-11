/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.aix;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.List;
import java.util.function.Supplier;

import com.sun.jna.platform.unix.aix.Perfstat.perfstat_disk_t;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.unix.aix.Lscfg;
import oshi.driver.unix.aix.perfstat.PerfstatDiskJNA;
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
import oshi.hardware.platform.unix.CupsPrinterJNA;

/**
 * JNA-backed AIX HardwareAbstractionLayer.
 */
@ThreadSafe
public final class AixHardwareAbstractionLayerJNA extends AbstractHardwareAbstractionLayer {

    private final Supplier<List<String>> lscfg = memoize(Lscfg::queryAllDevices, defaultExpiration());
    private final Supplier<perfstat_disk_t[]> diskStats = memoize(PerfstatDiskJNA::queryDiskStats, defaultExpiration());

    @Override
    public ComputerSystem createComputerSystem() {
        return new AixComputerSystem(lscfg);
    }

    @Override
    public GlobalMemory createMemory() {
        return new AixGlobalMemoryJNA(lscfg);
    }

    @Override
    public CentralProcessor createProcessor() {
        return new AixCentralProcessorJNA();
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
        return AixHWDiskStoreJNA.getDisks(diskStats);
    }

    @Override
    protected List<Display> createDisplays() {
        return UnixDisplay.getDisplays();
    }

    @Override
    public List<NetworkIF> getNetworkIFs(boolean includeLocalInterfaces) {
        return AixNetworkIFJNA.getNetworks(includeLocalInterfaces);
    }

    @Override
    protected List<UsbDevice> createUsbDevices() {
        return AixUsbDevice.getUsbDevices(lscfg);
    }

    @Override
    protected List<SoundCard> createSoundCards() {
        return AixSoundCard.getSoundCards(lscfg);
    }

    @Override
    protected List<GraphicsCard> createGraphicsCards() {
        return AixGraphicsCard.getGraphicsCards(lscfg);
    }

    @Override
    public List<Printer> getPrinters() {
        return CupsPrinterJNA.getPrinters();
    }
}
