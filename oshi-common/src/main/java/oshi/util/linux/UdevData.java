/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.linux;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.FileUtil;

/**
 * Reads device properties from the udev database that {@code systemd-udevd} keeps under {@code /run/udev/data}. The
 * files are world-readable and hold the same {@code E:} properties libudev returns from
 * {@code udev_device_get_property_value}, so code that cannot load libudev can still read them.
 * <p>
 * Each device has one file, named by subsystem: {@code b<major>:<minor>} for a block device and {@code n<ifindex>} for
 * a network interface. A host without {@code udevd}, such as most containers, has no database; every query then returns
 * an empty map.
 */
@ThreadSafe
public final class UdevData {

    /** The udev database directory. */
    public static final String RUN_UDEV_DATA = "/run/udev/data/";

    private UdevData() {
    }

    /**
     * Reads the udev properties of a block device.
     *
     * @param major the device's major number
     * @param minor the device's minor number
     * @return the properties, or an empty map if udev has no record of the device
     */
    public static Map<String, String> queryBlockDevice(int major, int minor) {
        return queryBlockDevice(RUN_UDEV_DATA, major, minor);
    }

    /**
     * Reads the udev properties of a block device from a given database directory. Used for testing and for a database
     * copied from another host.
     *
     * @param dataDir the database directory, ending in a slash
     * @param major   the device's major number
     * @param minor   the device's minor number
     * @return the properties, or an empty map if there is no record
     */
    public static Map<String, String> queryBlockDevice(String dataDir, int major, int minor) {
        if (major <= 0) {
            return Collections.emptyMap();
        }
        return parseProperties(FileUtil.readFile(dataDir + "b" + major + ":" + minor, false));
    }

    /**
     * Reads the udev properties of a network interface.
     *
     * @param ifindex the interface index, as in {@code /sys/class/net/<name>/ifindex}
     * @return the properties, or an empty map if udev has no record of the interface
     */
    public static Map<String, String> queryNetworkInterface(int ifindex) {
        return queryNetworkInterface(RUN_UDEV_DATA, ifindex);
    }

    /**
     * Reads the udev properties of a network interface from a given database directory. Used for testing and for a
     * database copied from another host.
     *
     * @param dataDir the database directory, ending in a slash
     * @param ifindex the interface index
     * @return the properties, or an empty map if there is no record
     */
    public static Map<String, String> queryNetworkInterface(String dataDir, int ifindex) {
        if (ifindex <= 0) {
            return Collections.emptyMap();
        }
        return parseProperties(FileUtil.readFile(dataDir + "n" + ifindex, false));
    }

    /**
     * Parses the property lines of a udev database file. A property line is {@code E:KEY=VALUE}; the other records
     * ({@code S:} symlinks, {@code G:} tags, {@code I:} initialization time and so on) are ignored.
     *
     * @param lines the lines of the file
     * @return the properties keyed by name
     */
    static Map<String, String> parseProperties(List<String> lines) {
        Map<String, String> properties = new HashMap<>();
        for (String line : lines) {
            if (line.startsWith("E:")) {
                int eq = line.indexOf('=');
                if (eq > 2) {
                    properties.put(line.substring(2, eq), line.substring(eq + 1));
                }
            }
        }
        return properties;
    }
}
