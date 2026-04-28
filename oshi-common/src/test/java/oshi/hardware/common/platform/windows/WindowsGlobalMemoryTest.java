/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

class WindowsGlobalMemoryTest {

    @Test
    void testMemoryTypeAllCases() {
        String[] expected = { "Unknown", "Other", "DRAM", "Synchronous DRAM", "Cache DRAM", "EDO", "EDRAM", "VRAM",
                "SRAM", "RAM", "ROM", "Flash", "EEPROM", "FEPROM", "EPROM", "CDRAM", "3DRAM", "SDRAM", "SGRAM", "RDRAM",
                "DDR", "DDR2", "BRAM", "DDR FB-DIMM" };
        for (int i = 0; i < expected.length; i++) {
            assertThat("memoryType(" + i + ")", WindowsGlobalMemory.memoryType(i), is(expected[i]));
        }
    }

    @Test
    void testMemoryTypeFallsBackToSmBios() {
        assertThat(WindowsGlobalMemory.memoryType(0x1A), is("DDR4"));
        assertThat(WindowsGlobalMemory.memoryType(0x22), is("DDR5"));
    }

    @Test
    void testSmBiosMemoryTypeAllCases() {
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x01), is("Other"));
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x03), is("DRAM"));
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x04), is("EDRAM"));
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x05), is("VRAM"));
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x06), is("SRAM"));
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x07), is("RAM"));
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x08), is("ROM"));
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x09), is("FLASH"));
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x0A), is("EEPROM"));
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x0B), is("FEPROM"));
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x0C), is("EPROM"));
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x0D), is("CDRAM"));
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x0E), is("3DRAM"));
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x0F), is("SDRAM"));
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x10), is("SGRAM"));
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x11), is("RDRAM"));
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x12), is("DDR"));
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x13), is("DDR2"));
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x14), is("DDR2 FB-DIMM"));
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x18), is("DDR3"));
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x19), is("FBD2"));
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x1A), is("DDR4"));
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x1B), is("LPDDR"));
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x1C), is("LPDDR2"));
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x1D), is("LPDDR3"));
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x1E), is("LPDDR4"));
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x1F), is("Logical non-volatile device"));
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x20), is("HBM"));
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x21), is("HBM2"));
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x22), is("DDR5"));
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x23), is("LPDDR5"));
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x24), is("HBM3"));
    }

    @Test
    void testSmBiosMemoryTypeUnknown() {
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x02), is("Unknown"));
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0xFF), is("Unknown"));
        // Gap values between defined ranges
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x15), is("Unknown"));
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x16), is("Unknown"));
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x17), is("Unknown"));
    }
}
