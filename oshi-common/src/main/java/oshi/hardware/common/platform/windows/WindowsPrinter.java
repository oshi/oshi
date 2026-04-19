/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.windows;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.Printer;
import oshi.hardware.common.AbstractPrinter;

/**
 * Common Windows printer logic shared between JNA and FFM implementations.
 */
@Immutable
public abstract class WindowsPrinter extends AbstractPrinter {

    // DetectedErrorState: 0=Unknown, 1=Other, 2=No Error, 3=Low Paper, 4=No Paper,
    // 5=Low Toner, 6=No Toner, 7=Door Open, 8=Jammed, 9=Offline, 10=Service Requested, 11=Output Bin Full
    private static final String[] ERROR_STATE_NAMES = { "Unknown", "Other", "", "Low Paper", "No Paper", "Low Toner",
            "No Toner", "Door Open", "Jammed", "Offline", "Service Requested", "Output Bin Full" };

    protected WindowsPrinter(String name, String driverName, String description, PrinterStatus status,
            String statusReason, boolean isDefault, boolean isLocal, String portName) {
        super(name, driverName, description, status, statusReason, isDefault, isLocal, portName);
    }

    /**
     * Parses Win32_Printer PrinterStatus and DetectedErrorState codes.
     *
     * @param statusCode The PrinterStatus code from WMI.
     * @param errorState The DetectedErrorState code from WMI.
     * @return The corresponding PrinterStatus enum value.
     */
    protected static Printer.PrinterStatus parseStatus(int statusCode, int errorState) {
        if (errorState == 4 || errorState == 6 || errorState >= 7) {
            return Printer.PrinterStatus.ERROR;
        }
        switch (statusCode) {
            case 1: // Other
            case 2: // Unknown
                return Printer.PrinterStatus.UNKNOWN;
            case 3: // Idle
                return Printer.PrinterStatus.IDLE;
            case 4: // Printing
                return Printer.PrinterStatus.PRINTING;
            case 5: // Warmup
                return Printer.PrinterStatus.IDLE;
            case 6: // Stopped Printing
            case 7: // Offline
                return Printer.PrinterStatus.OFFLINE;
            default:
                return Printer.PrinterStatus.UNKNOWN;
        }
    }

    /**
     * Converts a DetectedErrorState code to a human-readable string.
     *
     * @param errorState The DetectedErrorState code from WMI.
     * @return A string describing the error state.
     */
    protected static String parseErrorState(int errorState) {
        if (errorState >= 0 && errorState < ERROR_STATE_NAMES.length) {
            return ERROR_STATE_NAMES[errorState];
        }
        return "";
    }
}
