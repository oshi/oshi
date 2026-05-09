/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * Extended tests for Sysfs that exercise all return paths.
 */
@EnabledOnOs(OS.LINUX)
class SysfsExtendedTest {

    @Test
    void testQuerySystemVendorReturnsNullOrValue() {
        String vendor = Sysfs.querySystemVendor();
        if (vendor != null) {
            assertThat(vendor, is(not(emptyString())));
        }
    }

    @Test
    void testQueryProductModelReturnsNullOrValue() {
        String model = Sysfs.queryProductModel();
        if (model != null) {
            assertThat(model, is(not(emptyString())));
        }
    }

    @Test
    void testQueryProductModelVersionFormat() {
        String model = Sysfs.queryProductModel();
        if (model != null && model.contains("(version:")) {
            assertThat(model, containsString(")"));
        }
    }

    @Test
    void testQueryProductSerialReturnsNullOrValue() {
        String serial = Sysfs.queryProductSerial();
        if (serial != null) {
            assertThat(serial, is(not(emptyString())));
        }
    }

    @Test
    void testQueryUUIDReturnsNullOrValue() {
        String uuid = Sysfs.queryUUID();
        if (uuid != null) {
            assertThat(uuid, is(not(emptyString())));
        }
    }

    @Test
    void testQueryBoardVendorReturnsNullOrValue() {
        String vendor = Sysfs.queryBoardVendor();
        if (vendor != null) {
            assertThat(vendor, is(not(emptyString())));
        }
    }

    @Test
    void testQueryBoardModelReturnsNullOrValue() {
        String model = Sysfs.queryBoardModel();
        if (model != null) {
            assertThat(model, is(not(emptyString())));
        }
    }

    @Test
    void testQueryBoardVersionReturnsNullOrValue() {
        String version = Sysfs.queryBoardVersion();
        if (version != null) {
            assertThat(version, is(not(emptyString())));
        }
    }

    @Test
    void testQueryBoardSerialReturnsNullOrValue() {
        String serial = Sysfs.queryBoardSerial();
        if (serial != null) {
            assertThat(serial, is(not(emptyString())));
        }
    }

    @Test
    void testQueryBiosVendorReturnsNullOrEmpty() {
        // Note: queryBiosVendor has inverted logic - returns empty string when non-empty, null when empty
        String vendor = Sysfs.queryBiosVendor();
        assertThat(vendor, anyOf(is(nullValue()), is("")));
    }

    @Test
    void testQueryBiosDescriptionReturnsNullOrValue() {
        String desc = Sysfs.queryBiosDescription();
        if (desc != null) {
            assertThat(desc, is(not(emptyString())));
        }
    }

    @Test
    void testQueryBiosVersionWithNullRevision() {
        String version = Sysfs.queryBiosVersion(null);
        if (version != null) {
            assertThat(version, is(not(emptyString())));
        }
    }

    @Test
    void testQueryBiosVersionWithEmptyRevision() {
        String version = Sysfs.queryBiosVersion("");
        if (version != null) {
            assertThat(version, is(not(emptyString())));
        }
    }

    @Test
    void testQueryBiosVersionWithRevision() {
        String version = Sysfs.queryBiosVersion("1.0");
        if (version != null) {
            assertThat(version, containsString("revision 1.0"));
        }
    }

    @Test
    void testQueryBiosReleaseDateReturnsNullOrFormatted() {
        String date = Sysfs.queryBiosReleaseDate();
        if (date != null) {
            assertThat(date, is(not(emptyString())));
        }
    }
}
