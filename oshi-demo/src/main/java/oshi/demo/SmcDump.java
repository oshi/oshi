/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.demo;

import com.sun.jna.NativeLong;
import com.sun.jna.platform.mac.IOKit.IOConnect;

import java.util.Locale;

import oshi.annotation.SuppressForbidden;
import oshi.jna.ByRef.CloseableNativeLongByReference;
import oshi.jna.platform.mac.IOKit;
import oshi.jna.platform.mac.IOKit.SMCKeyData;
import oshi.jna.platform.mac.IOKit.SMCVal;
import oshi.util.ParseUtil;
import oshi.util.platform.mac.SmcUtil;

/**
 * Dumps all readable SMC keys and their raw float values. Run on macOS to discover Apple Silicon key names for fans,
 * voltage, and other sensors.
 */
public final class SmcDump {

    private static final byte SMC_CMD_READ_INDEX = 8;
    private static final IOKit IO = IOKit.INSTANCE;

    private SmcDump() {
    }

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
                String keyName = readKeyAtIndex(conn, i);
                if (keyName == null) {
                    continue;
                }
                try (SMCVal val = new SMCVal()) {
                    int result = SmcUtil.smcReadKey(conn, keyName, val);
                    String typeName = result == 0 ? asciiType(val.dataType) : "?";
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

    private static String readKeyAtIndex(IOConnect conn, int index) {
        try (SMCKeyData input = new SMCKeyData();
                SMCKeyData output = new SMCKeyData();
                CloseableNativeLongByReference size = new CloseableNativeLongByReference(
                        new NativeLong(output.size()))) {
            input.data8 = SMC_CMD_READ_INDEX;
            input.data32 = index;
            int result = IO.IOConnectCallStructMethod(conn, SmcUtil.KERNEL_INDEX_SMC, input,
                    new NativeLong(input.size()), output, size);
            if (result != 0) {
                return null;
            }
            // Key is stored as a big-endian 4-byte int in output.key; convert to ASCII string
            byte[] keyBytes = ParseUtil.longToByteArray(output.key, 4, 4);
            StringBuilder sb = new StringBuilder(4);
            for (byte b : keyBytes) {
                sb.append((char) (b & 0xFF));
            }
            return sb.toString();
        }
    }

    private static String asciiType(byte[] dataType) {
        if (dataType == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : dataType) {
            if (b == 0) {
                break;
            }
            sb.append((char) b);
        }
        return sb.toString();
    }
}
