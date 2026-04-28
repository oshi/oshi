/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.Printer;
import oshi.hardware.common.platform.unix.CupsPrinter;
import oshi.jna.platform.unix.Cups;
import oshi.jna.platform.unix.Cups.CupsDest;
import oshi.util.ParseUtil;

/**
 * CUPS-based printer implementation using JNA. Falls back to lpstat when libcups is unavailable.
 */
@Immutable
public final class CupsPrinterJNA extends CupsPrinter {

    private static final Logger LOG = LoggerFactory.getLogger(CupsPrinterJNA.class);

    private static final boolean HAS_CUPS;

    static {
        boolean hasCups = false;
        try {
            @SuppressWarnings("unused")
            Cups lib = Cups.INSTANCE;
            hasCups = true;
        } catch (UnsatisfiedLinkError e) {
            LOG.debug("libcups not found. Falling back to lpstat command.");
        }
        HAS_CUPS = hasCups;
    }

    CupsPrinterJNA(String name, String driverName, String description, PrinterStatus status, String statusReason,
            boolean isDefault, boolean isLocal, String portName) {
        super(name, driverName, description, status, statusReason, isDefault, isLocal, portName);
    }

    /**
     * Gets printers using CUPS. Uses libcups if available, otherwise falls back to lpstat command.
     *
     * @return A list of printers.
     */
    public static List<Printer> getPrinters() {
        if (HAS_CUPS) {
            return getPrintersFromLibCups();
        }
        return getPrintersFromLpstat(CupsPrinterJNA::new);
    }

    private static List<Printer> getPrintersFromLibCups() {
        List<Printer> printers = new ArrayList<>();
        PointerByReference destsRef = new PointerByReference();
        int numDests = Cups.INSTANCE.cupsGetDests(destsRef);
        Pointer destsPtr = destsRef.getValue();

        if (destsPtr != null && numDests > 0) {
            try {
                CupsDest[] dests = (CupsDest[]) new CupsDest(destsPtr).toArray(numDests);
                for (CupsDest dest : dests) {
                    if (dest.name == null) {
                        continue;
                    }
                    String name = dest.name;
                    boolean isDefault = dest.is_default != 0;

                    String deviceUri = "";
                    String printerInfo = "";
                    String printerMakeModel = "";
                    String printerState = "";
                    String stateReasons = "";

                    if (dest.num_options > 0 && dest.options != null) {
                        deviceUri = getOption(dest, "device-uri");
                        printerInfo = getOption(dest, "printer-info");
                        printerMakeModel = getOption(dest, "printer-make-and-model");
                        printerState = getOption(dest, "printer-state");
                        stateReasons = getOption(dest, "printer-state-reasons");
                    }

                    PrinterStatus status = parseStateFromCups(printerState, stateReasons);
                    String statusReason = "none".equals(stateReasons) ? "" : stateReasons;
                    int printerType = ParseUtil.parseIntOrDefault(getOption(dest, "printer-type"), 0);
                    boolean isLocal = (printerType & Cups.CUPS_PRINTER_REMOTE) == 0;

                    printers.add(new CupsPrinterJNA(name, printerMakeModel, printerInfo, status, statusReason,
                            isDefault, isLocal, deviceUri));
                }
            } finally {
                Cups.INSTANCE.cupsFreeDests(numDests, destsPtr);
            }
        }
        return printers;
    }

    private static String getOption(CupsDest dest, String optionName) {
        String value = Cups.INSTANCE.cupsGetOption(optionName, dest.num_options, dest.options);
        return value != null ? value : "";
    }

    private static PrinterStatus parseStateFromCups(String state, String stateReasons) {
        if (!stateReasons.isEmpty() && !"none".equals(stateReasons)) {
            String lower = stateReasons.toLowerCase(Locale.ROOT);
            if (lower.contains("error") || lower.contains("fault")) {
                return PrinterStatus.ERROR;
            }
        }
        if (state.isEmpty()) {
            return PrinterStatus.UNKNOWN;
        }
        int stateInt = ParseUtil.parseIntOrDefault(state, -1);
        switch (stateInt) {
            case Cups.IPP_PRINTER_IDLE:
                return PrinterStatus.IDLE;
            case Cups.IPP_PRINTER_PROCESSING:
                return PrinterStatus.PRINTING;
            case Cups.IPP_PRINTER_STOPPED:
                return PrinterStatus.OFFLINE;
            default:
                return PrinterStatus.UNKNOWN;
        }
    }
}
