/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.linux;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.FileUtil;
import oshi.util.Util;
import oshi.util.linux.SysPath;

/**
 * Utility to read the board identity from the devicetree, which ARM, RISC-V and POWER boards describe themselves with
 * in place of the DMI tables of a PC. Each property is a file under {@code /sys/firmware/devicetree/base} holding one
 * or more NUL-terminated strings.
 */
@ThreadSafe
public final class Devicetree {

    private static final String DEVICETREE_PROPERTIES = "oshi.devicetree.properties";

    private Devicetree() {
    }

    /**
     * Query the model from the devicetree
     *
     * @return The model if available, null otherwise
     */
    public static @Nullable String queryModel() {
        return queryModel(SysPath.DEVICETREE);
    }

    /**
     * Query the model from a devicetree directory. Package-private for testing.
     *
     * @param base the devicetree directory, ending in a slash
     * @return The model if available, null otherwise
     */
    static @Nullable String queryModel(String base) {
        List<String> model = readStrings(base + "model");
        return model.isEmpty() ? null : model.get(0).replace("Machine: ", "");
    }

    /**
     * Query the board's serial number from the devicetree. The firmware of a Raspberry Pi and of many other boards
     * fills it in.
     *
     * @return The serial number if available, null otherwise
     */
    public static @Nullable String querySerialNumber() {
        return querySerialNumber(SysPath.DEVICETREE);
    }

    /**
     * Query the board's serial number from a devicetree directory. Package-private for testing.
     *
     * @param base the devicetree directory, ending in a slash
     * @return The serial number if available, null otherwise
     */
    static @Nullable String querySerialNumber(String base) {
        List<String> serial = readStrings(base + "serial-number");
        return serial.isEmpty() ? null : serial.get(0);
    }

    /**
     * Query the board's manufacturer from the devicetree. The first {@code compatible} string names the board as
     * {@code vendor,board}; the vendor prefix is translated to a name through {@code oshi.devicetree.properties}.
     *
     * @return The manufacturer if available, null otherwise
     */
    public static @Nullable String queryManufacturer() {
        return queryManufacturer(SysPath.DEVICETREE);
    }

    /**
     * Query the board's manufacturer from a devicetree directory. Package-private for testing.
     *
     * @param base the devicetree directory, ending in a slash
     * @return The manufacturer if available, null otherwise
     */
    static @Nullable String queryManufacturer(String base) {
        List<String> compatible = readStrings(base + "compatible");
        if (compatible.isEmpty()) {
            return null;
        }
        int comma = compatible.get(0).indexOf(',');
        if (comma <= 0) {
            return null;
        }
        String vendor = compatible.get(0).substring(0, comma);
        // QEMU's generic virt machine claims no real vendor
        if ("linux".equals(vendor)) {
            return null;
        }
        String name = FileUtil.readPropertiesFromFilename(DEVICETREE_PROPERTIES).getProperty(vendor);
        return Util.isBlank(name) ? vendor : name;
    }

    /**
     * Reads the NUL-separated strings of a devicetree property.
     *
     * @param path the property file
     * @return the non-empty strings, in order
     */
    private static List<String> readStrings(String path) {
        List<String> strings = new ArrayList<>();
        for (String s : new String(FileUtil.readAllBytes(path, false), StandardCharsets.UTF_8).split("\0", -1)) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) {
                strings.add(trimmed);
            }
        }
        return strings;
    }
}
