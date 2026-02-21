/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.jna.platform.unix;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.ptr.PointerByReference;

/**
 * CUPS (Common Unix Printing System) library. This class should be considered non-API as it may be removed if/when its
 * code is incorporated into the JNA project.
 */
public interface Cups extends Library {

    Cups INSTANCE = Native.load("cups", Cups.class);

    // Printer state constants from cups/cups.h
    int IPP_PRINTER_IDLE = 3;
    int IPP_PRINTER_PROCESSING = 4;
    int IPP_PRINTER_STOPPED = 5;

    /**
     * CUPS destination (printer) structure.
     */
    @FieldOrder({ "name", "instance", "is_default", "num_options", "options" })
    class CupsDest extends Structure {
        public String name;
        public String instance;
        public int is_default;
        public int num_options;
        public Pointer options; // cups_option_t*

        public CupsDest() {
            super();
        }

        public CupsDest(Pointer p) {
            super(p);
            read();
        }
    }

    /**
     * Gets all available destinations (printers and classes).
     *
     * @param dests Pointer to receive destination array
     * @return Number of destinations
     */
    int cupsGetDests(PointerByReference dests);

    /**
     * Frees the memory used by a destination array.
     *
     * @param num_dests Number of destinations
     * @param dests     Pointer to destination array
     */
    void cupsFreeDests(int num_dests, Pointer dests);

    /**
     * Gets the default printer name.
     *
     * @return Default printer name or null if none
     */
    String cupsGetDefault();

    /**
     * Gets an option value from a destination.
     *
     * @param name        Option name
     * @param num_options Number of options
     * @param options     Pointer to options array
     * @return Option value or null if not found
     */
    String cupsGetOption(String name, int num_options, Pointer options);
}
