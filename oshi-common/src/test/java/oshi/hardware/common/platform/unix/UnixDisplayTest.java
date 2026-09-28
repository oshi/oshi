/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import oshi.hardware.Display;
import oshi.hardware.DisplayMode;
import oshi.hardware.DisplayModeImpl;
import oshi.util.driver.unix.Xrandr.Output;
import oshi.util.tuples.Triplet;

/**
 * Tests for {@link UnixDisplay}.
 */
class UnixDisplayTest {

    @Test
    void testDevicePortDefaultsToUnknown() {
        assertThat(new UnixDisplay(new byte[128]).getDevicePort(), is("unknown"));
    }

    @Test
    void testDevicePortFromConnectorConstructor() {
        assertThat(new UnixDisplay(new byte[128], "HDMI-A-1", 96).getDevicePort(), is("HDMI-A-1"));
    }

    @Test
    void testBatchSharesOneXrandrQuery() {
        AtomicInteger queries = new AtomicInteger();
        Supplier<List<Output>> countingQuery = () -> {
            queries.incrementAndGet();
            return xrandrData();
        };

        List<Display> displays = UnixDisplay.getDisplays(drmData(), countingQuery);
        assertThat(displays.size(), is(2));

        // Every display in the batch resolves its output name, twice over, from a single query
        for (int pass = 0; pass < 2; pass++) {
            assertThat(displays.get(0).getOutputName(), is(Optional.of("DP-2")));
            assertThat(displays.get(1).getOutputName(), is(Optional.of("HDMI-1")));
        }
        assertThat(queries.get(), is(1));
    }

    @Test
    void testBatchQueryIsNotRunUntilXrandrDataIsRequested() {
        AtomicInteger queries = new AtomicInteger();
        List<Display> displays = UnixDisplay.getDisplays(drmData(), () -> {
            queries.incrementAndGet();
            return xrandrData();
        });
        // The DRM connector name answers isBuiltIn() without xrandr
        assertThat(displays.get(0).isBuiltIn(), is(Optional.of(Boolean.FALSE)));
        assertThat(queries.get(), is(0));
        displays.get(0).getCurrentMode();
        assertThat(queries.get(), is(1));
    }

    @Test
    void testCurrentModeFromMatchedOutput() {
        List<Display> displays = UnixDisplay.getDisplays(drmData(), UnixDisplayTest::xrandrData);
        assertThat(displays.get(0).getCurrentMode(), is(Optional.of(DP_2_MODE)));
        // HDMI-1 is matched to an output that is connected but not enabled
        assertThat(displays.get(1).getOutputName(), is(Optional.of("HDMI-1")));
        assertThat(displays.get(1).getCurrentMode().isPresent(), is(false));
    }

    @Test
    void testCurrentModeEmptyWithoutXrandr() {
        List<Display> displays = UnixDisplay.getDisplays(drmData(), Collections::emptyList);
        assertThat(displays.get(0).getCurrentMode().isPresent(), is(false));
        assertThat(displays.get(0).getOutputName().isPresent(), is(false));
    }

    @Test
    void testIsBuiltInConnector() {
        for (String name : new String[] { "eDP-1", "eDP1", "LVDS-1", "LVDS1", "DSI-1" }) {
            assertThat(name, UnixDisplay.isBuiltInConnector(name), is(Optional.of(Boolean.TRUE)));
        }
        for (String name : new String[] { "HDMI-A-1", "HDMI1", "DP-2", "DP2", "DisplayPort-0", "DVI-D-1", "VGA-1",
                "Composite-1" }) {
            assertThat(name, UnixDisplay.isBuiltInConnector(name), is(Optional.of(Boolean.FALSE)));
        }
        for (String name : new String[] { "DPI-1", "Virtual-1", "Writeback-1", "screen", "default", "unknown" }) {
            assertThat(name, UnixDisplay.isBuiltInConnector(name).isPresent(), is(false));
        }
    }

    @Test
    void testPrimaryStatusFromXrandr() {
        List<Display> displays = UnixDisplay.getDisplays(drmData(), () -> xrandrData());
        assertThat(displays.size(), is(2));
        // DP-2 is marked primary in xrandr data
        assertThat(displays.get(0).isPrimary(), is(Optional.of(Boolean.TRUE)));
        // HDMI-1 is not marked primary
        assertThat(displays.get(1).isPrimary(), is(Optional.of(Boolean.FALSE)));
    }

    @Test
    void testPrimaryStatusEmptyWithoutXrandr() {
        // When xrandr data is empty (e.g., Wayland), primary status cannot be determined
        AtomicInteger queries = new AtomicInteger();
        Supplier<List<Output>> emptyQuery = () -> {
            queries.incrementAndGet();
            return new ArrayList<>();
        };
        List<Display> displays = UnixDisplay.getDisplays(drmData(), emptyQuery);
        assertThat(displays.size(), is(2));
        assertThat(displays.get(0).isPrimary().isPresent(), is(false));
        assertThat(displays.get(1).isPrimary().isPresent(), is(false));
    }

    // Two displays as DRM sysfs reports them: connector name, connector ID, EDID
    private static List<Triplet<String, Integer, byte[]>> drmData() {
        List<Triplet<String, Integer, byte[]>> drmData = new ArrayList<>();
        drmData.add(new Triplet<>("DP-2", 96, edid((byte) 0x01)));
        drmData.add(new Triplet<>("HDMI-1", 80, edid((byte) 0x02)));
        return drmData;
    }

    private static final DisplayMode DP_2_MODE = new DisplayModeImpl(0, 0, 2560, 1440, 2560, 1440, 59.95, 0);

    // The same two displays as xrandr names them, matched to the DRM data by connector ID
    private static List<Output> xrandrData() {
        List<Output> data = new ArrayList<>();
        data.add(new Output("DP-2", 96, edid((byte) 0x01), DP_2_MODE, true));
        data.add(new Output("HDMI-1", 80, edid((byte) 0x02), null, false));
        return data;
    }

    private static byte[] edid(byte marker) {
        byte[] edid = new byte[128];
        Arrays.fill(edid, marker);
        return edid;
    }
}
