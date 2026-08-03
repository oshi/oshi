/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import oshi.software.os.OperatingSystem.OSVersionInfo;
import oshi.util.tuples.Pair;

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

    @Test
    void testResolveVersionAliasWindows11() {
        assertThat("Build 22000 is the first Windows 11 build",
                WindowsOperatingSystem.resolveVersionAlias("10", "22000"), is("11"));
        assertThat("Later builds are still Windows 11", WindowsOperatingSystem.resolveVersionAlias("10", "26100"),
                is("11"));
        assertThat("Earlier builds remain Windows 10", WindowsOperatingSystem.resolveVersionAlias("10", "19045"),
                is("10"));
    }

    @Test
    void testResolveVersionAliasServer() {
        assertThat("Build 17763 is Server 2019", WindowsOperatingSystem.resolveVersionAlias("Server 2016", "17763"),
                is("Server 2019"));
        assertThat("Build 20348 is Server 2022", WindowsOperatingSystem.resolveVersionAlias("Server 2016", "20348"),
                is("Server 2022"));
        assertThat("Build 26100 is Server 2025", WindowsOperatingSystem.resolveVersionAlias("Server 2016", "26100"),
                is("Server 2025"));
        assertThat("Build 14393 remains Server 2016",
                WindowsOperatingSystem.resolveVersionAlias("Server 2016", "14393"), is("Server 2016"));
    }

    @Test
    void testResolveVersionAliasUnmapped() {
        assertThat("A version with no alias is returned unchanged",
                WindowsOperatingSystem.resolveVersionAlias("8.1", "9600"), is("8.1"));
    }

    @Test
    void testParseVersionInfoStripsWindowsPrefix() {
        Pair<String, OSVersionInfo> pair = WindowsOperatingSystem.parseVersionInfo("Windows 10", "", 0, "19045");
        assertThat("Family is always Windows", pair.getA(), is("Windows"));
        OSVersionInfo version = pair.getB();
        assertThat("The \"Windows \" prefix is stripped", version.getVersion(), is("10"));
        assertThat("An empty suite mask yields an empty code name", version.getCodeName(), is(""));
        assertThat("The build number is passed through", version.getBuildNumber(), is("19045"));
    }

    @Test
    void testParseVersionInfoAbbreviatesServicePack() {
        OSVersionInfo version = WindowsOperatingSystem
                .parseVersionInfo("Windows 7", "Service Pack 1", 0x00000200, "7601").getB();
        assertThat("Service Pack is abbreviated to SP", version.getVersion(), is("7 SP1"));
        assertThat("The suite mask is decoded", version.getCodeName(), is("Home"));
    }

    @Test
    void testParseVersionInfoIgnoresAbsentServicePack() {
        assertThat("An empty service pack is not appended",
                WindowsOperatingSystem.parseVersionInfo("Windows 10", "", 0, "19045").getB().getVersion(), is("10"));
        assertThat("An unknown service pack is not appended",
                WindowsOperatingSystem.parseVersionInfo("Windows 10", "unknown", 0, "19045").getB().getVersion(),
                is("10"));
    }

    @Test
    void testParseVersionInfoAppliesAlias() {
        assertThat("A Windows 10 name with a Windows 11 build is aliased",
                WindowsOperatingSystem.parseVersionInfo("Windows 10", "", 0, "22631").getB().getVersion(), is("11"));
    }

    @Test
    void testParseVersionInfoWithoutWindowsPrefix() {
        assertThat("A name without the prefix is used as-is",
                WindowsOperatingSystem.parseVersionInfo("Windows", "", 0, "").getB().getVersion(), is("Windows"));
    }
}
