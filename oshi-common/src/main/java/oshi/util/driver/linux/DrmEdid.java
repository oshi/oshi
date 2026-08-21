/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.linux;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.linux.SysPath;
import oshi.util.tuples.Triplet;

/**
 * Utility to read EDID data from the Linux DRM (Direct Rendering Manager) subsystem. The kernel exposes raw EDID bytes
 * for each connected display at {@code /sys/class/drm/card<N>-<connector>/edid}, which works regardless of whether X11
 * or Wayland is in use.
 */
@ThreadSafe
public final class DrmEdid {

    private DrmEdid() {
    }

    /**
     * Read EDID byte arrays from /sys/class/drm for all connected displays.
     *
     * @return a list of EDID byte arrays (at least 128 bytes each), or empty if none found
     */
    public static List<byte[]> getEdidArrays() {
        return getEdidArrays(new File(SysPath.DRM));
    }

    /**
     * Read EDID byte arrays from the given DRM directory.
     *
     * @param drmDir the directory containing card*-* subdirectories
     * @return a list of EDID byte arrays (at least 128 bytes each), or empty if none found
     */
    static List<byte[]> getEdidArrays(File drmDir) {
        List<Triplet<String, Integer, byte[]>> data = getDisplayData(drmDir);
        List<byte[]> edids = new ArrayList<>(data.size());
        for (Triplet<String, Integer, byte[]> t : data) {
            edids.add(t.getC());
        }
        return Collections.unmodifiableList(edids);
    }

    /**
     * Read display data from /sys/class/drm for all connected displays.
     *
     * @return a list of {@link Triplet} of kernel connector name (e.g. {@code HDMI-A-1}), DRM connector ID ({@code -1}
     *         if not available), and EDID byte array (at least 128 bytes each), or empty if none found
     */
    public static List<Triplet<String, Integer, byte[]>> getDisplayData() {
        return getDisplayData(new File(SysPath.DRM));
    }

    /**
     * Read display data from the given DRM directory.
     *
     * @param drmDir the directory containing card*-* subdirectories
     * @return a list of {@link Triplet} of kernel connector name, DRM connector ID ({@code -1} if not available), and
     *         EDID byte array (at least 128 bytes each), or empty if none found
     */
    static List<Triplet<String, Integer, byte[]>> getDisplayData(File drmDir) {
        if (!drmDir.isDirectory()) {
            return Collections.emptyList();
        }
        File[] connectors = drmDir.listFiles(f -> f.isDirectory() && f.getName().matches("card\\d+-.+"));
        if (connectors == null || connectors.length == 0) {
            return Collections.emptyList();
        }
        List<Triplet<String, Integer, byte[]>> results = new ArrayList<>();
        for (File connector : connectors) {
            File statusFile = new File(connector, "status");
            if (statusFile.exists()) {
                String status = FileUtil.getStringFromFile(statusFile.getPath()).trim();
                if (!"connected".equals(status)) {
                    continue;
                }
            }
            File edidFile = new File(connector, "edid");
            if (edidFile.exists()) {
                byte[] edid = FileUtil.readAllBytes(edidFile.getPath(), false);
                if (edid.length >= 128) {
                    // Extract connector name from directory name: "card0-HDMI-A-1" -> "HDMI-A-1"
                    String dirName = connector.getName();
                    String connectorName = dirName.substring(dirName.indexOf('-') + 1);
                    int connectorId = -1;
                    File connectorIdFile = new File(connector, "connector_id");
                    if (connectorIdFile.exists()) {
                        connectorId = ParseUtil
                                .parseIntOrDefault(FileUtil.getStringFromFile(connectorIdFile.getPath()).trim(), -1);
                    }
                    results.add(new Triplet<>(connectorName, connectorId, edid));
                }
            }
        }
        return Collections.unmodifiableList(results);
    }
}
