/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.windows.DeviceTreeFFM;
import oshi.hardware.BluetoothDevice;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.Display;
import oshi.hardware.GlobalMemory;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HWDiskStore;
import oshi.hardware.LogicalVolumeGroup;
import oshi.hardware.NetworkIF;
import oshi.hardware.PowerSource;
import oshi.hardware.Printer;
import oshi.hardware.Sensors;
import oshi.hardware.SoundCard;
import oshi.hardware.UsbDevice;
import oshi.hardware.common.AbstractHardwareAbstractionLayer;
import oshi.hardware.common.platform.windows.WindowsUsbDevice;

/**
 * FFM-based hardware abstraction layer for Windows. Extends {@link AbstractHardwareAbstractionLayer} with pure-FFM
 * implementations for all Windows hardware components.
 */
@ThreadSafe
public final class WindowsHardwareAbstractionLayerFFM extends AbstractHardwareAbstractionLayer {

    @Override
    public CentralProcessor createProcessor() {
        return new WindowsCentralProcessorFFM();
    }

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
    protected List<GraphicsCard> createGraphicsCards() {
        return WindowsGraphicsCardFFM.getGraphicsCards();
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
    protected List<Display> createDisplays() {
        return WindowsDisplayFFM.getDisplays();
    }

    // GUID_DEVINTERFACE_USB_HOST_CONTROLLER {3ABF6F2D-71C4-462A-8A92-1E6861E6AF27}
    private static final byte[] GUID_DEVINTERFACE_USB_HOST_CONTROLLER = { 0x2D, 0x6F, (byte) 0xBF, 0x3A, (byte) 0xC4,
            0x71, 0x2A, 0x46, (byte) 0x8A, (byte) 0x92, 0x1E, 0x68, 0x61, (byte) 0xE6, (byte) 0xAF, 0x27 };

    @Override
    protected List<UsbDevice> createUsbDevices() {
        return WindowsUsbDevice
                .getUsbDevices(() -> DeviceTreeFFM.queryDeviceTree(GUID_DEVINTERFACE_USB_HOST_CONTROLLER));
    }

    @Override
    public List<HWDiskStore> getDiskStores() {
        return WindowsHWDiskStoreFFM.getDisks();
    }

    @Override
    protected List<SoundCard> createSoundCards() {
        return WindowsSoundCardFFM.getSoundCards();
    }

    @Override
    public List<Printer> getPrinters() {
        return WindowsPrinterFFM.getPrinters();
    }

    @Override
    public List<BluetoothDevice> getBluetoothDevices() {
        return WindowsBluetoothDeviceFFM.getBluetoothDevices();
    }
}
