/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.BluetoothDevice;
import oshi.hardware.common.AbstractBluetoothDevice;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * macOS Bluetooth device enumeration via {@code system_profiler SPBluetoothDataType}.
 */
@Immutable
public final class MacBluetoothDevice extends AbstractBluetoothDevice {

    private MacBluetoothDevice(String name, String address, String majorDeviceClass, boolean connected, boolean paired,
            int batteryLevel, String adapterName) {
        super(name, address, majorDeviceClass, connected, paired, batteryLevel, adapterName);
    }

    /**
     * Gets Bluetooth devices known to the system.
     *
     * @return a list of {@link BluetoothDevice} objects
     */
    public static List<BluetoothDevice> getBluetoothDevices() {
        return parseSystemProfiler(ExecutingCommand.runNative("system_profiler SPBluetoothDataType"));
    }

    /**
     * Parses the output of {@code system_profiler SPBluetoothDataType}.
     *
     * @param lines the output lines
     * @return a list of Bluetooth devices
     */
    static List<BluetoothDevice> parseSystemProfiler(List<String> lines) {
        List<BluetoothDevice> devices = new ArrayList<>();
        boolean inConnected = false;
        boolean inNotConnected = false;
        boolean inDevice = false;
        String name = "";
        String address = "";
        String majorClass = "";
        int batteryLevel = -1;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            // Section headers (indented with 6 spaces in system_profiler output)
            if (trimmed.startsWith("Connected:") || trimmed.equals("Devices (Paired, Configured, & Connected):")) {
                if (inDevice) {
                    devices.add(new MacBluetoothDevice(name, address, majorClass, inConnected, true, batteryLevel, ""));
                }
                inConnected = true;
                inNotConnected = false;
                inDevice = false;
                continue;
            }
            if (trimmed.startsWith("Not Connected:") || trimmed.equals("Devices (Paired, Not Connected):")) {
                if (inDevice) {
                    devices.add(new MacBluetoothDevice(name, address, majorClass, inConnected, true, batteryLevel, ""));
                }
                inConnected = false;
                inNotConnected = true;
                inDevice = false;
                continue;
            }
            if (!inConnected && !inNotConnected) {
                continue;
            }

            // Device entries are names followed by a colon at a certain indent level
            // Properties are key: value pairs at deeper indent
            String[] parts = trimmed.split(":", 2);
            if (parts.length == 2) {
                String key = parts[0].trim();
                String value = parts[1].trim();

                if (value.isEmpty() && !key.startsWith("Address") && !key.startsWith("Minor")) {
                    // This is a device name header
                    if (inDevice) {
                        devices.add(
                                new MacBluetoothDevice(name, address, majorClass, inConnected, true, batteryLevel, ""));
                    }
                    inDevice = true;
                    name = key;
                    address = "";
                    majorClass = "";
                    batteryLevel = -1;
                } else if (inDevice) {
                    switch (key.toLowerCase(Locale.ROOT)) {
                        case "address":
                            address = value.toUpperCase(Locale.ROOT);
                            break;
                        case "major type":
                        case "minor type":
                            majorClass = value;
                            break;
                        case "battery level":
                            String digits = value.replace("%", "").trim();
                            batteryLevel = ParseUtil.parseIntOrDefault(digits, -1);
                            break;
                        default:
                            break;
                    }
                }
            }
        }
        // Add last device
        if (inDevice) {
            devices.add(new MacBluetoothDevice(name, address, majorClass, inConnected, true, batteryLevel, ""));
        }
        return Collections.unmodifiableList(devices);
    }
}
