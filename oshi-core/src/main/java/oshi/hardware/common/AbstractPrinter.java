/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.Printer;

/**
 * Abstract base class for Printer implementations.
 */
@Immutable
public abstract class AbstractPrinter implements Printer {

    private final String name;
    private final String driverName;
    private final String description;
    private final PrinterStatus status;
    private final String statusReason;
    private final boolean isDefault;
    private final boolean isLocal;
    private final String portName;

    protected AbstractPrinter(String name, String driverName, String description, PrinterStatus status,
            String statusReason, boolean isDefault, boolean isLocal, String portName) {
        this.name = name;
        this.driverName = driverName;
        this.description = description;
        this.status = status;
        this.statusReason = statusReason;
        this.isDefault = isDefault;
        this.isLocal = isLocal;
        this.portName = portName;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDriverName() {
        return driverName;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public PrinterStatus getStatus() {
        return status;
    }

    @Override
    public String getStatusReason() {
        return statusReason;
    }

    @Override
    public boolean isDefault() {
        return isDefault;
    }

    @Override
    public boolean isLocal() {
        return isLocal;
    }

    @Override
    public String getPortName() {
        return portName;
    }

    @Override
    public String toString() {
        return "Printer [name=" + name + ", driverName=" + driverName + ", description=" + description + ", status="
                + status + ", statusReason=" + statusReason + ", isDefault=" + isDefault + ", isLocal=" + isLocal
                + ", portName=" + redactPortName(portName) + "]";
    }

    private static String redactPortName(String portName) {
        if (portName == null || portName.isEmpty()) {
            return "";
        }
        int schemeEnd = portName.indexOf("://");
        if (schemeEnd > 0) {
            int atIndex = portName.indexOf('@', schemeEnd + 3);
            if (atIndex > schemeEnd + 3) {
                return portName.substring(0, schemeEnd + 3) + "***" + portName.substring(atIndex);
            }
        }
        return portName;
    }
}
