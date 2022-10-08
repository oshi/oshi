/*
 * Copyright 2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.unix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

@DisabledOnOs({ OS.WINDOWS, OS.MAC })
class XrandrTest {
    @Test
    void testGetEdidArrays() {
        for (byte[] edid : Xrandr.getEdidArrays()) {
            assertThat("Edid length must be at least 128", edid.length, greaterThanOrEqualTo(128));
        }
    }
}
