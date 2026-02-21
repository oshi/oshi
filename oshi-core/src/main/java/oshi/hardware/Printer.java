/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import oshi.annotation.concurrent.Immutable;

/**
 * Printer interface representing a printer device.
 */
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
    enum PrinterStatus {
        IDLE, PRINTING, ERROR, OFFLINE, UNKNOWN
    }
}
