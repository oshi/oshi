/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.unix;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.Printer.PrinterStatus;
import oshi.util.ExecutingCommand;

/**
 * Utility to parse printer information from {@code lpstat} and {@code lpoptions} commands. Shared fallback path for
 * both JNA and FFM implementations when libcups is unavailable.
 */
@ThreadSafe
public final class Lpstat {

    private static final String[] LOCAL_URI_PREFIXES = { "usb:", "parallel:", "serial:", "file:", "direct:", "hp:",
            "lpd://127.", "lpd://localhost", "socket://127.", "socket://localhost" };

    private Lpstat() {
    }

    /**
     * Query the default printer name.
     *
     * @return the default printer name, or empty string if none
     */
    public static String queryDefaultPrinter() {
        return queryDefaultPrinter(ExecutingCommand.runNative(new String[] { "lpstat", "-d" }));
    }

    /**
     * Parse the default printer from lpstat -d output.
     *
     * @param lines output of {@code lpstat -d}
     * @return the default printer name, or empty string if none
     */
    static String queryDefaultPrinter(List<String> lines) {
        for (String line : lines) {
            if (line.contains("default destination:")) {
                String[] parts = line.split(":", 2);
                if (parts.length >= 2) {
                    return parts[1].trim();
                }
            }
        }
        return "";
    }

    /**
     * Query the port (device URI) map for all printers.
     *
     * @return map of printer name to device URI
     */
    public static Map<String, String> queryPortMap() {
        return queryPortMap(ExecutingCommand.runNative(new String[] { "lpstat", "-v" }));
    }

    /**
     * Parse the port map from lpstat -v output.
     *
     * @param lines output of {@code lpstat -v}
     * @return map of printer name to device URI
     */
    static Map<String, String> queryPortMap(List<String> lines) {
        Map<String, String> map = new HashMap<>();
        for (String line : lines) {
            int forIdx = line.indexOf("device for ");
            if (forIdx >= 0) {
                int nameStart = forIdx + 11;
                int colonIdx = line.indexOf(':', nameStart);
                if (colonIdx > nameStart) {
                    map.put(line.substring(nameStart, colonIdx).trim(), line.substring(colonIdx + 1).trim());
                }
            }
        }
        return map;
    }

    /**
     * Query the description map for all printers.
     *
     * @return map of printer name to description
     */
    public static Map<String, String> queryDescriptionMap() {
        return queryDescriptionMap(ExecutingCommand.runNative(new String[] { "lpstat", "-l", "-p" }));
    }

    /**
     * Parse the description map from lpstat -l -p output.
     *
     * @param lines output of {@code lpstat -l -p}
     * @return map of printer name to description
     */
    static Map<String, String> queryDescriptionMap(List<String> lines) {
        Map<String, String> map = new HashMap<>();
        String currentPrinter = null;
        for (String line : lines) {
            if (line.startsWith("printer ")) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 2) {
                    currentPrinter = parts[1];
                }
            } else if (currentPrinter != null && line.trim().startsWith("Description:")) {
                map.put(currentPrinter, line.substring(line.indexOf(':') + 1).trim());
            }
        }
        return map;
    }

    /**
     * Query the driver name for a specific printer.
     *
     * @param printerName the printer name
     * @return the driver (make-and-model) string, or empty string if unknown
     */
    public static String queryDriver(String printerName) {
        return queryDriver(ExecutingCommand.runNative(new String[] { "lpoptions", "-p", printerName }));
    }

    /**
     * Parse the driver name from lpoptions output.
     *
     * @param lines output of {@code lpoptions -p <printer>}
     * @return the driver (make-and-model) string, or empty string if unknown
     */
    static String queryDriver(List<String> lines) {
        for (String line : lines) {
            int idx = line.indexOf("printer-make-and-model='");
            if (idx >= 0) {
                int start = idx + 24;
                int end = line.indexOf('\'', start);
                if (end > start) {
                    return line.substring(start, end);
                }
            }
        }
        return "";
    }

    /**
     * Parse the printer status from an lpstat -p line.
     *
     * @param line a single line from {@code lpstat -p}
     * @return the parsed status
     */
    public static PrinterStatus parseStatus(String line) {
        if (line == null) {
            return PrinterStatus.UNKNOWN;
        }
        String lower = line.toLowerCase(Locale.ROOT);
        if (lower.contains("disabled") || lower.contains("not accepting")) {
            return PrinterStatus.OFFLINE;
        } else if (lower.contains("printing")) {
            return PrinterStatus.PRINTING;
        } else if (lower.contains("idle")) {
            return PrinterStatus.IDLE;
        } else if (lower.contains("error") || lower.contains("fault")) {
            return PrinterStatus.ERROR;
        }
        return PrinterStatus.UNKNOWN;
    }

    /**
     * Parse the status reason from an lpstat -p line.
     *
     * @param line a single line from {@code lpstat -p}
     * @return the reason string, or empty string if none
     */
    public static String parseStatusReason(String line) {
        if (line == null) {
            return "";
        }
        int dashIdx = line.indexOf(" - ");
        return dashIdx > 0 ? line.substring(dashIdx + 3).trim() : "";
    }

    /**
     * Determine if a device URI represents a local printer.
     *
     * @param uri the device URI
     * @return true if the URI indicates a local printer
     */
    public static boolean isLocalUri(String uri) {
        if (uri == null) {
            return false;
        }
        if (uri.startsWith("/dev")) {
            return true;
        }
        for (String prefix : LOCAL_URI_PREFIXES) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
