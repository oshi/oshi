/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.UnaryOperator;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.Printer;
import oshi.hardware.common.AbstractPrinter;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.driver.unix.Lpstat;

/**
 * CUPS-based printer implementation with shared lpstat fallback logic. Subclasses provide native CUPS access via JNA or
 * FFM.
 */
@Immutable
public abstract class CupsPrinter extends AbstractPrinter {

    /**
     * Creates a CupsPrinter with the given parameters.
     *
     * @param name         the printer name
     * @param driverName   the driver name
     * @param description  the description
     * @param status       the printer status
     * @param statusReason the status reason
     * @param isDefault    whether this is the default printer
     * @param isLocal      whether this is a local printer
     * @param portName     the port name
     */
    protected CupsPrinter(String name, String driverName, String description, PrinterStatus status, String statusReason,
            boolean isDefault, boolean isLocal, String portName) {
        super(name, driverName, description, status, statusReason, isDefault, isLocal, portName);
    }

    /** CUPS {@code ipp_pstate_t} value for an idle printer. */
    private static final int IPP_PRINTER_IDLE = 3;
    /** CUPS {@code ipp_pstate_t} value for a printer processing a job. */
    private static final int IPP_PRINTER_PROCESSING = 4;
    /** CUPS {@code ipp_pstate_t} value for a stopped printer. */
    private static final int IPP_PRINTER_STOPPED = 5;

    /**
     * Maps a CUPS printer state and its state reasons to a {@link PrinterStatus}. An error or fault in the reasons
     * takes precedence over the numeric state, matching the CUPS {@code ipp_pstate_t} values.
     *
     * @param state        the CUPS {@code printer-state} value as a string, or empty if unavailable
     * @param stateReasons the CUPS {@code printer-state-reasons} value, or empty/{@code "none"} if there are none
     * @return the mapped {@link PrinterStatus}, or {@link PrinterStatus#UNKNOWN} if the state is empty or unrecognized
     */
    protected static PrinterStatus parseStateFromCups(String state, String stateReasons) {
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
            case IPP_PRINTER_IDLE:
                return PrinterStatus.IDLE;
            case IPP_PRINTER_PROCESSING:
                return PrinterStatus.PRINTING;
            case IPP_PRINTER_STOPPED:
                return PrinterStatus.OFFLINE;
            default:
                return PrinterStatus.UNKNOWN;
        }
    }

    /**
     * Gets printers by parsing lpstat command output. Used as a fallback when libcups is unavailable.
     *
     * @param factory function to create concrete printer instances
     * @return list of printers
     */
    protected static List<Printer> getPrintersFromLpstat(PrinterFactory factory) {
        return getPrintersFromLpstat(ExecutingCommand.runNative(new String[] { "lpstat", "-p" }), factory);
    }

    /**
     * Parse lpstat -p output to build a list of printers.
     *
     * @param lpstatLines output of {@code lpstat -p}
     * @param factory     function to create concrete printer instances
     * @return list of printers
     */
    static List<Printer> getPrintersFromLpstat(List<String> lpstatLines, PrinterFactory factory) {
        return getPrintersFromLpstat(lpstatLines, Lpstat.queryDefaultPrinter(), Lpstat.queryPortMap(),
                Lpstat.queryDescriptionMap(), Lpstat::queryDriver, factory);
    }

    /**
     * Parse lpstat -p output to build a list of printers with pre-fetched data.
     *
     * @param lpstatLines    output of {@code lpstat -p}
     * @param defaultPrinter the default printer name
     * @param portMap        map of printer name to device URI
     * @param descriptionMap map of printer name to description
     * @param driverLookup   function to look up driver name for a printer
     * @param factory        function to create concrete printer instances
     * @return list of printers
     */
    static List<Printer> getPrintersFromLpstat(List<String> lpstatLines, String defaultPrinter,
            Map<String, String> portMap, Map<String, String> descriptionMap, UnaryOperator<String> driverLookup,
            PrinterFactory factory) {
        List<Printer> printers = new ArrayList<>();

        for (String line : lpstatLines) {
            if (line.startsWith("printer ")) {
                String[] parts = ParseUtil.whitespaces.split(line, -1);
                if (parts.length >= 3) {
                    String name = parts[1];
                    PrinterStatus status = Lpstat.parseStatus(line);
                    boolean isDefault = name.equals(defaultPrinter);
                    String portName = portMap.getOrDefault(name, "");
                    boolean isLocal = Lpstat.isLocalUri(portName);
                    String driverName = driverLookup.apply(name);
                    String description = descriptionMap.getOrDefault(name, "");
                    String statusReason = Lpstat.parseStatusReason(line);

                    printers.add(factory.create(name, driverName, description, status, statusReason, isDefault, isLocal,
                            portName));
                }
            }
        }
        return printers;
    }

    /**
     * Factory interface for creating concrete printer instances.
     */
    @FunctionalInterface
    public interface PrinterFactory {
        /**
         * Create a printer instance.
         *
         * @param name         printer name
         * @param driverName   driver name
         * @param description  description
         * @param status       status
         * @param statusReason status reason
         * @param isDefault    whether this is the default printer
         * @param isLocal      whether this is a local printer
         * @param portName     port/URI
         * @return a new Printer instance
         */
        Printer create(String name, String driverName, String description, PrinterStatus status, String statusReason,
                boolean isDefault, boolean isLocal, String portName);
    }
}
