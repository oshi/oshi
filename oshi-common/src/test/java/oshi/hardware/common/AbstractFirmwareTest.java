/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import oshi.util.Constants;

class AbstractFirmwareTest {

    private static final AbstractFirmware FIRMWARE = new AbstractFirmware() {
        @Override
        public String getManufacturer() {
            return "AMI";
        }

        @Override
        public String getVersion() {
            return "F.50";
        }
    };

    @Test
    void testDefaults() {
        assertThat(FIRMWARE.getName(), is(Constants.UNKNOWN));
        assertThat(FIRMWARE.getDescription(), is(Constants.UNKNOWN));
        assertThat(FIRMWARE.getReleaseDate(), is(Constants.UNKNOWN));
    }

    @Test
    void testToString() {
        String s = FIRMWARE.toString();
        assertThat(s, containsString("AMI"));
        assertThat(s, containsString("F.50"));
    }
}
