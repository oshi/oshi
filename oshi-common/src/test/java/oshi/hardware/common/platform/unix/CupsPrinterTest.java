/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import oshi.hardware.Printer;
import oshi.hardware.Printer.PrinterStatus;

class CupsPrinterTest {

    /** Minimal concrete subclass for testing. */
    private static final class StubPrinter extends CupsPrinter {
        StubPrinter(String name, String driverName, String description, PrinterStatus status, String statusReason,
                boolean isDefault, boolean isLocal, String portName) {
            super(name, driverName, description, status, statusReason, isDefault, isLocal, portName);
        }
    }

    private static final CupsPrinter.PrinterFactory STUB_FACTORY = StubPrinter::new;

    // Fixture: lpstat -p output with two printers
    private static final List<String> LPSTAT_P = Arrays.asList(
            "printer HP_LaserJet is idle.  enabled since Mon 01 Jan 2024 12:00:00 AM",
            "printer PDF_Printer disabled since Tue 02 Jan 2024 - paused by admin");

    private static final Map<String, String> PORT_MAP = new HashMap<>();
    private static final Map<String, String> DESC_MAP = new HashMap<>();
    static {
        PORT_MAP.put("HP_LaserJet", "usb://HP/LaserJet");
        PORT_MAP.put("PDF_Printer", "cups-pdf:/");
        DESC_MAP.put("HP_LaserJet", "HP LaserJet Pro MFP");
        DESC_MAP.put("PDF_Printer", "CUPS-PDF Virtual Printer");
    }

    @Test
    void testGetPrintersFromLpstatTwoPrinters() {
        List<Printer> printers = CupsPrinter.getPrintersFromLpstat(LPSTAT_P, "HP_LaserJet", PORT_MAP, DESC_MAP,
                name -> "TestDriver", STUB_FACTORY);
        assertThat(printers, hasSize(2));

        Printer hp = printers.get(0);
        assertThat(hp.getName(), is("HP_LaserJet"));
        assertThat(hp.getStatus(), is(PrinterStatus.IDLE));
        assertThat(hp.isDefault(), is(true));
        assertThat(hp.isLocal(), is(true));
        assertThat(hp.getPortName(), is("usb://HP/LaserJet"));
        assertThat(hp.getDescription(), is("HP LaserJet Pro MFP"));
        assertThat(hp.getDriverName(), is("TestDriver"));

        Printer pdf = printers.get(1);
        assertThat(pdf.getName(), is("PDF_Printer"));
        assertThat(pdf.getStatus(), is(PrinterStatus.OFFLINE));
        assertThat(pdf.isDefault(), is(false));
        assertThat(pdf.getStatusReason(), is("paused by admin"));
    }

    @Test
    void testGetPrintersFromLpstatEmpty() {
        List<Printer> printers = CupsPrinter.getPrintersFromLpstat(Collections.emptyList(), "", Collections.emptyMap(),
                Collections.emptyMap(), name -> "", STUB_FACTORY);
        assertThat(printers, is(empty()));
    }

    @Test
    void testGetPrintersFromLpstatSkipsNonPrinterLines() {
        List<String> mixed = Arrays.asList("scheduler is running",
                "printer MyPrinter now printing MyPrinter-42. enabled since Mon 01 Jan",
                "no system default destination");
        List<Printer> printers = CupsPrinter.getPrintersFromLpstat(mixed, "", Collections.emptyMap(),
                Collections.emptyMap(), name -> "", STUB_FACTORY);
        assertThat(printers, hasSize(1));
        assertThat(printers.get(0).getName(), is("MyPrinter"));
        assertThat(printers.get(0).getStatus(), is(PrinterStatus.PRINTING));
    }
}
