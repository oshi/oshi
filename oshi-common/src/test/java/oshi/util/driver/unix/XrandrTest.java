/*
 * Copyright 2022-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.unix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.hardware.DisplayMode;
import oshi.util.driver.unix.Xrandr.Output;

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
    void testGetOutputsSingleWithConnectorId() {
        Map<String, Output> data = byName(Xrandr.getOutputs(createXrandrWithConnectorId()));
        assertThat(data.size(), is(1));
        assertThat(data.containsKey("DP2"), is(true));
        Output pair = data.get("DP2");
        assertNotNull(pair);
        assertThat(pair.getConnectorId(), is(96));
        assertThat(pair.getEdid().length, is(128));
        assertThat(pair.getEdid()[0], is((byte) 0x00));
        assertThat(pair.getEdid()[1], is((byte) 0xFF));
    }

    @Test
    void testGetOutputsWithoutConnectorId() {
        Map<String, Output> data = byName(Xrandr.getOutputs(createXrandrWithEdid()));
        assertThat(data.size(), is(1));
        assertThat(data.containsKey("HDMI-1"), is(true));
        Output pair = data.get("HDMI-1");
        assertNotNull(pair);
        assertThat(pair.getConnectorId(), is(-1));
        assertThat(pair.getEdid().length, is(128));
    }

    @Test
    void testGetOutputsTwoDisplays() {
        Map<String, Output> data = byName(Xrandr.getOutputs(createXrandrTwoDisplays()));
        assertThat(data.size(), is(2));
        assertThat(data.containsKey("DP2"), is(true));
        assertThat(data.containsKey("HDMI1"), is(true));
        Output dp2 = data.get("DP2");
        Output hdmi1 = data.get("HDMI1");
        assertNotNull(dp2);
        assertNotNull(hdmi1);
        // DP2 has CONNECTOR_ID, HDMI1 does not
        assertThat(dp2.getConnectorId(), is(96));
        assertThat(hdmi1.getConnectorId(), is(-1));
        // Both have valid EDIDs
        assertThat(dp2.getEdid().length, is(128));
        assertThat(hdmi1.getEdid().length, is(128));
    }

    @Test
    void testGetOutputsEmpty() {
        assertThat(Xrandr.getOutputs(Collections.emptyList()).isEmpty(), is(true));
    }

    @Test
    void testGetOutputsDisconnectedSkipped() {
        Map<String, Output> data = byName(Xrandr.getOutputs(createXrandrTwoDisplays()));
        // DP1 is disconnected and has no EDID, should not appear
        assertThat(data.containsKey("DP1"), is(false));
    }

    @Test
    void testGetOutputsDisconnectedDoesNotLeakState() {
        Map<String, Output> data = byName(Xrandr.getOutputs(createXrandrDisconnectedWithConnectorId()));
        assertThat(data.size(), is(1));
        assertThat(data.containsKey("eDP"), is(true));
        Output edp = data.get("eDP");
        assertNotNull(edp);
        // eDP's own CONNECTOR_ID is 51, not 70 or 80 from the disconnected outputs
        assertThat(edp.getConnectorId(), is(51));
        // Disconnected outputs should not appear
        assertThat(data.containsKey("DP-1"), is(false));
        assertThat(data.containsKey("HDMI-A-1"), is(false));
    }

    @Test
    void testGetEdidArraysDelegatesToGetOutputs() {
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
    void testGetOutputsLegacyEdidDataProperty() {
        // X.Org Server through 1.6 published the driver-side atom EDID_DATA
        Map<String, Output> data = byName(Xrandr.getOutputs(createXrandrWithEdidProperty("EDID_DATA:")));
        assertThat(data.size(), is(1));
        Output pair = data.get("HDMI-1");
        assertNotNull(pair);
        assertThat(pair.getEdid().length, is(128));
    }

    @Test
    void testGetOutputsLegacyRandrEdidProperty() {
        // randrproto before 1.3 named the conventional property RANDR_EDID
        Map<String, Output> data = byName(Xrandr.getOutputs(createXrandrWithEdidProperty("RANDR_EDID:")));
        assertThat(data.size(), is(1));
        Output pair = data.get("HDMI-1");
        assertNotNull(pair);
        assertThat(pair.getEdid().length, is(128));
    }

    @Test
    void testGetOutputsIgnoresOtherEdidNamedProperties() {
        // A property whose name merely contains EDID must not start an EDID block
        Map<String, Output> data = byName(Xrandr.getOutputs(createXrandrWithEdidProperty("EDID_HASH:")));
        assertThat(data.isEmpty(), is(true));
    }

    @Test
    void testFindOutputNameByConnectorId() {
        Map<String, Output> data = byName(Xrandr.getOutputs(createXrandrTwoDisplays()));
        // A connector ID match wins even though the EDID passed here belongs to no display
        assertThat(findOutputName(data, 96, new byte[0]), is(Optional.of("DP2")));
    }

    @Test
    void testFindOutputNameByEdid() {
        Map<String, Output> data = byName(Xrandr.getOutputs(createXrandrTwoDisplays()));
        Output hdmi1 = data.get("HDMI1");
        assertNotNull(hdmi1);
        // HDMI1 has no connector ID in xrandr, so only the EDID can identify it
        assertThat(findOutputName(data, -1, hdmi1.getEdid()), is(Optional.of("HDMI1")));
    }

    @Test
    void testFindOutputNameFallsBackToEdidWhenConnectorIdIsUnmatched() {
        Map<String, Output> data = byName(Xrandr.getOutputs(createXrandrTwoDisplays()));
        Output dp2 = data.get("DP2");
        assertNotNull(dp2);
        assertThat(findOutputName(data, 1234, dp2.getEdid()), is(Optional.of("DP2")));
    }

    @Test
    void testFindOutputNameNoMatch() {
        Map<String, Output> data = byName(Xrandr.getOutputs(createXrandrTwoDisplays()));
        byte[] unknownEdid = new byte[128];
        Arrays.fill(unknownEdid, (byte) 0x5A);
        assertThat(findOutputName(data, 1234, unknownEdid).isPresent(), is(false));
    }

    @Test
    void testFindOutputNameEmptyData() {
        assertThat(Xrandr.findOutput(Collections.emptyList(), 96, new byte[128]).isPresent(), is(false));
    }

    private static Map<String, Output> byName(List<Output> outputs) {
        Map<String, Output> byName = new LinkedHashMap<>();
        for (Output output : outputs) {
            byName.put(output.getName(), output);
        }
        return byName;
    }

    private static Optional<String> findOutputName(Map<String, Output> data, int connectorId, byte[] edid) {
        return Xrandr.findOutput(new ArrayList<>(data.values()), connectorId, edid).map(Output::getName);
    }

    // The EDID block from createXrandrWithEdid, as the property lines xrandr --verbose prints
    private static final String EDID_BLOCK = """
            \tEDID:
            \t\t00ffffffffffff001e6d085b0b0b0b0b
            \t\t0c1c0104b53c2278fb2eb5ae4f46a527
            \t\t0d5054254b80714f81809500a9c0b300
            \t\td1c001010101565e00a0a0a029503020
            \t\t35000f282100001a000000fd00384b1e
            \t\t5a1900000a202020202020000000fc00
            \t\t4c4720554c545241474541520000ff00
            \t\t3630344e54505a4832313337370a0100
            """;

    // Fixture: xrandr --verbose captured from Xvfb, with EDID_BLOCK added after its properties since Xvfb publishes
    // no EDID. Xvfb drives a virtual mode with no timings, so its refresh rate reads as 0.00Hz.
    private static List<String> createXvfbCapture() {
        return ("""
                Screen 0: minimum 1 x 1, current 1920 x 1080, maximum 1920 x 1080
                screen connected 1920x1080+0+0 (0x3a) normal (normal) 0mm x 0mm
                \tIdentifier: 0x3c
                \tTimestamp:  84938650
                \tSubpixel:   unknown
                \tGamma:      1.0:1.0:1.0
                \tBrightness: 0.0
                \tClones:
                \tCRTC:       0
                \tCRTCs:      0
                \tTransform:  1.000000 0.000000 0.000000
                \t            0.000000 1.000000 0.000000
                \t            0.000000 0.000000 1.000000
                \t           filter:
                \tnon-desktop: 0
                \t\tsupported: 0, 1
                """ + EDID_BLOCK + """
                  1920x1080 (0x3a)  0.000MHz *current
                        h: width  1920 start    0 end    0 total    0 skew    0 clock   0.00KHz
                        v: height 1080 start    0 end    0 total    0           clock   0.00Hz
                """).lines().toList();
    }

    // Fixture: a rotated display to the right of a scaled one, a reflected one, and one connected but disabled. The
    // header geometry is in the output's current orientation and scale; the mode timings are the unrotated mode.
    private static List<String> createXrandrModes() {
        return ("""
                Screen 0: minimum 320 x 200, current 5760 x 2160, maximum 16384 x 16384
                DP-1 connected primary 3840x2160+0+0 (0x48) normal (normal left inverted right x axis y axis) \
                600mm x 340mm
                """ + EDID_BLOCK + """
                  1920x1080 (0x48) 148.500MHz +HSync +VSync *current +preferred
                        h: width  1920 start 2008 end 2052 total 2200 skew    0 clock  67.50KHz
                        v: height 1080 start 1084 end 1089 total 1125           clock  60.00Hz
                  1280x720 (0x4b) 74.250MHz +HSync +VSync
                        h: width  1280 start 1390 end 1430 total 1650 skew    0 clock  45.00KHz
                        v: height  720 start  725 end  730 total  750           clock  59.94Hz
                HDMI-1 connected 1080x1920+3840+0 (0x46) left (normal left inverted right x axis y axis) 527mm x 296mm
                """ + EDID_BLOCK.replace("0b0b0b0b", "0c0c0c0c") + """
                  1920x1080 (0x46) 138.500MHz +HSync -VSync *current +preferred
                        h: width  1920 start 1968 end 2000 total 2080 skew    0 clock  66.59KHz
                        v: height 1080 start 1083 end 1088 total 1111           clock  59.93Hz
                HDMI-2 connected 1920x1080+0+2160 (0x46) inverted Y axis (normal left inverted right x axis y axis) \
                527mm x 296mm
                """ + EDID_BLOCK.replace("0b0b0b0b", "0d0d0d0d") + """
                  1920x1080 (0x46) 148.500MHz +HSync +VSync *current
                        h: width  1920 start 2008 end 2052 total 2200 skew    0 clock  67.50KHz
                        v: height 1080 start 1084 end 1089 total 1125           clock  60.00Hz
                DP-2 connected (normal left inverted right x axis y axis)
                """ + EDID_BLOCK.replace("0b0b0b0b", "0e0e0e0e") + """
                  2560x1440 (0x50) 241.500MHz +HSync -VSync +preferred
                        h: width  2560 start 2608 end 2640 total 2720 skew    0 clock  88.79KHz
                        v: height 1440 start 1443 end 1448 total 1481           clock  59.95Hz
                """).lines().toList();
    }

    private static DisplayMode modeOf(Map<String, Output> data, String name) {
        Output output = data.get(name);
        assertNotNull(output);
        Optional<DisplayMode> mode = output.getMode();
        assertThat(name + " should have a mode", mode.isPresent(), is(true));
        return mode.get();
    }

    @Test
    void testGetOutputsModeFromXvfbCapture() {
        Map<String, Output> data = byName(Xrandr.getOutputs(createXvfbCapture()));
        assertThat(data.keySet(), contains("screen"));
        DisplayMode mode = modeOf(data, "screen");
        assertThat(mode.getWidth(), is(1920));
        assertThat(mode.getHeight(), is(1080));
        assertThat(mode.getPixelWidth(), is(1920));
        assertThat(mode.getPixelHeight(), is(1080));
        assertThat(mode.getRefreshRate(), is(0d));
        assertThat(mode.getRotation(), is(0));
        assertThat(mode.getX(), is(0));
        assertThat(mode.getY(), is(0));
    }

    @Test
    void testGetOutputsModeScaled() {
        // Scaled 2x: the output covers 3840x2160 of the screen but drives a 1920x1080 mode; the current mode's
        // timings are used, not the 1280x720 mode listed after it
        DisplayMode mode = modeOf(byName(Xrandr.getOutputs(createXrandrModes())), "DP-1");
        assertThat(mode.getWidth(), is(3840));
        assertThat(mode.getHeight(), is(2160));
        assertThat(mode.getPixelWidth(), is(1920));
        assertThat(mode.getPixelHeight(), is(1080));
        assertThat(mode.getRefreshRate(), is(60d));
        assertThat(mode.getRotation(), is(0));
    }

    @Test
    void testGetOutputsModeRotatedLeft() {
        // "left" turns the contents a quarter turn counterclockwise, which is 270 degrees clockwise; the pixel size is
        // turned to match the header geometry
        DisplayMode mode = modeOf(byName(Xrandr.getOutputs(createXrandrModes())), "HDMI-1");
        assertThat(mode.getWidth(), is(1080));
        assertThat(mode.getHeight(), is(1920));
        assertThat(mode.getPixelWidth(), is(1080));
        assertThat(mode.getPixelHeight(), is(1920));
        assertThat(mode.getRefreshRate(), is(59.93));
        assertThat(mode.getRotation(), is(270));
        assertThat(mode.getX(), is(3840));
        assertThat(mode.getY(), is(0));
    }

    @Test
    void testGetOutputsModeInvertedAndReflected() {
        // A reflection follows the rotation on the header line and does not change it
        DisplayMode mode = modeOf(byName(Xrandr.getOutputs(createXrandrModes())), "HDMI-2");
        assertThat(mode.getRotation(), is(180));
        assertThat(mode.getPixelWidth(), is(1920));
        assertThat(mode.getX(), is(0));
        assertThat(mode.getY(), is(2160));
    }

    @Test
    void testGetOutputsConnectedButDisabledHasNoMode() {
        Map<String, Output> data = byName(Xrandr.getOutputs(createXrandrModes()));
        assertThat(data.keySet(), contains("DP-1", "HDMI-1", "HDMI-2", "DP-2"));
        Output dp2 = data.get("DP-2");
        assertNotNull(dp2);
        assertThat(dp2.getMode().isPresent(), is(false));
    }

    @Test
    void testGetOutputsHeaderWithoutModeBlock() {
        // An output whose mode list is missing still reports its geometry, with the pixel size taken from it
        DisplayMode mode = modeOf(byName(Xrandr.getOutputs(createXrandrTwoDisplays())), "HDMI1");
        assertThat(mode.getWidth(), is(1920));
        assertThat(mode.getPixelWidth(), is(1920));
        assertThat(mode.getRefreshRate(), is(0d));
        assertThat(mode.getX(), is(1920));
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
