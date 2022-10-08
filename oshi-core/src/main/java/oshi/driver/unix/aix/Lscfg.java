/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.unix.aix;

import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

/**
 * Utility to query lscfg
 */
@ThreadSafe
public final class Lscfg {

    private Lscfg() {
    }

    /**
     * Query {@code lscfg -vp} to get all hardware devices
     *
     * @return A list of the output
     */
    public static List<String> queryAllDevices() {
        return ExecutingCommand.runNative("lscfg -vp");
    }

    /**
     * Parse the output of {@code lscfg -vp} to get backplane info
     *
     * @param lscfg The output of a previous call to {@code lscfg -vp}
     * @return A triplet with backplane model, serial number, and version
     */
    public static Triplet<String, String, String> queryBackplaneModelSerialVersion(List<String> lscfg) {
        final String planeMarker = "WAY BACKPLANE";
        final String modelMarker = "Part Number";
        final String serialMarker = "Serial Number";
        final String versionMarker = "Version";
        final String locationMarker = "Physical Location";

        // 1 WAY BACKPLANE :
        // Serial Number...............YL10243490FB
        // Part Number.................80P4315
        // Customer Card ID Number.....26F4
        // CCIN Extender...............1
        // FRU Number.................. 80P4315
        // Version.....................RS6K
        // Hardware Location Code......U0.1-P1
        // Physical Location: U0.1-P1

        String model = null;
        String serialNumber = null;
        String version = null;
        boolean planeFlag = false;
        for (final String checkLine : lscfg) {
            if (!planeFlag && checkLine.contains(planeMarker)) {
                planeFlag = true;
            } else if (planeFlag) {
                if (checkLine.contains(modelMarker)) {
                    model = ParseUtil.removeLeadingDots(checkLine.split(modelMarker)[1].trim());
                } else if (checkLine.contains(serialMarker)) {
                    serialNumber = ParseUtil.removeLeadingDots(checkLine.split(serialMarker)[1].trim());
                } else if (checkLine.contains(versionMarker)) {
                    version = ParseUtil.removeLeadingDots(checkLine.split(versionMarker)[1].trim());
                } else if (checkLine.contains(locationMarker)) {
                    break;
                }
            }
        }
        return new Triplet<>(model, serialNumber, version);
    }

    /**
     * Query {@code lscfg -vl device} to get hardware info
     *
     * @param device The disk to get the model and serial from
     * @return A pair containing the model and serial number for the device, or null if not found
     */
    public static Pair<String, String> queryModelSerial(String device) {
        String modelMarker = "Machine Type and Model";
        String serialMarker = "Serial Number";
        String model = null;
        String serial = null;
        for (String s : ExecutingCommand.runNative("lscfg -vl " + device)) {
            // Default model to description at end of first line
            if (model == null && s.contains(device)) {
                String locDesc = s.split(device)[1].trim();
                int idx = locDesc.indexOf(' ');
                if (idx > 0) {
                    model = locDesc.substring(idx).trim();
                }
            }
            if (s.contains(modelMarker)) {
                model = ParseUtil.removeLeadingDots(s.split(modelMarker)[1].trim());
            } else if (s.contains(serialMarker)) {
                serial = ParseUtil.removeLeadingDots(s.split(serialMarker)[1].trim());
            }
        }
        return new Pair<>(model, serial);
    }
}
