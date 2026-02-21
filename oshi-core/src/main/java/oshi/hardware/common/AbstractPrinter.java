/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.Printer;
import oshi.jna.platform.unix.Cups;
import oshi.jna.platform.unix.Cups.CupsDest;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Abstract Printer with CUPS-based default implementation for Unix-like systems.
 */
@Immutable
public abstract class AbstractPrinter implements Printer {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractPrinter.class);

    /** Identifies whether libcups is available. */
    public static final boolean HAS_CUPS;

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

    private final String name;
    private final String driverName;
    private final String description;
    private final PrinterStatus status;
    private final boolean isDefault;
    private final boolean isLocal;
    private final String portName;

    protected AbstractPrinter(String name, String driverName, String description, PrinterStatus status,
            boolean isDefault, boolean isLocal, String portName) {
        this.name = name;
        this.driverName = driverName;
        this.description = description;
        this.status = status;
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
                + status + ", isDefault=" + isDefault + ", isLocal=" + isLocal + ", portName="
                + redactPortName(portName) + "]";
    }

    private static String redactPortName(String portName) {
        if (portName == null || portName.isEmpty()) {
            return "";
        }
        int schemeEnd = portName.indexOf("://");
        if (schemeEnd > 0) {
            int atIndex = portName.indexOf('@', schemeEnd + 3);
            if (atIndex > schemeEnd + 3) {
                // Has userinfo - redact it
                return portName.substring(0, schemeEnd + 3) + "***" + portName.substring(atIndex);
            }
        }
        return portName;
    }

    /**
     * Gets printers using CUPS. Uses libcups if available, otherwise falls back to lpstat command.
     *
     * @return A list of printers.
     */
    public static List<Printer> getPrintersFromCups() {
        if (HAS_CUPS) {
            return getPrintersFromLibCups();
        }
        return getPrintersFromLpstat();
    }

    /**
     * Gets printers using libcups native library.
     *
     * @return A list of printers.
     */
    private static List<Printer> getPrintersFromLibCups() {
        List<Printer> printers = new ArrayList<>();
        PointerByReference destsRef = new PointerByReference();
        int numDests = Cups.INSTANCE.cupsGetDests(destsRef);
        Pointer destsPtr = destsRef.getValue();

        if (destsPtr != null && numDests > 0) {
            try {
                int destSize = new CupsDest().size();
                for (int i = 0; i < numDests; i++) {
                    CupsDest dest = new CupsDest(destsPtr.share((long) i * destSize));

                    String name = dest.name;
                    boolean isDefault = dest.is_default != 0;

                    // Get options
                    String deviceUri = "";
                    String printerInfo = "";
                    String printerMakeModel = "";
                    String printerState = "";

                    if (dest.num_options > 0 && dest.options != null) {
                        deviceUri = getOption(dest, "device-uri");
                        printerInfo = getOption(dest, "printer-info");
                        printerMakeModel = getOption(dest, "printer-make-and-model");
                        printerState = getOption(dest, "printer-state");
                    }

                    PrinterStatus status = parseStateFromCups(printerState);
                    boolean isLocal = deviceUri.startsWith("usb:") || deviceUri.startsWith("/dev")
                            || deviceUri.startsWith("parallel:") || deviceUri.startsWith("serial:")
                            || deviceUri.startsWith("file:");

                    printers.add(new CupsPrinter(name, printerMakeModel, printerInfo, status, isDefault, isLocal,
                            deviceUri));
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

    private static PrinterStatus parseStateFromCups(String state) {
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

    /**
     * Gets printers using lpstat command. Fallback when libcups is not available.
     *
     * @return A list of printers.
     */
    private static List<Printer> getPrintersFromLpstat() {
        List<Printer> printers = new ArrayList<>();
        String defaultPrinter = getDefaultPrinterFromCups();

        // lpstat -p gives printer names and status
        // Format: "printer PrinterName is idle." or "printer PrinterName disabled since..."
        for (String line : ExecutingCommand.runNative("lpstat -p")) {
            if (line.startsWith("printer ")) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 3) {
                    String name = parts[1];
                    PrinterStatus status = parseStatusFromLpstat(line);
                    boolean isDefault = name.equals(defaultPrinter);

                    // Get additional info
                    String portName = getPortForPrinter(name);
                    boolean isLocal = portName.startsWith("usb:") || portName.startsWith("/dev")
                            || portName.startsWith("parallel:") || portName.startsWith("serial:")
                            || portName.startsWith("file:");
                    String driverName = getDriverForPrinter(name);
                    String description = getDescriptionForPrinter(name);

                    printers.add(new CupsPrinter(name, driverName, description, status, isDefault, isLocal, portName));
                }
            }
        }
        return printers;
    }

    private static String getDefaultPrinterFromCups() {
        // lpstat -d gives: "system default destination: PrinterName"
        for (String line : ExecutingCommand.runNative("lpstat -d")) {
            if (line.contains("default destination:")) {
                String[] parts = line.split(":");
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

    private static String getPortForPrinter(String printerName) {
        // lpstat -v gives: "device for PrinterName: uri"
        for (String line : ExecutingCommand.runNative(new String[] { "lpstat", "-v", printerName })) {
            if (line.contains("device for")) {
                int colonIdx = line.indexOf(':', line.indexOf("device for"));
                if (colonIdx >= 0 && colonIdx < line.length() - 1) {
                    return line.substring(colonIdx + 1).trim();
                }
            }
        }
        return "";
    }

    private static String getDriverForPrinter(String printerName) {
        // lpoptions -p printer gives printer-make-and-model (IPP attribute matching Win32_Printer.DRIVERNAME)
        for (String line : ExecutingCommand.runNative(new String[] { "lpoptions", "-p", printerName })) {
            int idx = line.indexOf("printer-make-and-model='");
            if (idx >= 0) {
                int start = idx + 24; // length of "printer-make-and-model='"
                int end = line.indexOf('\'', start);
                if (end > start) {
                    return line.substring(start, end);
                }
            }
        }
        return "";
    }

    private static String getDescriptionForPrinter(String printerName) {
        // lpstat -l -p gives Description (user-friendly label)
        for (String line : ExecutingCommand.runNative(new String[] { "lpstat", "-l", "-p", printerName })) {
            if (line.trim().startsWith("Description:")) {
                return line.substring(line.indexOf(':') + 1).trim();
            }
        }
        return "";
    }

    /**
     * Concrete CUPS printer implementation for Unix-like systems.
     */
    @Immutable
    private static final class CupsPrinter extends AbstractPrinter {
        CupsPrinter(String name, String driverName, String description, PrinterStatus status, boolean isDefault,
                boolean isLocal, String portName) {
            super(name, driverName, description, status, isDefault, isLocal, portName);
        }
    }
}
