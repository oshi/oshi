/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix;

import java.util.List;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.Printer;

/**
 * Native-free CUPS printer implementation. Queries printers via the {@code lpstat} command rather than libcups, so it
 * works without JNA or FFM native access. Used by platforms whose hardware abstraction layer lives entirely in
 * {@code oshi-common} (currently NetBSD).
 */
@Immutable
public final class LpstatPrinter extends CupsPrinter {

    LpstatPrinter(String name, String driverName, String description, PrinterStatus status, String statusReason,
            boolean isDefault, boolean isLocal, String portName) {
        super(name, driverName, description, status, statusReason, isDefault, isLocal, portName);
    }

    /**
     * Gets the list of printers known to CUPS by parsing {@code lpstat -p} output.
     *
     * @return list of printers, or an empty list if {@code lpstat} is unavailable.
     */
    public static List<Printer> getPrinters() {
        return getPrintersFromLpstat(LpstatPrinter::new);
    }
}
