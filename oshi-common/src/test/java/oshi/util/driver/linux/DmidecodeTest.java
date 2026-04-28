/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesRegex;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.TestConstants;
import oshi.util.tuples.Pair;

class DmidecodeTest {

    // Fixture: typical dmidecode -t system output
    private static final List<String> SYSTEM_OUTPUT = Arrays.asList("# dmidecode 3.2",
            "Getting SMBIOS data from sysfs.", "SMBIOS 2.7 present.", "", "Handle 0x0001, DMI type 1, 27 bytes",
            "System Information", "\tManufacturer: Dell Inc.", "\tProduct Name: PowerEdge R720",
            "\tVersion: Not Specified", "\tSerial Number: ABC1234", "\tUUID: 4C4C4544-0044-4810-8031-B4C04F333132",
            "\tWake-up Type: Power Switch", "\tSKU Number: Not Specified", "\tFamily: Not Specified");

    // Fixture: typical dmidecode -t bios output (uppercase BIOS Revision, as seen in real dmidecode)
    private static final List<String> BIOS_OUTPUT = Arrays.asList("# dmidecode 3.2", "SMBIOS 2.7 present.", "",
            "Handle 0x0000, DMI type 0, 24 bytes", "BIOS Information", "\tVendor: Dell Inc.", "\tVersion: 2.5.2",
            "\tRelease Date: 01/20/2014", "\tAddress: 0xF0000", "\tRuntime Size: 64 kB", "\tROM Size: 16 MB",
            "\tBIOS Revision: 2.5");

    @Test
    void testQuerySerialNumber() {
        assertThat(Dmidecode.querySerialNumber(SYSTEM_OUTPUT), is("ABC1234"));
    }

    @Test
    void testQuerySerialNumberNotFound() {
        assertThat(Dmidecode.querySerialNumber(Collections.emptyList()), is(nullValue()));
    }

    @Test
    void testQueryUUID() {
        assertThat(Dmidecode.queryUUID(SYSTEM_OUTPUT), is("4C4C4544-0044-4810-8031-B4C04F333132"));
    }

    @Test
    void testQueryUUIDNotFound() {
        assertThat(Dmidecode.queryUUID(Collections.emptyList()), is(nullValue()));
    }

    @Test
    void testQueryBiosNameRev() {
        Pair<String, String> result = Dmidecode.queryBiosNameRev(BIOS_OUTPUT);
        assertThat(result.getA(), is("SMBIOS 2.7"));
        assertThat(result.getB(), is("2.5"));
    }

    @Test
    void testQueryBiosNameRevEmpty() {
        Pair<String, String> result = Dmidecode.queryBiosNameRev(Collections.emptyList());
        assertThat(result.getA(), is(nullValue()));
        assertThat(result.getB(), is(nullValue()));
    }

    @Test
    void testQueryBiosNameRevNoRevision() {
        List<String> noRev = Arrays.asList("SMBIOS 3.0 present.", "Handle 0x0000, DMI type 0, 24 bytes");
        Pair<String, String> result = Dmidecode.queryBiosNameRev(noRev);
        assertThat(result.getA(), is("SMBIOS 3.0"));
        assertThat(result.getB(), is(nullValue()));
    }

    @Test
    void testQueryBiosNameRevLowercaseVariant() {
        List<String> lowerCase = Arrays.asList("SMBIOS 3.1 present.", "\tbios revision: 1.2");
        Pair<String, String> result = Dmidecode.queryBiosNameRev(lowerCase);
        assertThat(result.getA(), is("SMBIOS 3.1"));
        assertThat(result.getB(), is("1.2"));
    }

    @Nested
    @EnabledOnOs(OS.LINUX)
    class LiveTests {
        @Test
        void testQueries() {
            assertDoesNotThrow(() -> Dmidecode.querySerialNumber());
            assertDoesNotThrow(() -> Dmidecode.queryBiosNameRev());

            final String uuid = Dmidecode.queryUUID();
            if (uuid != null) {
                assertThat("Test Dmidecode queryUUID format", uuid, matchesRegex(TestConstants.UUID_REGEX));
            }
        }

        @Test
        void testQueryBiosNameRevReturnsPair() {
            Pair<String, String> result = Dmidecode.queryBiosNameRev();
            assertThat(result, notNullValue());
        }
    }
}
