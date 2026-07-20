/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.solaris;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import oshi.hardware.common.platform.unix.solaris.SolarisComputerSystem.SmbType;

class SolarisComputerSystemTest {

    @Test
    void testParseSmbios() {
        // Representative smbios output with BIOS, System, and Baseboard sections
        List<String> smbios = Arrays.asList(//
                "ID    SIZE TYPE", //
                "0     87   SMB_TYPE_BIOS (BIOS Information)", //
                "", //
                "  Vendor: Parallels Software International Inc.", //
                "  Version String: 11.2.1 (32686)", //
                "  Release Date: 07/15/2016", //
                "  Address Segment: 0xf000", //
                "", //
                "ID    SIZE TYPE", //
                "1     177  SMB_TYPE_SYSTEM (system information)", //
                "", //
                "  Manufacturer: Parallels Software International Inc.", //
                "  Product: Parallels Virtual Platform", //
                "  Version: None", //
                "  Serial Number: Parallels-45 2E 7E 2D 57 5C 4B 59 B1 30 28 81 B7 81 89 34", //
                "  UUID: 452e7e2d-575c04b59-b130-2881b7818934", //
                "", //
                "ID    SIZE TYPE", //
                "2     90   SMB_TYPE_BASEBOARD (base board)", //
                "", //
                "  Manufacturer: Parallels Software International Inc.", //
                "  Product: Parallels Virtual Platform", //
                "  Version: None", //
                "  Serial Number: None");

        EnumMap<SmbType, Map<String, String>> result = SolarisComputerSystem.parseSmbios(smbios);

        Map<String, String> bios = result.get(SmbType.SMB_TYPE_BIOS);
        assertThat(bios.get("Vendor"), is("Parallels Software International Inc."));
        assertThat(bios.get("Version String"), is("11.2.1 (32686)"));
        assertThat(bios.get("Release Date"), is("07/15/2016"));

        Map<String, String> system = result.get(SmbType.SMB_TYPE_SYSTEM);
        assertThat(system.get("Manufacturer"), is("Parallels Software International Inc."));
        assertThat(system.get("Product"), is("Parallels Virtual Platform"));
        assertThat(system.get("Serial Number"), is("Parallels-45 2E 7E 2D 57 5C 4B 59 B1 30 28 81 B7 81 89 34"));
        assertThat(system.get("UUID"), is("452e7e2d-575c04b59-b130-2881b7818934"));

        Map<String, String> baseboard = result.get(SmbType.SMB_TYPE_BASEBOARD);
        assertThat(baseboard.get("Manufacturer"), is("Parallels Software International Inc."));
        assertThat(baseboard.get("Product"), is("Parallels Virtual Platform"));
        assertThat(baseboard.get("Serial Number"), is("None"));
    }

    @Test
    void testParseSmbiosEmpty() {
        EnumMap<SmbType, Map<String, String>> result = SolarisComputerSystem.parseSmbios(Collections.emptyList());
        assertThat(result.get(SmbType.SMB_TYPE_BIOS).isEmpty(), is(true));
        assertThat(result.get(SmbType.SMB_TYPE_SYSTEM).isEmpty(), is(true));
        assertThat(result.get(SmbType.SMB_TYPE_BASEBOARD).isEmpty(), is(true));
    }

    @Test
    void testParseSerialFromPrtconf() {
        List<String> prtconf = Arrays.asList(//
                "System Configuration:  Sun Microsystems  sun4u", //
                "    name:  'SUNW,Ultra-5_10'", //
                "      chassis-sn:  'ABC123'", //
                "    banner-name:  'Sun Ultra 5/10 UPA/PCI'");
        assertThat(SolarisComputerSystem.parseSerialFromPrtconf(prtconf), is("ABC123"));
    }

    @Test
    void testParseSerialFromPrtconfEmpty() {
        assertThat(SolarisComputerSystem.parseSerialFromPrtconf(Collections.emptyList()), is(""));
    }

    @Test
    void testParseSerialFromPrtconfNoMatch() {
        List<String> prtconf = Arrays.asList(//
                "System Configuration:  Sun Microsystems  sun4u", //
                "    name:  'SUNW,Ultra-5_10'");
        assertThat(SolarisComputerSystem.parseSerialFromPrtconf(prtconf), is(""));
    }
}
