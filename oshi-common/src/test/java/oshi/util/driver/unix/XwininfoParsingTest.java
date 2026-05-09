/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.unix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class XwininfoParsingTest {

    // Fixture: xprop -root _NET_CLIENT_LIST_STACKING output
    private static final List<String> STACKING_OUTPUT = Arrays
            .asList("_NET_CLIENT_LIST_STACKING(WINDOW): window id # 0x1400003, 0x1600003, 0x1800003");

    // Fixture: xwininfo -root -tree output (trimmed)
    private static final List<String> XWININFO_TREE = Arrays.asList(
            "     0x1400003 \"Terminal\": (\"gnome-terminal\" \"Gnome-terminal\")  800x600+0+0  +100+200",
            "     0x1600003 \"Firefox\": (\"Navigator\" \"firefox\")  1920x1080+0+0  +0+0",
            "     0x1800003 (has no name): ()  200x200+0+0  +50+-10",
            "     0x1a00003 \"Hidden Window\": (\"hidden\" \"Hidden\")  400x300+0+0  +10+20");

    @Test
    void testParseZOrder() {
        Map<String, Integer> zOrder = Xwininfo.parseZOrder(STACKING_OUTPUT);
        assertThat(zOrder.size(), is(3));
        assertThat(zOrder.get("0x1400003"), is(1));
        assertThat(zOrder.get("0x1600003"), is(2));
        assertThat(zOrder.get("0x1800003"), is(3));
    }

    @Test
    void testParseZOrderEmpty() {
        Map<String, Integer> zOrder = Xwininfo.parseZOrder(Collections.emptyList());
        assertThat(zOrder, is(anEmptyMap()));
    }

    @Test
    void testParseZOrderNoHexIds() {
        List<String> noIds = Arrays.asList("_NET_CLIENT_LIST_STACKING: no such atom on any window.");
        Map<String, Integer> zOrder = Xwininfo.parseZOrder(noIds);
        assertThat(zOrder, is(anEmptyMap()));
    }

    @Test
    void testParseWindowTreeAllWindows() {
        Map<String, Integer> zOrder = Xwininfo.parseZOrder(STACKING_OUTPUT);
        Map<String, String> nameMap = new HashMap<>();
        Map<String, String> pathMap = new HashMap<>();

        Map<String, Rectangle> windows = Xwininfo.parseWindowTree(XWININFO_TREE, false, zOrder, nameMap, pathMap);

        // All 4 windows should be included (visibleOnly=false)
        assertThat(windows.size(), is(4));
        assertThat(windows, hasKey("0x1400003"));
        assertThat(windows, hasKey("0x1a00003"));

        // Check names
        assertThat(nameMap.get("0x1400003"), is("Terminal"));
        assertThat(nameMap.get("0x1600003"), is("Firefox"));
        // 0x1800003 has no name, should not be in nameMap
        assertThat(nameMap.containsKey("0x1800003"), is(false));

        // Check paths
        assertThat(pathMap.get("0x1400003"), is("gnome-terminal"));
        assertThat(pathMap.get("0x1600003"), is("Navigator"));

        // Check rectangle for Terminal: 800x600 at +100+200
        Rectangle termRect = windows.get("0x1400003");
        assertThat(termRect.width, is(800));
        assertThat(termRect.height, is(600));
        assertThat(termRect.x, is(100));
        assertThat(termRect.y, is(200));

        // Check negative coordinate
        Rectangle noNameRect = windows.get("0x1800003");
        assertThat(noNameRect.y, is(-10));
    }

    @Test
    void testParseWindowTreeVisibleOnly() {
        Map<String, Integer> zOrder = Xwininfo.parseZOrder(STACKING_OUTPUT);
        Map<String, String> nameMap = new HashMap<>();
        Map<String, String> pathMap = new HashMap<>();

        Map<String, Rectangle> windows = Xwininfo.parseWindowTree(XWININFO_TREE, true, zOrder, nameMap, pathMap);

        // Only 3 visible windows (in zOrderMap), 0x1a00003 is not visible
        assertThat(windows.size(), is(3));
        assertThat(windows.containsKey("0x1a00003"), is(false));
    }

    @Test
    void testParseWindowTreeEmpty() {
        Map<String, String> nameMap = new HashMap<>();
        Map<String, String> pathMap = new HashMap<>();

        Map<String, Rectangle> windows = Xwininfo.parseWindowTree(Collections.emptyList(), false,
                Collections.emptyMap(), nameMap, pathMap);

        assertThat(windows, is(anEmptyMap()));
        assertThat(nameMap, is(anEmptyMap()));
    }

    @Test
    void testParseWindowTreeNonMatchingLines() {
        List<String> nonMatching = Arrays.asList("xwininfo: Window id: 0x1e3 (the root window) \"i3\"",
                "  Root window id: 0x1e3 (the root window) (has no name)", "  Parent window id: 0x0 (none)",
                "     2 children:");
        Map<String, String> nameMap = new HashMap<>();
        Map<String, String> pathMap = new HashMap<>();

        Map<String, Rectangle> windows = Xwininfo.parseWindowTree(nonMatching, false, Collections.emptyMap(), nameMap,
                pathMap);

        assertThat(windows, is(anEmptyMap()));
    }
}
