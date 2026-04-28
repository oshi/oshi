/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.Printer;
import oshi.hardware.common.AbstractPrinter;
import oshi.util.ExecutingCommand;
import oshi.util.driver.unix.Lpstat;

/**
 * CUPS-based printer implementation with shared lpstat fallback logic. Subclasses provide native CUPS access via JNA or
 * FFM.
 */
@Immutable
public abstract class CupsPrinter extends AbstractPrinter {

    protected CupsPrinter(String name, String driverName, String description, PrinterStatus status, String statusReason,
            boolean isDefault, boolean isLocal, String portName) {
        super(name, driverName, description, status, statusReason, isDefault, isLocal, portName);
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
            Map<String, String> portMap, Map<String, String> descriptionMap,
            java.util.function.Function<String, String> driverLookup, PrinterFactory factory) {
        List<Printer> printers = new ArrayList<>();

        for (String line : lpstatLines) {
            if (line.startsWith("printer ")) {
                String[] parts = line.split("\\s+");
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
