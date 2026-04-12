/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

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
import oshi.hardware.UsbDevice;
import oshi.hardware.common.platform.mac.MacHardwareAbstractionLayer;
import oshi.hardware.platform.unix.UnixPrinter;

/**
 * MacHardwareAbstractionLayer JNA implementation.
 */
@ThreadSafe
public final class MacHardwareAbstractionLayerJNA extends MacHardwareAbstractionLayer {

    @Override
    public ComputerSystem createComputerSystem() {
        return new MacComputerSystemJNA();
    }

    @Override
    public CentralProcessor createProcessor() {
        return new MacCentralProcessorJNA();
    }

    @Override
    public GlobalMemory createMemory() {
        return new MacGlobalMemoryJNA();
    }

    @Override
    public Sensors createSensors() {
        return new MacSensorsJNA();
    }

    @Override
    public List<PowerSource> getPowerSources() {
        return MacPowerSourceJNA.getPowerSources();
    }

    @Override
    public List<HWDiskStore> getDiskStores() {
        return MacHWDiskStoreJNA.getDisks();
    }

    @Override
    public List<Display> getDisplays() {
        return MacDisplayJNA.getDisplays();
    }

    @Override
    public List<NetworkIF> getNetworkIFs(boolean includeLocalInterfaces) {
        return MacNetworkIfJNA.getNetworks(includeLocalInterfaces);
    }

    @Override
    public List<UsbDevice> getUsbDevices(boolean tree) {
        return MacUsbDeviceJNA.getUsbDevices(tree);
    }

    @Override
    public List<GraphicsCard> getGraphicsCards() {
        return MacGraphicsCardJNA.getGraphicsCards();
    }

    @Override
    public List<Printer> getPrinters() {
        return UnixPrinter.getPrinters();
    }
}
