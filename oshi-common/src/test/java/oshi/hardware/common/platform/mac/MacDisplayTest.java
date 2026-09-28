/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import oshi.driver.common.mac.CoreGraphicsDisplay;
import oshi.hardware.DisplayInfo;
import oshi.hardware.DisplayInfoImpl;
import oshi.hardware.DisplayMode;
import oshi.util.EdidUtil;

class MacDisplayTest {

    private static final DisplayMode BUILT_IN_MODE = CoreGraphicsDisplay.toMode(0, 0, 1728, 1117, 3456, 2234, 120, 0);
    private static final DisplayMode EXTERNAL_MODE = CoreGraphicsDisplay.toMode(1728, 0, 2560, 1440, 2560, 1440, 60, 0);

    private final AtomicInteger queries = new AtomicInteger();
    private final Supplier<List<CoreGraphicsDisplay>> cgDisplays = this::queryCoreGraphicsDisplays;

    // A built-in panel, which is the main display, and an external monitor, counting how often they are queried
    private List<CoreGraphicsDisplay> queryCoreGraphicsDisplays() {
        queries.incrementAndGet();
        return List.of(new CoreGraphicsDisplay(0x610, 0xa050, 0, true, true, BUILT_IN_MODE),
                new CoreGraphicsDisplay(0x1e6d, 0x5b08, 0x0b0b0b0b, false, false, EXTERNAL_MODE));
    }

    private static final class TestDisplay extends MacDisplay {
        TestDisplay(byte[] edid, @Nullable Boolean builtIn, Supplier<List<CoreGraphicsDisplay>> cgDisplays) {
            super(edid, "Port-HDMI@1", builtIn, cgDisplays);
        }

        TestDisplay(DisplayInfo info, Supplier<List<CoreGraphicsDisplay>> cgDisplays) {
            super(info, "disp0", cgDisplays);
        }
    }

    // An EDID carrying the identity CoreGraphics reports for the external display above
    private static byte[] externalEdid() {
        byte[] edid = EdidUtil.newEdidTemplate();
        edid[8] = 0x1e;
        edid[9] = 0x6d;
        edid[10] = 0x08;
        edid[11] = 0x5b;
        edid[12] = edid[13] = edid[14] = edid[15] = 0x0b;
        return edid;
    }

    @Test
    void testExternalDisplayMatchedByEdid() {
        TestDisplay display = new TestDisplay(externalEdid(), null, cgDisplays);
        assertThat(queries.get(), is(0));
        assertThat(display.getCurrentMode(), is(Optional.of(EXTERNAL_MODE)));
        assertThat(display.isBuiltIn(), is(Optional.of(Boolean.FALSE)));
        assertThat(display.getDevicePort(), is("Port-HDMI@1"));
        assertThat(display.isPrimary(), is(Optional.of(Boolean.FALSE)));
    }

    @Test
    void testExternalMainDisplayIsPrimary() {
        Supplier<List<CoreGraphicsDisplay>> externalMain = () -> List.of(
                new CoreGraphicsDisplay(0x610, 0xa050, 0, true, false, BUILT_IN_MODE),
                new CoreGraphicsDisplay(0x1e6d, 0x5b08, 0x0b0b0b0b, false, true, EXTERNAL_MODE));
        assertThat(new TestDisplay(externalEdid(), null, externalMain).isPrimary(), is(Optional.of(Boolean.TRUE)));
    }

    @Test
    void testKnownBuiltInStatusNeedsNoQuery() {
        TestDisplay display = new TestDisplay(externalEdid(), Boolean.FALSE, cgDisplays);
        assertThat(display.isBuiltIn(), is(Optional.of(Boolean.FALSE)));
        assertThat(queries.get(), is(0));
    }

    @Test
    void testSyntheticBuiltInPanelMatchedByBuiltInFlag() {
        DisplayInfo info = new DisplayInfoImpl("APP", "A050", null, (byte) 0, 2023, "1.4", true, 35, 22, "3456x2234",
                "Built-in Retina Display", "");
        TestDisplay display = new TestDisplay(info, cgDisplays);
        assertThat(display.getCurrentMode(), is(Optional.of(BUILT_IN_MODE)));
        assertThat(display.isBuiltIn(), is(Optional.of(Boolean.TRUE)));
        assertThat(display.isPrimary(), is(Optional.of(Boolean.TRUE)));
    }

    @Test
    void testUnmatchedDisplayHasNoModeOrBuiltInStatus() {
        TestDisplay display = new TestDisplay(EdidUtil.newEdidTemplate(), null, cgDisplays);
        assertThat(display.getCurrentMode().isPresent(), is(false));
        assertThat(display.isBuiltIn().isPresent(), is(false));
        assertThat(display.isPrimary().isPresent(), is(false));
    }
}
