/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.linux;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Utility to read info from {@code lshal}
 */
@ThreadSafe
public final class Lshal {

    private Lshal() {
    }

    /**
     * Query the serial number from lshal
     *
     * @return The serial number if available, null otherwise
     */
    public static String querySerialNumber() {
        // if lshal command available (HAL deprecated in newer linuxes)
        String marker = "system.hardware.serial =";
        for (String checkLine : ExecutingCommand.runNative("lshal")) {
            if (checkLine.contains(marker)) {
                return ParseUtil.getSingleQuoteStringValue(checkLine);
            }
        }
        return null;
    }

    /**
     * Query the UUID from lshal
     *
     * @return The UUID if available, null otherwise
     */
    public static String queryUUID() {
        // if lshal command available (HAL deprecated in newer linuxes)
        String marker = "system.hardware.uuid =";
        for (String checkLine : ExecutingCommand.runNative("lshal")) {
            if (checkLine.contains(marker)) {
                return ParseUtil.getSingleQuoteStringValue(checkLine);
            }
        }
        return null;
    }
}
