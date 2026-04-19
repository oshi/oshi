/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.ComputerSystem;
import oshi.hardware.GlobalMemory;
import oshi.hardware.LogicalVolumeGroup;
import oshi.hardware.NetworkIF;
import oshi.hardware.PowerSource;
import oshi.hardware.Printer;
import oshi.hardware.Sensors;
import oshi.hardware.SoundCard;

/**
 * FFM-based hardware abstraction layer for Windows. Extends {@link WindowsHardwareAbstractionLayer}, overriding methods
 * as FFM implementations become available.
 */
@ThreadSafe
public final class WindowsHardwareAbstractionLayerFFM extends WindowsHardwareAbstractionLayer {

    @Override
    public ComputerSystem createComputerSystem() {
        return new WindowsComputerSystemFFM();
    }

    @Override
    public GlobalMemory createMemory() {
        return new WindowsGlobalMemoryFFM();
    }

    @Override
    public Sensors createSensors() {
        return new WindowsSensorsFFM();
    }

    @Override
    public List<PowerSource> getPowerSources() {
        return WindowsPowerSourceFFM.getPowerSources();
    }

    @Override
    public List<LogicalVolumeGroup> getLogicalVolumeGroups() {
        return WindowsLogicalVolumeGroupFFM.getLogicalVolumeGroups();
    }

    @Override
    public List<NetworkIF> getNetworkIFs(boolean includeLocalInterfaces) {
        return WindowsNetworkIfFFM.getNetworks(includeLocalInterfaces);
    }

    @Override
    public List<SoundCard> getSoundCards() {
        return WindowsSoundCardFFM.getSoundCards();
    }

    @Override
    public List<Printer> getPrinters() {
        return WindowsPrinterFFM.getPrinters();
    }
}
