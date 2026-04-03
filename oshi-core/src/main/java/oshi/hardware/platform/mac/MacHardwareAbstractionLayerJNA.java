/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.ComputerSystem;
import oshi.hardware.Display;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;
import oshi.hardware.NetworkIF;
import oshi.hardware.PowerSource;
import oshi.hardware.Sensors;
import oshi.hardware.UsbDevice;

/**
 * MacHardwareAbstractionLayer JNA implementation.
 */
@ThreadSafe
public final class MacHardwareAbstractionLayerJNA extends MacHardwareAbstractionLayer {

    @Override
    public ComputerSystem createComputerSystem() {
        return new MacComputerSystem();
    }

    @Override
    public GlobalMemory createMemory() {
        return new MacGlobalMemoryJNA();
    }

    @Override
    public Sensors createSensors() {
        return new MacSensors();
    }

    @Override
    public List<PowerSource> getPowerSources() {
        return MacPowerSource.getPowerSources();
    }

    @Override
    public List<HWDiskStore> getDiskStores() {
        return MacHWDiskStore.getDisks();
    }

    @Override
    public List<Display> getDisplays() {
        return MacDisplay.getDisplays();
    }

    @Override
    public List<NetworkIF> getNetworkIFs(boolean includeLocalInterfaces) {
        return MacNetworkIF.getNetworks(includeLocalInterfaces);
    }

    @Override
    public List<UsbDevice> getUsbDevices(boolean tree) {
        return MacUsbDevice.getUsbDevices(tree);
    }
}
