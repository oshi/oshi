/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.demo;

import java.util.Locale;

import com.sun.jna.platform.mac.IOKit.IOConnect;

import oshi.annotation.SuppressForbidden;
import oshi.jna.platform.mac.IOKit.SMCVal;
import oshi.util.platform.mac.SmcUtil;

/**
 * Dumps all readable SMC keys and their raw float values. Run on macOS to discover Apple Silicon key names for fans,
 * voltage, and other sensors.
 */
public final class SmcDump {

    private SmcDump() {
    }

    /**
     * Entry point.
     *
     * @param args command line arguments
     */
    @SuppressForbidden(reason = "Using System.out in a demo class")
    public static void main(String[] args) {
        IOConnect conn = SmcUtil.smcOpen();
        if (conn == null) {
            System.err.println("Failed to open SMC connection"); // NOPMD
            return;
        }
        try {
            long keyCount = SmcUtil.smcGetLong(conn, "#KEY");
            System.out.printf(Locale.ROOT, "Total SMC keys: %d%n%n", keyCount);
            System.out.printf(Locale.ROOT, "%-6s %-6s %-8s %s%n", "Key", "Type", "Size", "Float value");
            System.out.println("----------------------------------------------");

            for (int i = 0; i < keyCount; i++) {
                String keyName = SmcUtil.smcReadKeyAtIndex(conn, i);
                if (keyName == null) {
                    continue;
                }
                try (SMCVal val = new SMCVal()) {
                    int result = SmcUtil.smcReadKey(conn, keyName, val);
                    String typeName = result == 0 ? SmcUtil.smcGetDataType(conn, keyName) : "?";
                    int size = result == 0 ? val.dataSize : 0;
                    double floatVal = SmcUtil.smcGetFloat(conn, keyName);
                    char first = keyName.charAt(0);
                    if (first == 'F' || first == 'V' || first == 'T' || floatVal != 0d) {
                        System.out.printf(Locale.ROOT, "%-6s %-6s %-8d %.4f%n", keyName, typeName, size, floatVal);
                    }
                }
            }
        } finally {
            SmcUtil.smcClose(conn);
        }
    }
}
