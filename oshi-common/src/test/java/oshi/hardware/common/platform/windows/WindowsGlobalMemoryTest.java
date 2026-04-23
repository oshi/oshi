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
    void testMemoryTypeKnownValues() {
        assertThat(WindowsGlobalMemory.memoryType(0), is("Unknown"));
        assertThat(WindowsGlobalMemory.memoryType(1), is("Other"));
        assertThat(WindowsGlobalMemory.memoryType(2), is("DRAM"));
        assertThat(WindowsGlobalMemory.memoryType(17), is("SDRAM"));
        assertThat(WindowsGlobalMemory.memoryType(20), is("DDR"));
        assertThat(WindowsGlobalMemory.memoryType(21), is("DDR2"));
        assertThat(WindowsGlobalMemory.memoryType(22), is("BRAM"));
        assertThat(WindowsGlobalMemory.memoryType(23), is("DDR FB-DIMM"));
    }

    @Test
    void testMemoryTypeFallsBackToSmBios() {
        // Values > 23 fall through to smBiosMemoryType
        assertThat(WindowsGlobalMemory.memoryType(0x1A), is("DDR4"));
        assertThat(WindowsGlobalMemory.memoryType(0x22), is("DDR5"));
    }

    @Test
    void testSmBiosMemoryTypeKnownValues() {
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x01), is("Other"));
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x03), is("DRAM"));
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x12), is("DDR"));
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x13), is("DDR2"));
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x14), is("DDR2 FB-DIMM"));
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x18), is("DDR3"));
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x1A), is("DDR4"));
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x1E), is("LPDDR4"));
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x22), is("DDR5"));
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x23), is("LPDDR5"));
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x24), is("HBM3"));
    }

    @Test
    void testSmBiosMemoryTypeUnknown() {
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0x02), is("Unknown"));
        assertThat(WindowsGlobalMemory.smBiosMemoryType(0xFF), is("Unknown"));
    }
}
