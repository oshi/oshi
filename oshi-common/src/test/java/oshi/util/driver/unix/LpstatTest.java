/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.unix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import oshi.hardware.Printer.PrinterStatus;

class LpstatTest {

    // Fixture: lpstat -d
    private static final List<String> LPSTAT_D = Arrays.asList("system default destination: HP_LaserJet");

    // Fixture: lpstat -v
    private static final List<String> LPSTAT_V = Arrays.asList(
            "device for HP_LaserJet: usb://HP/LaserJet%20Pro?serial=ABC123", "device for PDF_Printer: cups-pdf:/",
            "device for Network_Printer: ipp://192.168.1.100/ipp/print");

    // Fixture: lpstat -l -p
    private static final List<String> LPSTAT_LP = Arrays.asList(
            "printer HP_LaserJet is idle.  enabled since Mon 01 Jan 2024 12:00:00 AM",
            "\tDescription: HP LaserJet Pro MFP", "\tLocation: Office",
            "printer PDF_Printer disabled since Tue 02 Jan 2024 01:00:00 AM -",
            "\tDescription: CUPS-PDF Virtual Printer");

    // Fixture: lpoptions -p
    private static final List<String> LPOPTIONS = Arrays
            .asList("copies=1 device-uri=usb://HP/LaserJet printer-make-and-model='HP LaserJet Pro' printer-type=2");

    @Test
    void testQueryDefaultPrinter() {
        assertThat(Lpstat.queryDefaultPrinter(LPSTAT_D), is("HP_LaserJet"));
    }

    @Test
    void testQueryDefaultPrinterEmpty() {
        assertThat(Lpstat.queryDefaultPrinter(Collections.emptyList()), is(""));
    }

    @Test
    void testQueryDefaultPrinterNoDefault() {
        List<String> noDefault = Arrays.asList("no system default destination");
        assertThat(Lpstat.queryDefaultPrinter(noDefault), is(""));
    }

    @Test
    void testQueryPortMap() {
        Map<String, String> map = Lpstat.queryPortMap(LPSTAT_V);
        assertThat(map.size(), is(3));
        assertThat(map.get("HP_LaserJet"), is("usb://HP/LaserJet%20Pro?serial=ABC123"));
        assertThat(map.get("PDF_Printer"), is("cups-pdf:/"));
        assertThat(map.get("Network_Printer"), is("ipp://192.168.1.100/ipp/print"));
    }

    @Test
    void testQueryPortMapEmpty() {
        assertThat(Lpstat.queryPortMap(Collections.emptyList()).isEmpty(), is(true));
    }

    @Test
    void testQueryDescriptionMap() {
        Map<String, String> map = Lpstat.queryDescriptionMap(LPSTAT_LP);
        assertThat(map.size(), is(2));
        assertThat(map.get("HP_LaserJet"), is("HP LaserJet Pro MFP"));
        assertThat(map.get("PDF_Printer"), is("CUPS-PDF Virtual Printer"));
    }

    @Test
    void testQueryDescriptionMapEmpty() {
        assertThat(Lpstat.queryDescriptionMap(Collections.emptyList()).isEmpty(), is(true));
    }

    @Test
    void testQueryDriver() {
        assertThat(Lpstat.queryDriver(LPOPTIONS), is("HP LaserJet Pro"));
    }

    @Test
    void testQueryDriverNotFound() {
        assertThat(Lpstat.queryDriver(Arrays.asList("copies=1 device-uri=usb://HP")), is(""));
    }

    @Test
    void testQueryDriverEmpty() {
        assertThat(Lpstat.queryDriver(Collections.emptyList()), is(""));
    }

    @Test
    void testParseStatusIdle() {
        assertThat(Lpstat.parseStatus("printer HP_LaserJet is idle.  enabled since Mon 01 Jan"),
                is(PrinterStatus.IDLE));
    }

    @Test
    void testParseStatusDisabled() {
        assertThat(Lpstat.parseStatus("printer PDF_Printer disabled since Tue 02 Jan"), is(PrinterStatus.OFFLINE));
    }

    @Test
    void testParseStatusPrinting() {
        assertThat(Lpstat.parseStatus("printer HP_LaserJet now printing HP_LaserJet-42"), is(PrinterStatus.PRINTING));
    }

    @Test
    void testParseStatusError() {
        assertThat(Lpstat.parseStatus("printer Broken_Printer has error condition"), is(PrinterStatus.ERROR));
    }

    @Test
    void testParseStatusUnknown() {
        assertThat(Lpstat.parseStatus("printer Mystery_Printer something unexpected"), is(PrinterStatus.UNKNOWN));
    }

    @Test
    void testParseStatusReason() {
        assertThat(Lpstat.parseStatusReason("printer HP disabled - paused"), is("paused"));
    }

    @Test
    void testParseStatusReasonNone() {
        assertThat(Lpstat.parseStatusReason("printer HP is idle."), is(""));
    }

    @Test
    void testIsLocalUriUsb() {
        assertThat(Lpstat.isLocalUri("usb://HP/LaserJet"), is(true));
    }

    @Test
    void testIsLocalUriDev() {
        assertThat(Lpstat.isLocalUri("/dev/usb/lp0"), is(true));
    }

    @Test
    void testIsLocalUriNetwork() {
        assertThat(Lpstat.isLocalUri("ipp://192.168.1.100/ipp/print"), is(false));
    }

    @Test
    void testIsLocalUriLocalLpd() {
        assertThat(Lpstat.isLocalUri("lpd://localhost/queue"), is(true));
    }

    @Test
    void testIsLocalUriEmpty() {
        assertThat(Lpstat.isLocalUri(""), is(false));
    }
}
