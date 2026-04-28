/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesRegex;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.TestConstants;

class LshalTest {

    // Fixture: lshal output with system hardware info
    private static final List<String> LSHAL_OUTPUT = Arrays.asList("udi = '/org/freedesktop/Hal/devices/computer'",
            "  system.hardware.vendor = 'Dell Inc.'  (string)",
            "  system.hardware.product = 'PowerEdge R720'  (string)", "  system.hardware.serial = 'ABC1234'  (string)",
            "  system.hardware.uuid = '4C4C4544-0044-4810-8031-B4C04F333132'  (string)",
            "  system.firmware.vendor = 'Dell Inc.'  (string)");

    @Test
    void testQuerySerialNumber() {
        assertThat(Lshal.querySerialNumber(LSHAL_OUTPUT), is("ABC1234"));
    }

    @Test
    void testQuerySerialNumberNotFound() {
        assertThat(Lshal.querySerialNumber(Collections.emptyList()), is(nullValue()));
    }

    @Test
    void testQueryUUID() {
        assertThat(Lshal.queryUUID(LSHAL_OUTPUT), is("4C4C4544-0044-4810-8031-B4C04F333132"));
    }

    @Test
    void testQueryUUIDNotFound() {
        assertThat(Lshal.queryUUID(Collections.emptyList()), is(nullValue()));
    }

    @Test
    void testLiveQueries() {
        assertDoesNotThrow(() -> Lshal.querySerialNumber());

        final String uuid = Lshal.queryUUID();
        if (uuid != null) {
            assertThat("Test Lshal queryUUID format", uuid, matchesRegex(TestConstants.UUID_REGEX));
        }
    }
}
