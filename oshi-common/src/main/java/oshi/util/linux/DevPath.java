/*
 * Copyright 2024-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.linux;

import java.io.File;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.GlobalConfig;

/**
 * Provides constants for paths in the {@code /dev} filesystem on Linux.
 * <p>
 * Two roots are involved. {@link #DEV} is where OSHI reads the device filesystem, and can be configured elsewhere, for
 * example to a host's {@code /dev} mounted into a container. {@link #NODE_ROOT} is the root of the device names the
 * kernel, udev, {@code /proc/mounts} and tools such as {@code pvs} report, which is always {@code /dev/}. Device names
 * OSHI returns use {@link #NODE_ROOT}, so they match those sources wherever OSHI reads the files from; the prefix
 * constants other than {@link #DISK_BY_UUID} are names, and {@link #toFilePath(String)} gives the path to read one.
 * <p>
 * If the user desires to configure a custom {@code /dev} path, it must be declared in the OSHI configuration file or
 * updated in the {@link GlobalConfig} class prior to initializing this class.
 */
@ThreadSafe
public final class DevPath {

    /**
     * The /dev filesystem location, where device files are read.
     */
    public static final String DEV = queryDevConfig() + "/";

    /**
     * The root of device node names as the kernel and udev report them, regardless of {@link #DEV}.
     */
    public static final String NODE_ROOT = "/dev/";

    /** Path to disk-by-uuid directory, below {@link #DEV}. */
    public static final String DISK_BY_UUID = DEV + "disk/by-uuid";
    /** Name prefix for device-mapper devices. */
    public static final String DM = NODE_ROOT + "dm";
    /** Name prefix for loop devices. */
    public static final String LOOP = NODE_ROOT + "loop";
    /** Name prefix for device-mapper mapped names. */
    public static final String MAPPER = NODE_ROOT + "mapper/";
    /** Name prefix for RAM disk devices. */
    public static final String RAM = NODE_ROOT + "ram";

    private DevPath() {
    }

    private static String queryDevConfig() {
        String devPath = GlobalConfig.get(GlobalConfig.OSHI_UTIL_DEV_PATH, "/dev");
        // Ensure prefix begins with path separator, but doesn't end with one
        devPath = '/' + devPath.replaceAll("/$|^/", "");
        if (!new File(devPath).exists()) {
            throw new GlobalConfig.PropertyException(GlobalConfig.OSHI_UTIL_DEV_PATH, "The path does not exist");
        }
        return devPath;
    }

    /**
     * Converts a device name below {@link #NODE_ROOT} to the path below {@link #DEV} where its file can be read.
     *
     * @param nodeName a device name such as {@code /dev/mapper/vg-root}
     * @return the path to read, or {@code nodeName} unchanged if it is not below {@link #NODE_ROOT}
     */
    public static String toFilePath(String nodeName) {
        return reroot(nodeName, NODE_ROOT, DEV);
    }

    /**
     * Converts a path below {@link #DEV}, such as a resolved symbolic link, to the device name below
     * {@link #NODE_ROOT}.
     *
     * @param filePath a path such as {@code /host/dev/sda1}
     * @return the device name, or {@code filePath} unchanged if it is not below {@link #DEV}
     */
    public static String toNodeName(String filePath) {
        return reroot(filePath, DEV, NODE_ROOT);
    }

    /**
     * Replaces one root prefix with another. Package-private for testing, since {@link #DEV} is fixed at startup.
     *
     * @param path the path to convert
     * @param from the root to replace, ending in a slash
     * @param to   the root to replace it with, ending in a slash
     * @return the converted path, or {@code path} unchanged if it is not below {@code from}
     */
    static String reroot(String path, String from, String to) {
        return path.startsWith(from) ? to + path.substring(from.length()) : path;
    }
}
