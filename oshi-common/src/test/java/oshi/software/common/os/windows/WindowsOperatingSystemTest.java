/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

class WindowsOperatingSystemTest {

    @Test
    void testParseCodeNameSingleBitEnterprise() {
        assertThat(WindowsOperatingSystem.parseCodeName(0x00000002), is("Enterprise"));
    }

    @Test
    void testParseCodeNameSingleBitHome() {
        assertThat(WindowsOperatingSystem.parseCodeName(0x00000200), is("Home"));
    }

    @Test
    void testParseCodeNameMultipleBits() {
        assertThat(WindowsOperatingSystem.parseCodeName(0x00000202), is("Enterprise,Home"));
    }

    @Test
    void testParseCodeNameZeroMask() {
        assertThat(WindowsOperatingSystem.parseCodeName(0), is(""));
    }

    @Test
    void testParseCodeNameAllBitsSet() {
        int allBits = 0x00000002 | 0x00000004 | 0x00000008 | 0x00000080 | 0x00000200 | 0x00000400 | 0x00002000
                | 0x00004000 | 0x00008000;
        assertThat(WindowsOperatingSystem.parseCodeName(allBits),
                is("Enterprise,BackOffice,Communications Server,Datacenter,Home,Web Server,Storage Server,"
                        + "Compute Cluster,Home Server"));
    }
}
