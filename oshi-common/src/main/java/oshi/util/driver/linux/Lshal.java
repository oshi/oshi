/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.linux;

import java.util.List;

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
        return querySerialNumber(ExecutingCommand.runNative("lshal"));
    }

    /**
     * Parse the serial number from lshal output.
     *
     * @param lines output of {@code lshal}
     * @return The serial number if available, null otherwise
     */
    static String querySerialNumber(List<String> lines) {
        String marker = "system.hardware.serial =";
        for (String checkLine : lines) {
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
        return queryUUID(ExecutingCommand.runNative("lshal"));
    }

    /**
     * Parse the UUID from lshal output.
     *
     * @param lines output of {@code lshal}
     * @return The UUID if available, null otherwise
     */
    static String queryUUID(List<String> lines) {
        String marker = "system.hardware.uuid =";
        for (String checkLine : lines) {
            if (checkLine.contains(marker)) {
                return ParseUtil.getSingleQuoteStringValue(checkLine);
            }
        }
        return null;
    }
}
