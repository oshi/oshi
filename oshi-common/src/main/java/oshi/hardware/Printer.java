/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import oshi.annotation.PublicApi;
import oshi.annotation.concurrent.Immutable;

/**
 * Represents a printer device available to the operating system, including its name, driver, status, and connection
 * type (local or network).
 * <p>
 * <b>Platform notes:</b> On Windows, printer information is retrieved via WMI. On Linux, CUPS is used. On macOS,
 * printer information is retrieved from the CUPS/IPP subsystem.
 */
@PublicApi
@Immutable
public interface Printer {

    /**
     * Retrieves the name of the printer.
     *
     * @return The printer name.
     */
    String getName();

    /**
     * Retrieves the driver name or make/model of the printer.
     *
     * @return The driver or model name.
     */
    String getDriverName();

    /**
     * Retrieves the user-friendly description of the printer.
     *
     * @return The printer description.
     */
    String getDescription();

    /**
     * Retrieves the current status of the printer.
     *
     * @return The printer status.
     */
    PrinterStatus getStatus();

    /**
     * Retrieves the reason for the current printer status, if available.
     *
     * @return A string describing the status reason (e.g., "Paper Jam", "media-empty"), or empty string if none.
     */
    String getStatusReason();

    /**
     * Indicates whether this is the default printer.
     *
     * @return {@code true} if this is the default printer, {@code false} otherwise.
     */
    boolean isDefault();

    /**
     * Indicates whether this is a local printer (as opposed to a network printer).
     *
     * @return {@code true} if this is a local printer, {@code false} if it is a network printer.
     */
    boolean isLocal();

    /**
     * Retrieves the port name or device URI of the printer.
     *
     * @return The port name or URI.
     */
    String getPortName();

    /**
     * Printer status enumeration.
     */
    @PublicApi
    enum PrinterStatus {
        IDLE, PRINTING, ERROR, OFFLINE, UNKNOWN
    }
}
