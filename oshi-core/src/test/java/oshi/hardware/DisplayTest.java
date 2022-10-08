/*
 * Copyright 2016-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.SystemInfo;

/**
 * Tests Displays
 */
class DisplayTest {

    /**
     * Test displays
     */
    @Test
    void testDisplay() {
        SystemInfo si = new SystemInfo();
        List<Display> displays = si.getHardware().getDisplays();
        for (Display d : displays) {
            assertThat("EDID Byte length should be at least 128", d.getEdid().length, is(greaterThanOrEqualTo(128)));
        }
    }
}
