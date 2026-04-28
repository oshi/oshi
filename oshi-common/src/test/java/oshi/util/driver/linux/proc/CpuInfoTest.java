/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.linux.proc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.util.Constants;
import oshi.util.tuples.Quartet;

class CpuInfoTest {

    // Fixture: ARM /proc/cpuinfo (Raspberry Pi style)
    private static final List<String> CPUINFO_ARM = Arrays.asList("processor\t: 0", "BogoMIPS\t: 108.00",
            "Features\t: fp asimd evtstrm crc32 cpuid", "CPU implementer\t: 0x41", "CPU architecture: 8",
            "CPU variant\t: 0x0", "CPU part\t: 0xd08", "CPU revision\t: 3", "", "Hardware\t: BCM2835",
            "Revision\t: a020d3", "Serial\t\t: 00000000abcdef01");

    // Fixture: x86 /proc/cpuinfo
    private static final List<String> CPUINFO_X86 = Arrays.asList("processor\t: 0", "vendor_id\t: GenuineIntel",
            "model name\t: Intel(R) Core(TM) i7-8700K CPU @ 3.70GHz",
            "flags\t\t: fpu vme de pse tsc msr pae mce cx8 apic", "", "processor\t: 1", "vendor_id\t: GenuineIntel",
            "model name\t: Intel(R) Core(TM) i7-8700K CPU @ 3.70GHz",
            "flags\t\t: fpu vme de pse tsc msr pae mce cx8 apic");

    @Test
    void testQueryCpuManufacturerARM() {
        assertThat(CpuInfo.queryCpuManufacturer(CPUINFO_ARM), is("ARM"));
    }

    @Test
    void testQueryCpuManufacturerQualcomm() {
        List<String> qualcomm = Arrays.asList("CPU implementer\t: 0x51");
        assertThat(CpuInfo.queryCpuManufacturer(qualcomm), is("Qualcomm"));
    }

    @Test
    void testQueryCpuManufacturerUnknown() {
        List<String> unknown = Arrays.asList("CPU implementer\t: 0xff");
        assertThat(CpuInfo.queryCpuManufacturer(unknown), is(nullValue()));
    }

    @Test
    void testQueryCpuManufacturerX86() {
        // x86 doesn't have "CPU implementer" line
        assertThat(CpuInfo.queryCpuManufacturer(CPUINFO_X86), is(nullValue()));
    }

    @Test
    void testQueryCpuManufacturerEmpty() {
        assertThat(CpuInfo.queryCpuManufacturer(Collections.emptyList()), is(nullValue()));
    }

    @Test
    void testQueryBoardInfoRaspberryPi() {
        Quartet<String, String, String, String> info = CpuInfo.queryBoardInfo(CPUINFO_ARM);
        assertThat(info.getA(), is("Sony UK")); // manufacturer from revision digit '0'
        assertThat(info.getB(), is("BCM2835")); // model from Hardware
        assertThat(info.getC(), is("a020d3")); // version from Revision
        assertThat(info.getD(), is("00000000abcdef01")); // serial
    }

    @Test
    void testQueryBoardInfoEgoman() {
        List<String> egoman = Arrays.asList("Revision\t: a120d3");
        Quartet<String, String, String, String> info = CpuInfo.queryBoardInfo(egoman);
        assertThat(info.getA(), is("Egoman"));
    }

    @Test
    void testQueryBoardInfoUnknownManufacturer() {
        List<String> unknown = Arrays.asList("Revision\t: a920d3");
        Quartet<String, String, String, String> info = CpuInfo.queryBoardInfo(unknown);
        assertThat(info.getA(), is(Constants.UNKNOWN));
    }

    @Test
    void testQueryBoardInfoX86() {
        // x86 doesn't have Hardware/Revision/Serial
        Quartet<String, String, String, String> info = CpuInfo.queryBoardInfo(CPUINFO_X86);
        assertThat(info.getA(), is(nullValue()));
        assertThat(info.getB(), is(nullValue()));
        assertThat(info.getC(), is(nullValue()));
        assertThat(info.getD(), is(nullValue()));
    }

    @Test
    void testQueryBoardInfoEmpty() {
        Quartet<String, String, String, String> info = CpuInfo.queryBoardInfo(Collections.emptyList());
        assertThat(info.getA(), is(nullValue()));
    }

    @Test
    void testQueryFeatureFlagsX86() {
        List<String> flags = CpuInfo.queryFeatureFlags(CPUINFO_X86);
        // Two identical flag lines should be deduplicated to one
        assertThat(flags, hasSize(1));
        assertThat(flags.get(0).contains("fpu"), is(true));
    }

    @Test
    void testQueryFeatureFlagsARM() {
        List<String> flags = CpuInfo.queryFeatureFlags(CPUINFO_ARM);
        assertThat(flags, hasSize(1));
        assertThat(flags.get(0).contains("asimd"), is(true));
    }

    @Test
    void testQueryFeatureFlagsEmpty() {
        assertThat(CpuInfo.queryFeatureFlags(Collections.emptyList()), is(empty()));
    }

    @Nested
    @EnabledOnOs(OS.LINUX)
    class LiveTests {
        @Test
        void testQueries() {
            assertDoesNotThrow(() -> CpuInfo.queryCpuManufacturer());
            assertDoesNotThrow(() -> CpuInfo.queryBoardInfo());
        }

        @Test
        void testQueryBoardInfoAccessors() {
            Quartet<String, String, String, String> info = CpuInfo.queryBoardInfo();
            assertDoesNotThrow(info::getA);
            assertDoesNotThrow(info::getB);
            assertDoesNotThrow(info::getC);
            assertDoesNotThrow(info::getD);
        }

        @Test
        void testQueryFeatureFlagLines() {
            List<String> flagLines = CpuInfo.queryFeatureFlags();
            assertThat(flagLines, hasSize(greaterThan(0)));
        }
    }
}
