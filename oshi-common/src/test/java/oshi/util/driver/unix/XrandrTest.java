/*
 * Copyright 2022-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.unix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

class XrandrTest {

    // Fixture: xrandr --verbose output with one EDID block (128 bytes = 256 hex chars)
    private static List<String> createXrandrWithEdid() {
        List<String> lines = new ArrayList<>();
        lines.add("Screen 0: minimum 8 x 8, current 1920 x 1080, maximum 32767 x 32767");
        lines.add("HDMI-1 connected primary 1920x1080+0+0");
        lines.add("\tEDID:");
        // 8 lines x 32 hex chars = 256 hex chars = 128 bytes (standard EDID block)
        lines.add("\t\t00ffffffffffff001e6d085b0b0b0b0b");
        lines.add("\t\t0c1c0104b53c2278fb2eb5ae4f46a527");
        lines.add("\t\t0d5054254b80714f81809500a9c0b300");
        lines.add("\t\td1c001010101565e00a0a0a029503020");
        lines.add("\t\t35000f282100001a000000fd00384b1e");
        lines.add("\t\t5a1900000a202020202020000000fc00");
        lines.add("\t\t4c4720554c545241474541520000ff00");
        lines.add("\t\t3630344e54505a4832313337370a0100");
        lines.add("  1920x1080 (0x48) 148.500MHz +HSync +VSync *current +preferred");
        return lines;
    }

    @Test
    void testGetEdidArraysSingleDisplay() {
        List<byte[]> edids = Xrandr.getEdidArrays(createXrandrWithEdid());
        assertThat(edids, hasSize(1));
        byte[] edid = edids.get(0);
        assertThat(edid.length, is(128));
        // Verify EDID magic header: 00 FF FF FF FF FF FF 00
        assertThat(edid[0], is((byte) 0x00));
        assertThat(edid[1], is((byte) 0xFF));
        assertThat(edid[2], is((byte) 0xFF));
        assertThat(edid[7], is((byte) 0x00));
    }

    @Test
    void testGetEdidArraysEmpty() {
        assertThat(Xrandr.getEdidArrays(Collections.emptyList()), is(empty()));
    }

    @Test
    void testGetEdidArraysNoEdidBlock() {
        List<String> noEdid = new ArrayList<>();
        noEdid.add("Screen 0: minimum 8 x 8, current 1920 x 1080");
        noEdid.add("HDMI-1 connected primary 1920x1080+0+0");
        assertThat(Xrandr.getEdidArrays(noEdid), is(empty()));
    }

    @Nested
    @DisabledOnOs({ OS.WINDOWS, OS.MAC })
    class LiveTests {
        @Test
        void testGetEdidArrays() {
            List<byte[]> edids = Xrandr.getEdidArrays();
            assumeFalse(edids.isEmpty(), "No displays found (headless system); skipping");
            for (byte[] edid : edids) {
                assertThat("Edid length must be at least 128", edid.length, greaterThanOrEqualTo(128));
            }
        }
    }
}
