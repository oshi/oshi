/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.BluetoothDevice;
import oshi.hardware.common.AbstractBluetoothDevice;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.linux.SysPath;

/**
 * Linux Bluetooth device enumeration via BlueZ filesystem paths.
 * <p>
 * Adapters are discovered from {@code /sys/class/bluetooth/hciX}. Paired devices are read from
 * {@code /var/lib/bluetooth/<adapter-mac>/<device-mac>/info}.
 */
@Immutable
public final class LinuxBluetoothDevice extends AbstractBluetoothDevice {

    private static final String SYS_BLUETOOTH = SysPath.SYS + "class/bluetooth/";
    private static final String VAR_LIB_BLUETOOTH = "/var/lib/bluetooth/";

    private LinuxBluetoothDevice(String name, String address, String majorDeviceClass, boolean connected,
            boolean paired, int batteryLevel, String adapterName) {
        super(name, address, majorDeviceClass, connected, paired, batteryLevel, adapterName);
    }

    /**
     * Gets Bluetooth devices known to the system.
     *
     * @return a list of {@link BluetoothDevice} objects
     */
    public static List<BluetoothDevice> getBluetoothDevices() {
        return queryBluetoothDevices(SYS_BLUETOOTH, VAR_LIB_BLUETOOTH);
    }

    /**
     * Queries Bluetooth devices from the specified paths.
     *
     * @param sysBluetoothPath path to sysfs bluetooth class (e.g., /sys/class/bluetooth/)
     * @param varLibPath       path to BlueZ state directory (e.g., /var/lib/bluetooth/)
     * @return a list of Bluetooth devices
     */
    static List<BluetoothDevice> queryBluetoothDevices(String sysBluetoothPath, String varLibPath) {
        File sysDir = new File(sysBluetoothPath);
        File[] adapterDirs = sysDir.listFiles();
        if (adapterDirs == null) {
            return Collections.emptyList();
        }

        List<BluetoothDevice> devices = new ArrayList<>();
        for (File adapterDir : adapterDirs) {
            String adapterName = adapterDir.getName();
            // Read adapter MAC from address file
            String adapterAddress = FileUtil.getStringFromFile(adapterDir.getAbsolutePath() + "/address").trim();
            if (adapterAddress.isEmpty()) {
                continue;
            }

            // Look for paired devices in /var/lib/bluetooth/<ADAPTER_MAC>/
            File adapterStateDir = new File(varLibPath + adapterAddress.toUpperCase(Locale.ROOT));
            if (!adapterStateDir.isDirectory()) {
                // Try lowercase (some systems)
                adapterStateDir = new File(varLibPath + adapterAddress);
            }
            if (!adapterStateDir.isDirectory()) {
                continue;
            }

            File[] deviceDirs = adapterStateDir.listFiles();
            if (deviceDirs == null) {
                continue;
            }

            for (File deviceDir : deviceDirs) {
                String dirName = deviceDir.getName();
                // Device directories are MAC addresses (XX:XX:XX:XX:XX:XX)
                if (!dirName.contains(":") || dirName.length() != 17) {
                    continue;
                }
                File infoFile = new File(deviceDir, "info");
                if (!infoFile.exists()) {
                    continue;
                }

                Map<String, String> props = FileUtil.getKeyValueMapFromFile(infoFile.getAbsolutePath(), "=");
                String name = props.getOrDefault("Name", "");
                String address = dirName;
                // Presence under /var/lib/bluetooth/<adapter>/<device>/ implies remembered/bonded state.
                boolean paired = Boolean.parseBoolean(props.getOrDefault("Paired", "true"));
                boolean connected = Boolean.parseBoolean(props.getOrDefault("Connected", "false"));
                int batteryLevel = ParseUtil.parseIntOrDefault(props.get("Battery"), -1);
                int classOfDevice = ParseUtil.hexStringToInt(props.getOrDefault("Class", "0"), 0);
                String majorClass = parseMajorDeviceClass(classOfDevice);

                devices.add(new LinuxBluetoothDevice(name, address, majorClass, connected, paired, batteryLevel,
                        adapterName));
            }
        }
        return Collections.unmodifiableList(devices);
    }
}
