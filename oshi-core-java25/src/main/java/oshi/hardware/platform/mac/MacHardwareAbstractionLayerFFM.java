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
import oshi.hardware.GraphicsCard;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;
import oshi.hardware.NetworkIF;
import oshi.hardware.PowerSource;
import oshi.hardware.Sensors;
import oshi.hardware.UsbDevice;

@ThreadSafe
public final class MacHardwareAbstractionLayerFFM extends MacHardwareAbstractionLayer {
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
    public List<Display> getDisplays() {
        return MacDisplayFFM.getDisplays();
    }

    @Override
    public List<NetworkIF> getNetworkIFs(boolean includeLocalInterfaces) {
        return MacNetworkIFFM.getNetworks(includeLocalInterfaces);
    }

    @Override
    public List<UsbDevice> getUsbDevices(boolean tree) {
        return MacUsbDeviceFFM.getUsbDevices(tree);
    }

    @Override
    public List<GraphicsCard> getGraphicsCards() {
        return MacGraphicsCardFFM.getGraphicsCards();
    }
}
