/*
 * Copyright 2025-2026 The OSHI Project Contributors
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
import oshi.hardware.platform.unix.CupsPrinterFFM;

@ThreadSafe
public final class MacHardwareAbstractionLayerFFM extends MacHardwareAbstractionLayer {
    @Override
    public List<Printer> getPrinters() {
        return CupsPrinterFFM.getPrinters();
    }

    @Override
    public CentralProcessor createProcessor() {
        return new MacCentralProcessorFFM();
    }

    @Override
    public GlobalMemory createMemory() {
        return new MacGlobalMemoryFFM();
    }

    @Override
    public ComputerSystem createComputerSystem() {
        return new MacComputerSystemFFM();
    }

    @Override
    public Sensors createSensors() {
        return new MacSensorsFFM();
    }

    @Override
    public List<PowerSource> getPowerSources() {
        return MacPowerSourceFFM.getPowerSources();
    }

    @Override
    public List<HWDiskStore> getDiskStores() {
        return MacHWDiskStoreFFM.getDisks();
    }

    @Override
    protected List<Display> createDisplays() {
        return MacDisplayFFM.getDisplays();
    }

    @Override
    public List<NetworkIF> getNetworkIFs(boolean includeLocalInterfaces) {
        return MacNetworkIfFFM.getNetworks(includeLocalInterfaces);
    }

    @Override
    protected List<UsbDevice> createUsbDevices() {
        return MacUsbDeviceFFM.getUsbDevices(true);
    }

    @Override
    protected List<GraphicsCard> createGraphicsCards() {
        return MacGraphicsCardFFM.getGraphicsCards();
    }
}
