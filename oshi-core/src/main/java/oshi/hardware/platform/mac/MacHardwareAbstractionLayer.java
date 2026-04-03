/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.CentralProcessor;
import oshi.hardware.Display;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HWDiskStore;
import oshi.hardware.LogicalVolumeGroup;
import oshi.hardware.NetworkIF;
import oshi.hardware.PowerSource;
import oshi.hardware.Printer;
import oshi.hardware.SoundCard;
import oshi.hardware.UsbDevice;

import oshi.hardware.common.AbstractHardwareAbstractionLayer;
import oshi.hardware.platform.unix.UnixPrinter;

/**
 * MacHardwareAbstractionLayer class.
 */
@ThreadSafe
public abstract class MacHardwareAbstractionLayer extends AbstractHardwareAbstractionLayer {

    @Override
    public CentralProcessor createProcessor() {
        return new MacCentralProcessor();
    }

    @Override
    public abstract List<PowerSource> getPowerSources();

    @Override
    public abstract List<HWDiskStore> getDiskStores();

    @Override
    public List<LogicalVolumeGroup> getLogicalVolumeGroups() {
        return MacLogicalVolumeGroup.getLogicalVolumeGroups();
    }

    @Override
    public abstract List<Display> getDisplays();

    @Override
    public abstract List<NetworkIF> getNetworkIFs(boolean includeLocalInterfaces);

    @Override
    public abstract List<UsbDevice> getUsbDevices(boolean tree);

    @Override
    public List<SoundCard> getSoundCards() {
        return MacSoundCard.getSoundCards();
    }

    @Override
    public List<GraphicsCard> getGraphicsCards() {
        return MacGraphicsCard.getGraphicsCards();
    }

    @Override
    public List<Printer> getPrinters() {
        return UnixPrinter.getPrinters();
    }
}
