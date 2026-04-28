/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.hardware.PhysicalMemory;
import oshi.hardware.VirtualMemory;
import oshi.util.Constants;

class AbstractGlobalMemoryTest {

    @Test
    void testToString() {
        AbstractGlobalMemory mem = new AbstractGlobalMemory() {
            @Override
            public long getTotal() {
                return 8L * 1024 * 1024 * 1024;
            }

            @Override
            public long getAvailable() {
                return 4L * 1024 * 1024 * 1024;
            }

            @Override
            public long getPageSize() {
                return 4096L;
            }

            @Override
            public VirtualMemory getVirtualMemory() {
                return null;
            }
        };
        String s = mem.toString();
        assertThat(s, containsString("Available:"));
        assertThat(s, containsString("4 GiB"));
        assertThat(s, containsString("8 GiB"));
    }

    // Fixture: dmidecode --type 17 with two populated DIMMs and one empty slot
    private static final List<String> DMI_TYPE_17 = Arrays.asList("# dmidecode 3.2", "Getting SMBIOS data from sysfs.",
            "SMBIOS 3.0.0 present.", "", "Handle 0x0038, DMI type 17, 40 bytes", "Memory Device",
            "\tArray Handle: 0x0036", "\tError Information Handle: Not Provided", "\tTotal Width: 72 bits",
            "\tData Width: 64 bits", "\tSize: 16384 MB", "\tForm Factor: DIMM", "\tSet: None", "\tLocator: DIMM_A1",
            "\tBank Locator: NODE 0", "\tType: DDR4", "\tType Detail: Synchronous Registered (Buffered)",
            "\tSpeed: 2666 MT/s", "\tManufacturer: Samsung", "\tSerial Number: 12345ABC",
            "\tAsset Tag: DIMM_A1_AssetTag", "\tPart Number: M393A2K43CB2-CTD", "\tRank: 2",
            "\tConfigured Memory Speed: 2666 MT/s", "", "Handle 0x0039, DMI type 17, 40 bytes", "Memory Device",
            "\tArray Handle: 0x0036", "\tError Information Handle: Not Provided", "\tTotal Width: 72 bits",
            "\tData Width: 64 bits", "\tSize: 8192 MB", "\tForm Factor: DIMM", "\tSet: None", "\tLocator: DIMM_B1",
            "\tBank Locator: NODE 1", "\tType: DDR4", "\tType Detail: Synchronous Registered (Buffered)",
            "\tSpeed: 2400 MT/s", "\tManufacturer: Micron", "\tSerial Number: 67890DEF",
            "\tAsset Tag: DIMM_B1_AssetTag", "\tPart Number: MTA18ASF1G72PZ", "\tRank: 1",
            "\tConfigured Memory Speed: 2400 MT/s", "", "Handle 0x003A, DMI type 17, 40 bytes", "Memory Device",
            "\tArray Handle: 0x0036", "\tError Information Handle: Not Provided", "\tTotal Width: Unknown",
            "\tData Width: Unknown", "\tSize: No Module Installed", "\tForm Factor: DIMM", "\tSet: None",
            "\tLocator: DIMM_C1", "\tBank Locator: NODE 2", "\tType: Unknown", "\tType Detail: None");

    @Test
    void testParsePhysicalMemoryTwoDimms() {
        List<PhysicalMemory> result = AbstractGlobalMemory.getPhysicalMemory(DMI_TYPE_17);
        assertThat(result, hasSize(2));

        PhysicalMemory first = result.get(0);
        assertThat(first.getBankLabel(), is("NODE 0/DIMM_A1"));
        assertThat(first.getCapacity(), is(16384L * 1024 * 1024));
        assertThat(first.getClockSpeed(), is(2_666_000_000L));
        assertThat(first.getManufacturer(), is("Samsung"));
        assertThat(first.getMemoryType(), is("DDR4"));
        assertThat(first.getPartNumber(), is("M393A2K43CB2-CTD"));
        assertThat(first.getSerialNumber(), is("12345ABC"));

        PhysicalMemory second = result.get(1);
        assertThat(second.getBankLabel(), is("NODE 1/DIMM_B1"));
        assertThat(second.getCapacity(), is(8192L * 1024 * 1024));
        assertThat(second.getClockSpeed(), is(2_400_000_000L));
        assertThat(second.getManufacturer(), is("Micron"));
        assertThat(second.getMemoryType(), is("DDR4"));
        assertThat(second.getPartNumber(), is("MTA18ASF1G72PZ"));
        assertThat(second.getSerialNumber(), is("67890DEF"));
    }

    @Test
    void testParsePhysicalMemoryEmpty() {
        assertThat(AbstractGlobalMemory.getPhysicalMemory(Collections.emptyList()), is(empty()));
    }

    @Test
    void testParsePhysicalMemorySingleDimm() {
        List<String> single = Arrays.asList("Handle 0x0038, DMI type 17, 40 bytes", "Memory Device", "\tSize: 4096 MB",
                "\tLocator: DIMM0", "\tBank Locator: Bank0", "\tType: DDR3", "\tSpeed: 1600 MT/s",
                "\tManufacturer: Kingston", "\tPart Number: KVR16N11/4", "\tSerial Number: AABB1122");
        List<PhysicalMemory> result = AbstractGlobalMemory.getPhysicalMemory(single);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getBankLabel(), is("Bank0/DIMM0"));
        assertThat(result.get(0).getCapacity(), is(4096L * 1024 * 1024));
    }

    @Test
    void testParsePhysicalMemoryNoStateLeak() {
        // Second DIMM omits Manufacturer and Part Number — should NOT inherit from first
        List<String> twoEntries = Arrays.asList("Handle 0x0038, DMI type 17, 40 bytes", "Memory Device",
                "\tSize: 8192 MB", "\tLocator: DIMM_A1", "\tBank Locator: Bank0", "\tType: DDR4", "\tSpeed: 2666 MT/s",
                "\tManufacturer: Samsung", "\tPart Number: M393A2K43CB2-CTD", "\tSerial Number: SN111",
                "Handle 0x0039, DMI type 17, 40 bytes", "Memory Device", "\tSize: 4096 MB", "\tLocator: DIMM_B1",
                "\tBank Locator: Bank1", "\tType: DDR4", "\tSpeed: 2400 MT/s", "\tSerial Number: SN222");
        List<PhysicalMemory> result = AbstractGlobalMemory.getPhysicalMemory(twoEntries);
        assertThat(result, hasSize(2));

        assertThat(result.get(0).getManufacturer(), is("Samsung"));
        assertThat(result.get(0).getPartNumber(), is("M393A2K43CB2-CTD"));

        // Second DIMM should have defaults, not Samsung/M393...
        assertThat(result.get(1).getBankLabel(), is("Bank1/DIMM_B1"));
        assertThat(result.get(1).getCapacity(), is(4096L * 1024 * 1024));
        assertThat(result.get(1).getManufacturer(), is(Constants.UNKNOWN));
        assertThat(result.get(1).getPartNumber(), is(Constants.UNKNOWN));
    }
}
