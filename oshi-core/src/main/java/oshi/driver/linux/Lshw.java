/*
 * Copyright 2020-2023 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.linux;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.UserGroupInfo;

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
        String model = null;
        String serial = null;
        String uuid = null;

        if (UserGroupInfo.isElevated()) {
            String modelMarker = "product:";
            String serialMarker = "serial:";
            String uuidMarker = "uuid:";

            for (String checkLine : ExecutingCommand.runNative("lshw -C system")) {
                if (checkLine.contains(modelMarker)) {
                    model = checkLine.split(modelMarker)[1].trim();
                } else if (checkLine.contains(serialMarker)) {
                    serial = checkLine.split(serialMarker)[1].trim();
                } else if (checkLine.contains(uuidMarker)) {
                    uuid = checkLine.split(uuidMarker)[1].trim();
                }
            }
        }
        MODEL = model;
        SERIAL = serial;
        UUID = uuid;
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
        String capacityMarker = "capacity:";
        for (String checkLine : ExecutingCommand.runNative("lshw -class processor")) {
            if (checkLine.contains(capacityMarker)) {
                return ParseUtil.parseHertz(checkLine.split(capacityMarker)[1].trim());
            }
        }
        return -1L;
    }
}
