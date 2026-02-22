/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.Test;

import oshi.SystemInfo;

/**
 * Test Printer
 */
class PrinterTest {

    /**
     * Testing printers, each attribute.
     */
    @Test
    void testPrinters() {
        SystemInfo info = new SystemInfo();
        for (Printer printer : info.getHardware().getPrinters()) {
            assertThat("Printer's name should not be null", printer.getName(), is(notNullValue()));
            assertThat("Printer's driver name should not be null", printer.getDriverName(), is(notNullValue()));
            assertThat("Printer's description should not be null", printer.getDescription(), is(notNullValue()));
            assertThat("Printer's status should not be null", printer.getStatus(), is(notNullValue()));
            assertThat("Printer's status reason should not be null", printer.getStatusReason(), is(notNullValue()));
            assertThat("Printer's port name should not be null", printer.getPortName(), is(notNullValue()));
            // Exercise boolean methods (no null check needed)
            printer.isDefault();
            printer.isLocal();
        }
    }
}
