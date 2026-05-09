/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.hardware.PhysicalMemory;
import oshi.util.Constants;

class MacGlobalMemoryTest {

    // Fixture: typical system_profiler SPMemoryDataType output
    // Note: real output has " :" (space before colon) in bank labels
    private static final List<String> SP_MEMORY_TWO_BANKS = Arrays.asList("Memory:", "", "    Memory Slots:", "",
            "      ECC: Disabled", "      Upgradeable Memory: Yes", "", "        BANK 0/DIMM0 :", "",
            "          Size: 8 GB", "          Type: DDR4", "          Speed: 2400 MHz",
            "          Manufacturer: Samsung", "          Part Number: M471A1K43CB1-CRC",
            "          Serial Number: 12345678", "          Status: OK", "", "        BANK 1/DIMM0 :", "",
            "          Size: 8 GB", "          Type: DDR4", "          Speed: 2400 MHz",
            "          Manufacturer: Hynix", "          Part Number: HMA81GS6AFR8N-UH",
            "          Serial Number: 87654321", "          Status: OK");

    @Test
    void testParseSystemProfilerMemoryTwoBanks() {
        List<PhysicalMemory> result = MacGlobalMemory.parseSystemProfilerMemory(SP_MEMORY_TWO_BANKS);
        assertThat(result, hasSize(2));

        PhysicalMemory bank0 = result.get(0);
        assertThat(bank0.getBankLabel(), is("BANK 0/DIMM0"));
        assertThat(bank0.getCapacity(), is(greaterThan(0L)));
        assertThat(bank0.getMemoryType(), is("DDR4"));
        assertThat(bank0.getManufacturer(), is("Samsung"));
        assertThat(bank0.getClockSpeed(), is(greaterThan(0L)));
        assertThat(bank0.getPartNumber(), is("M471A1K43CB1-CRC"));

        PhysicalMemory bank1 = result.get(1);
        assertThat(bank1.getBankLabel(), is("BANK 1/DIMM0"));
        assertThat(bank1.getManufacturer(), is("Hynix"));
        assertThat(bank1.getSerialNumber(), is("87654321"));
        assertThat(bank1.getPartNumber(), is("HMA81GS6AFR8N-UH"));
    }

    @Test
    void testParseSystemProfilerMemoryEmpty() {
        List<PhysicalMemory> result = MacGlobalMemory.parseSystemProfilerMemory(Collections.emptyList());
        // Always returns at least one entry (the final add outside the loop)
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getBankLabel(), is(Constants.UNKNOWN));
        assertThat(result.get(0).getCapacity(), is(0L));
    }

    @Test
    void testParseSystemProfilerMemorySingleBank() {
        List<String> singleBank = Arrays.asList("        BANK 0/DIMM0 :", "          Size: 16 GB",
                "          Type: LPDDR4X", "          Speed: 4267 MHz", "          Manufacturer: Micron",
                "          Part Number: MT53E1G32D4NQ-046", "          Serial Number: ABCDEF01");
        List<PhysicalMemory> result = MacGlobalMemory.parseSystemProfilerMemory(singleBank);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getBankLabel(), is("BANK 0/DIMM0"));
        assertThat(result.get(0).getMemoryType(), is("LPDDR4X"));
        assertThat(result.get(0).getManufacturer(), is("Micron"));
        assertThat(result.get(0).getPartNumber(), is("MT53E1G32D4NQ-046"));
    }

    @Test
    void testParseSystemProfilerMemoryNoColon() {
        // Bank label without trailing colon — substring logic skipped
        List<String> noColon = Arrays.asList("        BANK 0", "          Size: 4 GB", "          Type: DDR3");
        List<PhysicalMemory> result = MacGlobalMemory.parseSystemProfilerMemory(noColon);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getBankLabel(), is("BANK 0"));
    }
}
