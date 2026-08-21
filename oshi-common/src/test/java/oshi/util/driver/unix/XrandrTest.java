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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.util.tuples.Pair;

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

    // Fixture: xrandr --verbose output with CONNECTOR_ID after EDID (real order)
    private static List<String> createXrandrWithConnectorId() {
        List<String> lines = new ArrayList<>();
        lines.add("Screen 0: minimum 8 x 8, current 1920 x 1080, maximum 32767 x 32767");
        lines.add("DP2 connected primary 1920x1080+0+0 (normal left inverted right x axis y axis) 530mm x 300mm");
        lines.add("\tEDID:");
        lines.add("\t\t00ffffffffffff001e6d085b0b0b0b0b");
        lines.add("\t\t0c1c0104b53c2278fb2eb5ae4f46a527");
        lines.add("\t\t0d5054254b80714f81809500a9c0b300");
        lines.add("\t\td1c001010101565e00a0a0a029503020");
        lines.add("\t\t35000f282100001a000000fd00384b1e");
        lines.add("\t\t5a1900000a202020202020000000fc00");
        lines.add("\t\t4c4720554c545241474541520000ff00");
        lines.add("\t\t3630344e54505a4832313337370a0100");
        lines.add("\tCONNECTOR_ID: 96");
        lines.add("\t\tsupported: 96");
        lines.add("  1920x1080 (0x48) 148.500MHz +HSync +VSync *current +preferred");
        return lines;
    }

    // Fixture: two connected displays, one with CONNECTOR_ID (after EDID), one without
    private static List<String> createXrandrTwoDisplays() {
        List<String> lines = new ArrayList<>();
        lines.add("Screen 0: minimum 8 x 8, current 3840 x 1080, maximum 32767 x 32767");
        lines.add("DP2 connected primary 1920x1080+0+0 (normal left inverted right x axis y axis) 530mm x 300mm");
        lines.add("\tEDID:");
        lines.add("\t\t00ffffffffffff001e6d085b0b0b0b0b");
        lines.add("\t\t0c1c0104b53c2278fb2eb5ae4f46a527");
        lines.add("\t\t0d5054254b80714f81809500a9c0b300");
        lines.add("\t\td1c001010101565e00a0a0a029503020");
        lines.add("\t\t35000f282100001a000000fd00384b1e");
        lines.add("\t\t5a1900000a202020202020000000fc00");
        lines.add("\t\t4c4720554c545241474541520000ff00");
        lines.add("\t\t3630344e54505a4832313337370a0100");
        lines.add("\tCONNECTOR_ID: 96");
        lines.add("\t\tsupported: 96");
        lines.add("HDMI1 connected 1920x1080+1920+0 (normal left inverted right x axis y axis) 600mm x 340mm");
        lines.add("\tEDID:");
        lines.add("\t\t00ffffffffffff0010ac14414c305442");
        lines.add("\t\t161c010380351e782eee95a3544c9926");
        lines.add("\t\t0f5054a54b80714f81008180a9c0d1c0");
        lines.add("\t\t010101010101023a801871382d40582c");
        lines.add("\t\t45000f282100001e000000ff00545654");
        lines.add("\t\t37463835554254304c0a000000fc0044");
        lines.add("\t\t454c4c20503234313848540a000000fd");
        lines.add("\t\t00324c1e5311000a202020202020017e");
        lines.add("DP1 disconnected (normal left inverted right x axis y axis)");
        return lines;
    }

    // Fixture: disconnected output with CONNECTOR_ID after a connected output (state-leak trap)
    private static List<String> createXrandrDisconnectedWithConnectorId() {
        List<String> lines = new ArrayList<>();
        lines.add("Screen 0: minimum 8 x 8, current 1920 x 1080, maximum 32767 x 32767");
        lines.add("eDP connected primary 1920x1080+0+0 (normal left inverted right x axis y axis) 340mm x 190mm");
        lines.add("\tEDID:");
        lines.add("\t\t00ffffffffffff001e6d085b0b0b0b0b");
        lines.add("\t\t0c1c0104b53c2278fb2eb5ae4f46a527");
        lines.add("\t\t0d5054254b80714f81809500a9c0b300");
        lines.add("\t\td1c001010101565e00a0a0a029503020");
        lines.add("\t\t35000f282100001a000000fd00384b1e");
        lines.add("\t\t5a1900000a202020202020000000fc00");
        lines.add("\t\t4c4720554c545241474541520000ff00");
        lines.add("\t\t3630344e54505a4832313337370a0100");
        lines.add("\tCONNECTOR_ID: 51");
        lines.add("\t\tsupported: 51");
        lines.add("DP-1 disconnected (normal left inverted right x axis y axis)");
        lines.add("\tCONNECTOR_ID: 70");
        lines.add("\t\tsupported: 70");
        lines.add("HDMI-A-1 disconnected (normal left inverted right x axis y axis)");
        lines.add("\tCONNECTOR_ID: 80");
        lines.add("\t\tsupported: 80");
        return lines;
    }

    @Test
    void testGetDisplayDataSingleWithConnectorId() {
        Map<String, Pair<Integer, byte[]>> data = Xrandr.getDisplayData(createXrandrWithConnectorId());
        assertThat(data.size(), is(1));
        assertThat(data.containsKey("DP2"), is(true));
        Pair<Integer, byte[]> pair = data.get("DP2");
        assertNotNull(pair);
        assertThat(pair.getA(), is(96));
        assertThat(pair.getB().length, is(128));
        assertThat(pair.getB()[0], is((byte) 0x00));
        assertThat(pair.getB()[1], is((byte) 0xFF));
    }

    @Test
    void testGetDisplayDataWithoutConnectorId() {
        Map<String, Pair<Integer, byte[]>> data = Xrandr.getDisplayData(createXrandrWithEdid());
        assertThat(data.size(), is(1));
        assertThat(data.containsKey("HDMI-1"), is(true));
        Pair<Integer, byte[]> pair = data.get("HDMI-1");
        assertNotNull(pair);
        assertThat(pair.getA(), is(-1));
        assertThat(pair.getB().length, is(128));
    }

    @Test
    void testGetDisplayDataTwoDisplays() {
        Map<String, Pair<Integer, byte[]>> data = Xrandr.getDisplayData(createXrandrTwoDisplays());
        assertThat(data.size(), is(2));
        assertThat(data.containsKey("DP2"), is(true));
        assertThat(data.containsKey("HDMI1"), is(true));
        Pair<Integer, byte[]> dp2 = data.get("DP2");
        Pair<Integer, byte[]> hdmi1 = data.get("HDMI1");
        assertNotNull(dp2);
        assertNotNull(hdmi1);
        // DP2 has CONNECTOR_ID, HDMI1 does not
        assertThat(dp2.getA(), is(96));
        assertThat(hdmi1.getA(), is(-1));
        // Both have valid EDIDs
        assertThat(dp2.getB().length, is(128));
        assertThat(hdmi1.getB().length, is(128));
    }

    @Test
    void testGetDisplayDataEmpty() {
        assertThat(Xrandr.getDisplayData(Collections.emptyList()).isEmpty(), is(true));
    }

    @Test
    void testGetDisplayDataDisconnectedSkipped() {
        Map<String, Pair<Integer, byte[]>> data = Xrandr.getDisplayData(createXrandrTwoDisplays());
        // DP1 is disconnected and has no EDID, should not appear
        assertThat(data.containsKey("DP1"), is(false));
    }

    @Test
    void testGetDisplayDataDisconnectedDoesNotLeakState() {
        Map<String, Pair<Integer, byte[]>> data = Xrandr.getDisplayData(createXrandrDisconnectedWithConnectorId());
        assertThat(data.size(), is(1));
        assertThat(data.containsKey("eDP"), is(true));
        Pair<Integer, byte[]> edp = data.get("eDP");
        assertNotNull(edp);
        // eDP's own CONNECTOR_ID is 51, not 70 or 80 from the disconnected outputs
        assertThat(edp.getA(), is(51));
        // Disconnected outputs should not appear
        assertThat(data.containsKey("DP-1"), is(false));
        assertThat(data.containsKey("HDMI-A-1"), is(false));
    }

    @Test
    void testGetEdidArraysDelegatesToGetDisplayData() {
        List<byte[]> edids = Xrandr.getEdidArrays(createXrandrWithConnectorId());
        assertThat(edids, hasSize(1));
        assertThat(edids.get(0).length, is(128));
    }

    // Fixture: xrandr --verbose output using a legacy EDID property name
    private static List<String> createXrandrWithEdidProperty(String property) {
        List<String> lines = new ArrayList<>(createXrandrWithEdid());
        lines.set(2, "\t" + property);
        return lines;
    }

    @Test
    void testGetDisplayDataLegacyEdidDataProperty() {
        // X.Org Server through 1.6 published the driver-side atom EDID_DATA
        Map<String, Pair<Integer, byte[]>> data = Xrandr.getDisplayData(createXrandrWithEdidProperty("EDID_DATA:"));
        assertThat(data.size(), is(1));
        Pair<Integer, byte[]> pair = data.get("HDMI-1");
        assertNotNull(pair);
        assertThat(pair.getB().length, is(128));
    }

    @Test
    void testGetDisplayDataLegacyRandrEdidProperty() {
        // randrproto before 1.3 named the conventional property RANDR_EDID
        Map<String, Pair<Integer, byte[]>> data = Xrandr.getDisplayData(createXrandrWithEdidProperty("RANDR_EDID:"));
        assertThat(data.size(), is(1));
        Pair<Integer, byte[]> pair = data.get("HDMI-1");
        assertNotNull(pair);
        assertThat(pair.getB().length, is(128));
    }

    @Test
    void testGetDisplayDataIgnoresOtherEdidNamedProperties() {
        // A property whose name merely contains EDID must not start an EDID block
        Map<String, Pair<Integer, byte[]>> data = Xrandr.getDisplayData(createXrandrWithEdidProperty("EDID_HASH:"));
        assertThat(data.isEmpty(), is(true));
    }

    @Test
    void testFindOutputNameByConnectorId() {
        Map<String, Pair<Integer, byte[]>> data = Xrandr.getDisplayData(createXrandrTwoDisplays());
        // A connector ID match wins even though the EDID passed here belongs to no display
        assertThat(Xrandr.findOutputName(data, 96, new byte[0]), is(Optional.of("DP2")));
    }

    @Test
    void testFindOutputNameByEdid() {
        Map<String, Pair<Integer, byte[]>> data = Xrandr.getDisplayData(createXrandrTwoDisplays());
        Pair<Integer, byte[]> hdmi1 = data.get("HDMI1");
        assertNotNull(hdmi1);
        // HDMI1 has no connector ID in xrandr, so only the EDID can identify it
        assertThat(Xrandr.findOutputName(data, -1, hdmi1.getB()), is(Optional.of("HDMI1")));
    }

    @Test
    void testFindOutputNameFallsBackToEdidWhenConnectorIdIsUnmatched() {
        Map<String, Pair<Integer, byte[]>> data = Xrandr.getDisplayData(createXrandrTwoDisplays());
        Pair<Integer, byte[]> dp2 = data.get("DP2");
        assertNotNull(dp2);
        assertThat(Xrandr.findOutputName(data, 1234, dp2.getB()), is(Optional.of("DP2")));
    }

    @Test
    void testFindOutputNameNoMatch() {
        Map<String, Pair<Integer, byte[]>> data = Xrandr.getDisplayData(createXrandrTwoDisplays());
        byte[] unknownEdid = new byte[128];
        Arrays.fill(unknownEdid, (byte) 0x5A);
        assertThat(Xrandr.findOutputName(data, 1234, unknownEdid).isPresent(), is(false));
    }

    @Test
    void testFindOutputNameEmptyData() {
        assertThat(Xrandr.findOutputName(Collections.emptyMap(), 96, new byte[128]).isPresent(), is(false));
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
