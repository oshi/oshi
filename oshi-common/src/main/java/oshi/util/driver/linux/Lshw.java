/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.linux;

import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.tuples.Triplet;

/**
 * Utility to read info from {@code lshw}
 */
@ThreadSafe
public final class Lshw {

    private Lshw() {
    }

    private static final String MODEL;
    private static final String SERIAL;
    private static final String UUID;
    static {
        Triplet<String, String, String> info = parseSystemInfo(ExecutingCommand.runPrivilegedNative("lshw -C system"));
        MODEL = info.getA();
        SERIAL = info.getB();
        UUID = info.getC();
    }

    /**
     * Parse model, serial number, and UUID from lshw system output.
     *
     * @param lines output of {@code lshw -C system}
     * @return triplet of (model, serial, uuid), with null for missing values
     */
    static Triplet<String, String, String> parseSystemInfo(List<String> lines) {
        String model = null;
        String serial = null;
        String uuid = null;

        String modelMarker = "product:";
        String serialMarker = "serial:";
        String uuidMarker = "uuid:";

        for (String checkLine : lines) {
            if (checkLine.contains(modelMarker)) {
                model = checkLine.split(modelMarker)[1].trim();
            } else if (checkLine.contains(serialMarker)) {
                serial = checkLine.split(serialMarker)[1].trim();
            } else if (checkLine.contains(uuidMarker)) {
                uuid = checkLine.split(uuidMarker)[1].trim();
            }
        }
        return new Triplet<>(model, serial, uuid);
    }

    /**
     * Query the model from lshw
     *
     * @return The model if available, null otherwise
     */
    public static String queryModel() {
        return MODEL;
    }

    /**
     * Query the serial number from lshw
     *
     * @return The serial number if available, null otherwise
     */
    public static String querySerialNumber() {
        return SERIAL;
    }

    /**
     * Query the UUID from lshw
     *
     * @return The UUID if available, null otherwise
     */
    public static String queryUUID() {
        return UUID;
    }

    /**
     * Query the CPU capacity (max frequency) from lshw
     *
     * @return The CPU capacity (max frequency) if available, -1 otherwise
     */
    public static long queryCpuCapacity() {
        return queryCpuCapacity(ExecutingCommand.runPrivilegedNative("lshw -class processor"));
    }

    /**
     * Parse the CPU capacity (max frequency) from lshw processor output.
     *
     * @param lines output of {@code lshw -class processor}
     * @return The CPU capacity (max frequency) if available, -1 otherwise
     */
    static long queryCpuCapacity(List<String> lines) {
        String capacityMarker = "capacity:";
        for (String checkLine : lines) {
            if (checkLine.contains(capacityMarker)) {
                return ParseUtil.parseHertz(checkLine.split(capacityMarker)[1].trim());
            }
        }
        return -1L;
    }
}
