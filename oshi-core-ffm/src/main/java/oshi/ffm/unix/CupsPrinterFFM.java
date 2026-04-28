/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.unix;

import static java.lang.foreign.ValueLayout.ADDRESS;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.Immutable;
import oshi.ffm.ForeignFunctions;
import oshi.hardware.Printer;
import oshi.hardware.common.platform.unix.CupsPrinter;
import oshi.util.ParseUtil;

/**
 * CUPS-based printer implementation using the Java FFM API. Falls back to lpstat when libcups is unavailable.
 */
@Immutable
public final class CupsPrinterFFM extends CupsPrinter {

    private static final Logger LOG = LoggerFactory.getLogger(CupsPrinterFFM.class);

    CupsPrinterFFM(String name, String driverName, String description, PrinterStatus status, String statusReason,
            boolean isDefault, boolean isLocal, String portName) {
        super(name, driverName, description, status, statusReason, isDefault, isLocal, portName);
    }

    /**
     * Gets printers using CUPS. Uses libcups via FFM if available, otherwise falls back to the {@code lpstat} command.
     *
     * @return a list of printers
     */
    public static List<Printer> getPrinters() {
        if (CupsFunctions.isAvailable()) {
            return getPrintersFromLibCups();
        }
        LOG.debug("libcups not available, falling back to lpstat.");
        return getPrintersFromLpstat(CupsPrinterFFM::new);
    }

    private static List<Printer> getPrintersFromLibCups() {
        List<Printer> printers = new ArrayList<>();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment ptrSlot = arena.allocate(ADDRESS);
            int numDests = CupsFunctions.cupsGetDests(ptrSlot);
            if (numDests <= 0) {
                return printers;
            }
            MemorySegment destsPtr = ptrSlot.get(ADDRESS, 0);
            if (destsPtr == null || destsPtr.equals(MemorySegment.NULL)) {
                return printers;
            }
            try {
                MemorySegment destsArray = destsPtr.reinterpret(CupsFunctions.CUPS_DEST_T.byteSize() * numDests, arena,
                        null);
                for (int i = 0; i < numDests; i++) {
                    long offset = i * CupsFunctions.CUPS_DEST_T.byteSize();
                    MemorySegment dest = destsArray.asSlice(offset, CupsFunctions.CUPS_DEST_T.byteSize());

                    MemorySegment namePtr = (MemorySegment) CupsFunctions.CUPS_DEST_NAME.get(dest, 0L);
                    if (namePtr == null || namePtr.equals(MemorySegment.NULL)) {
                        continue;
                    }
                    String name = ForeignFunctions.getStringFromNativePointer(namePtr, arena);
                    int isDefaultInt = (int) CupsFunctions.CUPS_DEST_IS_DEFAULT.get(dest, 0L);
                    boolean isDefault = isDefaultInt != 0;
                    int numOptions = (int) CupsFunctions.CUPS_DEST_NUM_OPTIONS.get(dest, 0L);
                    MemorySegment options = (MemorySegment) CupsFunctions.CUPS_DEST_OPTIONS.get(dest, 0L);

                    String deviceUri = CupsFunctions.getOption("device-uri", numOptions, options, arena);
                    String printerInfo = CupsFunctions.getOption("printer-info", numOptions, options, arena);
                    String printerMakeModel = CupsFunctions.getOption("printer-make-and-model", numOptions, options,
                            arena);
                    String printerState = CupsFunctions.getOption("printer-state", numOptions, options, arena);
                    String stateReasons = CupsFunctions.getOption("printer-state-reasons", numOptions, options, arena);
                    String printerTypeStr = CupsFunctions.getOption("printer-type", numOptions, options, arena);

                    PrinterStatus status = parseStateFromCups(printerState, stateReasons);
                    String statusReason = "none".equals(stateReasons) ? "" : stateReasons;
                    int printerType = ParseUtil.parseIntOrDefault(printerTypeStr, 0);
                    boolean isLocal = (printerType & CupsFunctions.CUPS_PRINTER_REMOTE) == 0;

                    printers.add(new CupsPrinterFFM(name, printerMakeModel, printerInfo, status, statusReason,
                            isDefault, isLocal, deviceUri));
                }
            } finally {
                CupsFunctions.cupsFreeDests(numDests, destsPtr);
            }
        } catch (Throwable e) {
            LOG.warn("Failed to query printers from libcups: {}", e.getMessage(), e);
        }
        return printers;
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
        switch (ParseUtil.parseIntOrDefault(state, -1)) {
            case CupsFunctions.IPP_PRINTER_IDLE:
                return PrinterStatus.IDLE;
            case CupsFunctions.IPP_PRINTER_PROCESSING:
                return PrinterStatus.PRINTING;
            case CupsFunctions.IPP_PRINTER_STOPPED:
                return PrinterStatus.OFFLINE;
            default:
                return PrinterStatus.UNKNOWN;
        }
    }
}
