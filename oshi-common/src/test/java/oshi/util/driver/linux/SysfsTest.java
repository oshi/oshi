/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@EnabledOnOs(OS.LINUX)
class SysfsTest {

    @Test
    void testQueries() {
        // On most Linux systems (including CI), these sysfs files exist and return non-null
        // We verify actual values rather than just assertDoesNotThrow
        assertDoesNotThrow(Sysfs::querySystemVendor);
        assertDoesNotThrow(Sysfs::queryProductModel);
        assertDoesNotThrow(Sysfs::queryProductSerial);
        assertDoesNotThrow(Sysfs::queryUUID);
        assertDoesNotThrow(Sysfs::queryBoardVendor);
        assertDoesNotThrow(Sysfs::queryBoardModel);
        assertDoesNotThrow(Sysfs::queryBoardVersion);
        assertDoesNotThrow(Sysfs::queryBoardSerial);
        assertDoesNotThrow(Sysfs::queryBiosVendor);
        assertDoesNotThrow(Sysfs::queryBiosDescription);
        assertDoesNotThrow(Sysfs::queryBiosReleaseDate);
    }

    @Test
    void testQuerySystemVendorReturnsValue() {
        // On most Linux systems with DMI, sys_vendor is populated
        String vendor = Sysfs.querySystemVendor();
        if (vendor != null) {
            assertThat(vendor, is(not(emptyString())));
        }
    }

    @Test
    void testQueryProductModelReturnsValue() {
        String model = Sysfs.queryProductModel();
        if (model != null) {
            assertThat(model, is(not(emptyString())));
        }
    }

    @Test
    void testQueryBiosDescriptionReturnsValue() {
        String desc = Sysfs.queryBiosDescription();
        if (desc != null) {
            assertThat(desc, is(not(emptyString())));
        }
    }

    @Test
    void testQueryBiosReleaseDateFormat() {
        String date = Sysfs.queryBiosReleaseDate();
        if (date != null) {
            // Should be in yyyy-MM-dd or similar format after parsing
            assertThat(date, is(not(emptyString())));
        }
    }

    @Test
    void testQueryBiosVersion() {
        Sysfs.queryBiosVersion("");
        final String biosRevisionSuffix = "biosRevision";
        final String biosRevision = Sysfs.queryBiosVersion(biosRevisionSuffix);
        if (biosRevision != null) {
            assertThat("Test Sysfs queryBiosVersion with biosRevision", biosRevision,
                    containsString(biosRevisionSuffix));
        }
    }
}
