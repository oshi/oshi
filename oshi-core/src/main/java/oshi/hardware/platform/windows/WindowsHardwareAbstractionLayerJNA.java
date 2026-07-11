/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import java.util.List;

import com.sun.jna.platform.win32.Guid.GUID;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.windows.DeviceTree;
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
 * WindowsHardwareAbstractionLayer class.
 */
@ThreadSafe
public class WindowsHardwareAbstractionLayerJNA extends AbstractHardwareAbstractionLayer {

    @Override
    public ComputerSystem createComputerSystem() {
        return new WindowsComputerSystemJNA();
    }

    @Override
    public GlobalMemory createMemory() {
        return new WindowsGlobalMemoryJNA();
    }

    @Override
    public CentralProcessor createProcessor() {
        return new WindowsCentralProcessorJNA();
    }

    @Override
    public Sensors createSensors() {
        return new WindowsSensorsJNA();
    }

    @Override
    public List<PowerSource> getPowerSources() {
        return WindowsPowerSourceJNA.getPowerSources();
    }

    @Override
    public List<HWDiskStore> getDiskStores() {
        return WindowsHWDiskStoreJNA.getDisks();
    }

    @Override
    public List<LogicalVolumeGroup> getLogicalVolumeGroups() {
        return WindowsLogicalVolumeGroupJNA.getLogicalVolumeGroups();
    }

    @Override
    protected List<Display> createDisplays() {
        return WindowsDisplayJNA.getDisplays();
    }

    @Override
    public List<NetworkIF> getNetworkIFs(boolean includeLocalInterfaces) {
        return WindowsNetworkIfJNA.getNetworks(includeLocalInterfaces);
    }

    private static final GUID GUID_DEVINTERFACE_USB_HOST_CONTROLLER = new GUID(
            "{3ABF6F2D-71C4-462A-8A92-1E6861E6AF27}");

    @Override
    protected List<UsbDevice> createUsbDevices() {
        return WindowsUsbDevice.getUsbDevices(() -> DeviceTree.queryDeviceTree(GUID_DEVINTERFACE_USB_HOST_CONTROLLER));
    }

    @Override
    protected List<SoundCard> createSoundCards() {
        return WindowsSoundCardJNA.getSoundCards();
    }

    @Override
    protected List<GraphicsCard> createGraphicsCards() {
        return WindowsGraphicsCardJNA.getGraphicsCards();
    }

    @Override
    public List<Printer> getPrinters() {
        return WindowsPrinterJNA.getPrinters();
    }

    @Override
    public List<BluetoothDevice> getBluetoothDevices() {
        return WindowsBluetoothDeviceJNA.getBluetoothDevices();
    }
}
