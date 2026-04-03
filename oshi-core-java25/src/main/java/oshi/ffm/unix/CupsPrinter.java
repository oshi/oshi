/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.unix;

import static java.lang.foreign.ValueLayout.ADDRESS;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.Immutable;
import oshi.ffm.ForeignFunctions;
import oshi.hardware.Printer;
import oshi.hardware.common.AbstractPrinter;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * CUPS-based printer implementation using the Java FFM API. Uses libcups if available, otherwise falls back to the
 * {@code lpstat} command.
 */
@Immutable
public final class CupsPrinter extends AbstractPrinter {

    private static final Logger LOG = LoggerFactory.getLogger(CupsPrinter.class);

    // Local URI schemes for directly-attached or local printers
    private static final List<String> LOCAL_URI_PREFIXES = List.of("usb:", "parallel:", "serial:", "file:", "direct:",
            "hp:", "lpd://127.", "lpd://localhost", "socket://127.", "socket://localhost");

    CupsPrinter(String name, String driverName, String description, PrinterStatus status, String statusReason,
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
        return getPrintersFromLpstat();
    }

    private static List<Printer> getPrintersFromLibCups() {
        List<Printer> printers = new ArrayList<>();
        try (Arena arena = Arena.ofConfined()) {
            // cupsGetDests(cups_dest_t **dests) — allocate a pointer slot to receive the array pointer
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

                    printers.add(new CupsPrinter(name, printerMakeModel, printerInfo, status, statusReason, isDefault,
                            isLocal, deviceUri));
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

    private static List<Printer> getPrintersFromLpstat() {
        List<Printer> printers = new ArrayList<>();
        String defaultPrinter = getDefaultPrinter();
        Map<String, String> portMap = parsePortMap();
        Map<String, String> descriptionMap = parseDescriptionMap();

        for (String line : ExecutingCommand.runNative(new String[] { "lpstat", "-p" })) {
            if (line.startsWith("printer ")) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 3) {
                    String name = parts[1];
                    PrinterStatus status = parseStatusFromLpstat(line);
                    boolean isDefault = name.equals(defaultPrinter);
                    String portName = portMap.getOrDefault(name, "");
                    boolean isLocal = isLocalUri(portName);
                    String driverName = getDriverForPrinter(name);
                    String description = descriptionMap.getOrDefault(name, "");
                    String statusReason = getStatusReasonFromLpstat(line);
                    printers.add(new CupsPrinter(name, driverName, description, status, statusReason, isDefault,
                            isLocal, portName));
                }
            }
        }
        return printers;
    }

    private static Map<String, String> parsePortMap() {
        Map<String, String> map = new HashMap<>();
        for (String line : ExecutingCommand.runNative(new String[] { "lpstat", "-v" })) {
            if (line.contains("device for")) {
                int forIdx = line.indexOf("device for ") + 11;
                int colonIdx = line.indexOf(':', forIdx);
                if (colonIdx > forIdx) {
                    map.put(line.substring(forIdx, colonIdx).trim(), line.substring(colonIdx + 1).trim());
                }
            }
        }
        return map;
    }

    private static Map<String, String> parseDescriptionMap() {
        Map<String, String> map = new HashMap<>();
        String currentPrinter = null;
        for (String line : ExecutingCommand.runNative(new String[] { "lpstat", "-l", "-p" })) {
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

    private static String getDriverForPrinter(String printerName) {
        for (String line : ExecutingCommand.runNative(new String[] { "lpoptions", "-p", printerName })) {
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

    private static String getDefaultPrinter() {
        for (String line : ExecutingCommand.runNative(new String[] { "lpstat", "-d" })) {
            if (line.contains("default destination:")) {
                String[] parts = line.split(":", 2);
                if (parts.length >= 2) {
                    return parts[1].trim();
                }
            }
        }
        return "";
    }

    private static PrinterStatus parseStatusFromLpstat(String line) {
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

    private static String getStatusReasonFromLpstat(String line) {
        int dashIdx = line.indexOf(" - ");
        return dashIdx > 0 ? line.substring(dashIdx + 3).trim() : "";
    }

    private static boolean isLocalUri(String uri) {
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
