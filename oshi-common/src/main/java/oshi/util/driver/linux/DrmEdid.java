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
import oshi.util.linux.SysPath;

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
        if (!drmDir.isDirectory()) {
            return Collections.emptyList();
        }
        File[] connectors = drmDir.listFiles(f -> f.isDirectory() && f.getName().matches("card\\d+-.+"));
        if (connectors == null || connectors.length == 0) {
            return Collections.emptyList();
        }
        List<byte[]> displays = new ArrayList<>();
        for (File connector : connectors) {
            File statusFile = new File(connector, "status");
            if (statusFile.exists()) {
                String status = FileUtil.getStringFromFile(statusFile.getPath()).trim();
                if (!"connected".equals(status)) {
                    continue;
                }
            }
            File edidFile = new File(connector, "edid");
            if (edidFile.exists() && edidFile.length() >= 128) {
                byte[] edid = FileUtil.readAllBytes(edidFile.getPath(), false);
                if (edid != null && edid.length >= 128) {
                    displays.add(edid);
                }
            }
        }
        return displays;
    }
}
