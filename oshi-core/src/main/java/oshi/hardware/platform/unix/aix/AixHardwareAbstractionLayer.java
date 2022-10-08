/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.aix;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.List;
import java.util.function.Supplier;

import com.sun.jna.platform.unix.aix.Perfstat.perfstat_disk_t;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.aix.Lscfg;
import oshi.driver.unix.aix.perfstat.PerfstatDisk;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.Display;
import oshi.hardware.GlobalMemory;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HWDiskStore;
import oshi.hardware.NetworkIF;
import oshi.hardware.PowerSource;
import oshi.hardware.Sensors;
import oshi.hardware.SoundCard;
import oshi.hardware.UsbDevice;
import oshi.hardware.common.AbstractHardwareAbstractionLayer;
import oshi.hardware.platform.unix.UnixDisplay;

/**
 * AIXHardwareAbstractionLayer class.
 */
@ThreadSafe
public final class AixHardwareAbstractionLayer extends AbstractHardwareAbstractionLayer {

    // Memoized hardware listing
    private final Supplier<List<String>> lscfg = memoize(Lscfg::queryAllDevices, defaultExpiration());
    // Memoized disk stats to pass to disk object(s)
    private final Supplier<perfstat_disk_t[]> diskStats = memoize(PerfstatDisk::queryDiskStats, defaultExpiration());

    @Override
    public ComputerSystem createComputerSystem() {
        return new AixComputerSystem(lscfg);
    }

    @Override
    public GlobalMemory createMemory() {
        return new AixGlobalMemory(lscfg);
    }

    @Override
    public CentralProcessor createProcessor() {
        return new AixCentralProcessor();
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
        return AixHWDiskStore.getDisks(diskStats);
    }

    @Override
    public List<Display> getDisplays() {
        return UnixDisplay.getDisplays();
    }

    @Override
    public List<NetworkIF> getNetworkIFs(boolean includeLocalInterfaces) {
        return AixNetworkIF.getNetworks(includeLocalInterfaces);
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
}
