/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.unix.freebsd.disk;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.tuples.Quartet;

/**
 * Utility to query geom part list
 */
@ThreadSafe
public final class GeomDiskList {

    private static final String GEOM_DISK_LIST = "geom disk list";
    private static final String UNKNOWN_TYPE = "Unknown";

    private GeomDiskList() {
    }

    /**
     * Queries disk data using geom
     *
     * @return A map with disk name as the key and a Quartet of model, serial, size, and disk type as the value
     */
    public static Map<String, Quartet<String, String, Long, String>> queryDisks() {
        return parseGeomDiskList(ExecutingCommand.runNative(GEOM_DISK_LIST));
    }

    /**
     * Parses the output of {@code geom disk list} into a map of disk parameters.
     *
     * @param geom the lines of output from {@code geom disk list}
     * @return A map with disk name as the key and a Quartet of model, serial, size, and disk type as the value
     */
    static Map<String, Quartet<String, String, Long, String>> parseGeomDiskList(List<String> geom) {
        // Map of device name to disk, to be returned
        Map<String, Quartet<String, String, Long, String>> diskMap = new HashMap<>();
        // Parameters needed.
        String diskName = null; // Non-null identifies a valid partition
        String descr = Constants.UNKNOWN;
        String ident = Constants.UNKNOWN;
        long mediaSize = 0L;
        String diskType = UNKNOWN_TYPE;

        for (String line : geom) {
            line = line.trim();
            // Marks the DiskStore device
            if (line.startsWith("Geom name:")) {
                // Save any previous disk in the map
                if (diskName != null) {
                    diskMap.put(diskName, new Quartet<>(descr, ident, mediaSize, diskType));
                    descr = Constants.UNKNOWN;
                    ident = Constants.UNKNOWN;
                    mediaSize = 0L;
                    diskType = UNKNOWN_TYPE;
                }
                // Now use new diskName
                diskName = line.substring(line.lastIndexOf(' ') + 1);
            }
            // If we don't have a valid store, don't bother parsing anything
            if (diskName != null) {
                line = line.trim();
                if (line.startsWith("Mediasize:")) {
                    String[] split = ParseUtil.whitespaces.split(line, -1);
                    if (split.length > 1) {
                        mediaSize = ParseUtil.parseLongOrDefault(split[1], 0L);
                    }
                }
                if (line.startsWith("descr:")) {
                    descr = line.replace("descr:", "").trim();
                }
                if (line.startsWith("rotationrate:")) {
                    diskType = parseRotationRate(line.substring("rotationrate:".length()).trim());
                }
                if (line.startsWith("ident:")) {
                    ident = line.replace("ident:", "").replace("(null)", "").trim();
                }
            }
        }
        if (diskName != null) {
            diskMap.put(diskName, new Quartet<>(descr, ident, mediaSize, diskType));
        }
        return diskMap;
    }

    /**
     * Maps the {@code rotationrate} geom prints to a disk type. The kernel prints {@code 0} for a drive that reports
     * itself non-rotating, the speed in RPM for one that spins, and {@code unknown} or {@code invalid} otherwise
     * ({@code sys/geom/geom_disk.c}).
     *
     * @param rate the value after {@code rotationrate:}
     * @return {@code SSD}, {@code HDD}, or {@code Unknown}
     */
    static String parseRotationRate(String rate) {
        if ("0".equals(rate)) {
            return "SSD";
        }
        return ParseUtil.parseIntOrDefault(rate, 0) > 0 ? "HDD" : UNKNOWN_TYPE;
    }
}
