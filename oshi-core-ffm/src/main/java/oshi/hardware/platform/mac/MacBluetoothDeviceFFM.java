/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import oshi.annotation.concurrent.Immutable;
import oshi.ffm.mac.IOKit.IOIterator;
import oshi.ffm.mac.IOKit.IORegistryEntry;
import oshi.ffm.util.platform.mac.IOKitUtilFFM;
import oshi.hardware.BluetoothDevice;
import oshi.hardware.common.AbstractBluetoothDevice;

/**
 * macOS Bluetooth device enumeration via FFM/IOKit (IOBluetoothDevice).
 */
@Immutable
public final class MacBluetoothDeviceFFM extends AbstractBluetoothDevice {

    private static final String IO_BLUETOOTH_DEVICE = "IOBluetoothDevice";

    private MacBluetoothDeviceFFM(String name, String address, String majorDeviceClass, boolean connected,
            boolean paired, int batteryLevel, String adapterName) {
        super(name, address, majorDeviceClass, connected, paired, batteryLevel, adapterName);
    }

    /**
     * Gets Bluetooth devices known to the system via IOKit.
     *
     * @return a list of {@link BluetoothDevice} objects
     */
    public static List<BluetoothDevice> getBluetoothDevices() {
        List<BluetoothDevice> devices = new ArrayList<>();
        IOIterator iter = IOKitUtilFFM.getMatchingServices(IO_BLUETOOTH_DEVICE);
        if (iter == null) {
            return Collections.emptyList();
        }
        IORegistryEntry device = iter.next();
        while (device != null) {
            String name = device.getStringProperty("Name");
            if (name == null) {
                name = device.getName();
            }
            String address = device.getStringProperty("DeviceAddress");
            if (address == null) {
                address = "";
            } else {
                address = formatAddress(address);
            }
            Long cod = device.getLongProperty("ClassOfDevice");
            String majorClass = parseMajorDeviceClass(cod != null ? cod.intValue() : 0);
            Long connectedVal = device.getLongProperty("Connected");
            boolean connected = connectedVal != null && connectedVal != 0;
            Long pairedVal = device.getLongProperty("Paired");
            boolean paired = pairedVal != null && pairedVal != 0;
            Long batteryVal = device.getLongProperty("BatteryPercent");
            int batteryLevel = batteryVal != null ? batteryVal.intValue() : -1;

            devices.add(new MacBluetoothDeviceFFM(name, address, majorClass, connected, paired, batteryLevel, ""));
            device.release();
            device = iter.next();
        }
        iter.release();
        return Collections.unmodifiableList(devices);
    }

    private static String formatAddress(String raw) {
        String cleaned = raw.replace("-", "").replace(":", "").trim();
        if (cleaned.length() == 12) {
            return String.format(Locale.ROOT, "%s:%s:%s:%s:%s:%s", cleaned.substring(0, 2), cleaned.substring(2, 4),
                    cleaned.substring(4, 6), cleaned.substring(6, 8), cleaned.substring(8, 10),
                    cleaned.substring(10, 12)).toUpperCase(Locale.ROOT);
        }
        return raw.toUpperCase(Locale.ROOT);
    }
}
