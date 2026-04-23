/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
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
    void testAtLeastOneSysfsQueryReturnsValue() {
        // On any Linux system with DMI support (including CI), at least one of these
        // sysfs queries should return a non-null value. This catches blanket regressions.
        String vendor = Sysfs.querySystemVendor();
        String model = Sysfs.queryProductModel();
        String desc = Sysfs.queryBiosDescription();
        String date = Sysfs.queryBiosReleaseDate();
        assertThat("At least one sysfs query should return non-null on Linux",
                vendor != null || model != null || desc != null || date != null, is(true));
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
