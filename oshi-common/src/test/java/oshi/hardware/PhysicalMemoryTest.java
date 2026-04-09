/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

class PhysicalMemoryTest {

    private static final PhysicalMemory MEM = new PhysicalMemory("Bank 0", 8_589_934_592L, 3_200_000_000L, "Samsung",
            "DDR4", "M378A1K43CB2", "12345678");

    @Test
    void testGetters() {
        assertThat(MEM.getBankLabel(), is("Bank 0"));
        assertThat(MEM.getCapacity(), is(8_589_934_592L));
        assertThat(MEM.getClockSpeed(), is(3_200_000_000L));
        assertThat(MEM.getManufacturer(), is("Samsung"));
        assertThat(MEM.getMemoryType(), is("DDR4"));
        assertThat(MEM.getPartNumber(), is("M378A1K43CB2"));
        assertThat(MEM.getSerialNumber(), is("12345678"));
    }

    @Test
    void testToString() {
        String s = MEM.toString();
        assertThat(s, containsString("Bank 0"));
        assertThat(s, containsString("Samsung"));
        assertThat(s, containsString("DDR4"));
    }
}
