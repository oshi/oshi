/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import oshi.hardware.Display;
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
        Supplier<Map<String, Triplet<Integer, byte[], Boolean>>> countingQuery = () -> {
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
    void testBatchQueryIsRunOnceForPrimaryStatus() {
        AtomicInteger queries = new AtomicInteger();
        UnixDisplay.getDisplays(drmData(), () -> {
            queries.incrementAndGet();
            return xrandrData();
        });
        // Primary status is computed eagerly during display construction
        assertThat(queries.get(), is(1));
    }

    @Test
    void testPrimaryStatusFromXrandr() {
        List<Display> displays = UnixDisplay.getDisplays(drmData(), () -> xrandrData());
        assertThat(displays.size(), is(2));
        // DP-2 is marked primary in xrandr data
        assertThat(displays.get(0).isPrimary(), is(true));
        // HDMI-1 is not marked primary
        assertThat(displays.get(1).isPrimary(), is(false));
    }

    @Test
    void testPrimaryStatusDefaultsToFalse() {
        // When xrandr data is empty (e.g., Wayland), all displays are non-primary
        AtomicInteger queries = new AtomicInteger();
        Supplier<Map<String, Triplet<Integer, byte[], Boolean>>> emptyQuery = () -> {
            queries.incrementAndGet();
            return new LinkedHashMap<>();
        };
        List<Display> displays = UnixDisplay.getDisplays(drmData(), emptyQuery);
        assertThat(displays.size(), is(2));
        assertThat(displays.get(0).isPrimary(), is(false));
        assertThat(displays.get(1).isPrimary(), is(false));
    }

    // Two displays as DRM sysfs reports them: connector name, connector ID, EDID
    private static List<Triplet<String, Integer, byte[]>> drmData() {
        List<Triplet<String, Integer, byte[]>> drmData = new ArrayList<>();
        drmData.add(new Triplet<>("DP-2", 96, edid((byte) 0x01)));
        drmData.add(new Triplet<>("HDMI-1", 80, edid((byte) 0x02)));
        return drmData;
    }

    // The same two displays as xrandr names them, matched to the DRM data by connector ID
    private static Map<String, Triplet<Integer, byte[], Boolean>> xrandrData() {
        Map<String, Triplet<Integer, byte[], Boolean>> data = new LinkedHashMap<>();
        data.put("DP-2", new Triplet<>(96, edid((byte) 0x01), true));
        data.put("HDMI-1", new Triplet<>(80, edid((byte) 0x02), false));
        return data;
    }

    private static byte[] edid(byte marker) {
        byte[] edid = new byte[128];
        Arrays.fill(edid, marker);
        return edid;
    }
}
