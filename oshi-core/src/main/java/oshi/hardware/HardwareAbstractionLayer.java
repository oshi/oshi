/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import java.util.Collections;
import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * A hardware abstraction layer. Provides access to hardware items such as processors, memory, battery, and disks.
 */
@ThreadSafe
public interface HardwareAbstractionLayer {

    /**
     * Instantiates a {@link oshi.hardware.ComputerSystem} object. This represents the physical hardware, including
     * components such as BIOS/Firmware and a motherboard, logic board, etc.
     *
     * @return a {@link oshi.hardware.ComputerSystem} object.
     */
    ComputerSystem getComputerSystem();

    /**
     * Instantiates a {@link oshi.hardware.CentralProcessor} object. This represents one or more Logical CPUs.
     *
     * @return A {@link oshi.hardware.CentralProcessor} object.
     */
    CentralProcessor getProcessor();

    /**
     * Instantiates a {@link oshi.hardware.GlobalMemory} object.
     *
     * @return A memory object.
     */
    GlobalMemory getMemory();

    /**
     * Instantiates a list of {@link oshi.hardware.PowerSource} objects, representing batteries, etc.
     *
     * @return A list of PowerSource objects or an empty list if none are present.
     */
    List<PowerSource> getPowerSources();

    /**
     * Instantiates a list of {@link oshi.hardware.HWDiskStore} objects, representing physical hard disks or other
     * similar storage devices.
     *
     * @return A list of HWDiskStore objects or an empty list if none are present.
     */
    List<HWDiskStore> getDiskStores();

    /**
     * Instantiates a list of {@link LogicalVolumeGroup} objects, representing a storage pool or group of devices,
     * partitions, volumes, or other implementation specific means of file storage.
     * <p>
     * If not yet implemented or if logical volume groups do not exist, returns an empty list.
     * <p>
     * Currently implemented for Linux (LVM2), macOS (Core Storage), and Windows (Storage Spaces).
     *
     * @return A list of {@link LogicalVolumeGroup} objects or an empty list if none are present.
     */
    default List<LogicalVolumeGroup> getLogicalVolumeGroups() {
        return Collections.emptyList();
    }

    /**
     * Gets a list of non-local {@link NetworkIF} objects, representing a network interface. The list excludes local
     * interfaces.
     *
     * @return A list of {@link NetworkIF} objects representing the interfaces
     */
    List<NetworkIF> getNetworkIFs();

    /**
     * Gets a list {@link NetworkIF} objects, representing a network interface.
     *
     * @param includeLocalInterfaces whether to include local interfaces (loopback or no hardware address) in the result
     * @return A list of {@link NetworkIF} objects representing the interfaces
     */
    List<NetworkIF> getNetworkIFs(boolean includeLocalInterfaces);

    /**
     * Instantiates a list of {@link oshi.hardware.Display} objects, representing monitors or other video output
     * devices.
     *
     * @return A list of Display objects or an empty list if none are present.
     */
    List<Display> getDisplays();

    /**
     * Instantiates a {@link oshi.hardware.Sensors} object, representing CPU temperature and fan speed.
     *
     * @return A Sensors object
     */
    Sensors getSensors();

    /**
     * Instantiates a list of {@link oshi.hardware.UsbDevice} objects, representing devices connected via a usb port
     * (including internal devices).
     * <p>
     * If the value of {@code tree} is true, the top level devices returned from this method are the USB Controllers;
     * connected hubs and devices in its device tree share that controller's bandwidth. If the value of {@code tree} is
     * false, USB devices (not controllers) are listed in a single flat list.
     * <p>
     * Note: in both cases each {@link UsbDevice} in the returned list may still report connected child devices via
     * {@link UsbDevice#getConnectedDevices()}. When {@code tree} is false the list is intended for simple iteration
     * over individual devices; callers should not recurse into {@link UsbDevice#getConnectedDevices()} or rely on
     * {@link Object#toString()} (which renders the full subtree) to avoid processing devices more than once.
     * <p>
     * To print the full device tree rooted at each controller:
     *
     * <pre>{@code
     * for (UsbDevice controller : hal.getUsbDevices(true)) {
     *     System.out.println(controller); // toString() renders the full subtree
     * }
     * }</pre>
     *
     * To iterate individual devices without tree structure:
     *
     * <pre>{@code
     * for (UsbDevice device : hal.getUsbDevices(false)) {
     *     // Use individual fields; avoid toString() as it includes the subtree
     *     System.out.println(device.getName() + " [" + device.getVendorId() + ":" + device.getProductId() + "]");
     * }
     * }</pre>
     *
     * @param tree If {@code true}, returns the USB Controllers as top-level entries, with connected hubs and devices
     *             accessible via {@link UsbDevice#getConnectedDevices()}. If {@code false}, returns a flat list of
     *             non-controller devices; {@link UsbDevice#getConnectedDevices()} may still be non-empty but should not
     *             be iterated to avoid duplicates.
     * @return A list of UsbDevice objects representing (optionally) the USB Controllers and devices connected to them,
     *         or an empty list if none are present
     */
    List<UsbDevice> getUsbDevices(boolean tree);

    /**
     * Instantiates a list of {@link oshi.hardware.SoundCard} objects, representing the Sound cards.
     *
     * @return A list of SoundCard objects or an empty list if none are present.
     */
    List<SoundCard> getSoundCards();

    /**
     * Instantiates a list of {@link oshi.hardware.GraphicsCard} objects, representing the Graphics cards.
     *
     * @return A list of objects or an empty list if none are present.
     */
    List<GraphicsCard> getGraphicsCards();

    /**
     * Instantiates a list of {@link oshi.hardware.Printer} objects, representing printers.
     *
     * @return A list of Printer objects or an empty list if none are present.
     */
    default List<Printer> getPrinters() {
        return Collections.emptyList();
    }
}
