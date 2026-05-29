/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.unix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.is;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class XwininfoTest {

    // Fixture: xprop -root _NET_CLIENT_LIST_STACKING output
    private static final List<String> STACKING_OUTPUT = Collections
            .singletonList("_NET_CLIENT_LIST_STACKING(WINDOW): window id # 0x1400003, 0x1600003, 0x1800003");

    // Fixture: xwininfo -root -tree output with window entries
    private static final List<String> WINDOW_TREE = Arrays.asList(
            "xwininfo: Window id: 0x1e7 (the root window) (has no name)",
            "  Root window id: 0x1e7 (the root window) (has no name)",
            "     0x1400003 \"Terminal\": (\"gnome-terminal\" \"Gnome-terminal\")  800x600+0+0  +100+200",
            "     0x1600003 \"Firefox\": (\"navigator\" \"Firefox\")  1024x768+0+0  +50+75",
            "     0x1800003 (has no name): ()  640x480+0+0  +-10+30");

    @Test
    void testParseZOrderValid() {
        Map<String, Integer> zOrder = Xwininfo.parseZOrder(STACKING_OUTPUT);
        assertThat(zOrder, aMapWithSize(3));
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
    void testParseZOrderNoHexId() {
        List<String> noHex = Collections.singletonList("_NET_CLIENT_LIST_STACKING(WINDOW): window id # ");
        Map<String, Integer> zOrder = Xwininfo.parseZOrder(noHex);
        assertThat(zOrder, is(anEmptyMap()));
    }

    @Test
    void testParseWindowTree() {
        Map<String, Integer> zOrderMap = new HashMap<>();
        zOrderMap.put("0x1400003", 1);
        zOrderMap.put("0x1600003", 2);
        zOrderMap.put("0x1800003", 3);

        Map<String, String> windowNameMap = new HashMap<>();
        Map<String, String> windowPathMap = new HashMap<>();
        Map<String, Rectangle> windowMap = Xwininfo.parseWindowTree(WINDOW_TREE, false, zOrderMap, windowNameMap,
                windowPathMap);

        assertThat(windowMap, aMapWithSize(3));

        // Verify rectangle bounds (x, y, width, height)
        Rectangle terminal = windowMap.get("0x1400003");
        assertThat(terminal.x, is(100));
        assertThat(terminal.y, is(200));
        assertThat(terminal.width, is(800));
        assertThat(terminal.height, is(600));

        Rectangle firefox = windowMap.get("0x1600003");
        assertThat(firefox.x, is(50));
        assertThat(firefox.y, is(75));
        assertThat(firefox.width, is(1024));
        assertThat(firefox.height, is(768));

        // Negative coordinates
        Rectangle anonymous = windowMap.get("0x1800003");
        assertThat(anonymous.x, is(-10));
        assertThat(anonymous.y, is(30));
        assertThat(anonymous.width, is(640));
        assertThat(anonymous.height, is(480));

        // Verify window names
        assertThat(windowNameMap.get("0x1400003"), is("Terminal"));
        assertThat(windowNameMap.get("0x1600003"), is("Firefox"));
        assertThat(windowNameMap.containsKey("0x1800003"), is(false));

        // Verify window paths
        assertThat(windowPathMap.get("0x1400003"), is("gnome-terminal"));
        assertThat(windowPathMap.get("0x1600003"), is("navigator"));
        assertThat(windowPathMap.containsKey("0x1800003"), is(false));
    }

    @Test
    void testParseWindowTreeVisibleOnly() {
        // Only 0x1400003 is in the z-order map
        Map<String, Integer> zOrderMap = new HashMap<>();
        zOrderMap.put("0x1400003", 1);

        Map<String, String> windowNameMap = new HashMap<>();
        Map<String, String> windowPathMap = new HashMap<>();
        Map<String, Rectangle> windowMap = Xwininfo.parseWindowTree(WINDOW_TREE, true, zOrderMap, windowNameMap,
                windowPathMap);

        // Only the window present in zOrderMap should be included
        assertThat(windowMap, aMapWithSize(1));
        assertThat(windowMap.containsKey("0x1400003"), is(true));
        assertThat(windowMap.containsKey("0x1600003"), is(false));
        assertThat(windowMap.containsKey("0x1800003"), is(false));
    }

    @Test
    void testParseWindowTreeAnonymousWindow() {
        // A window line with no quoted name
        List<String> tree = Collections.singletonList("     0x2000001 (has no name): ()  320x240+0+0  +0+0");
        Map<String, Integer> zOrderMap = new HashMap<>();
        zOrderMap.put("0x2000001", 1);

        Map<String, String> windowNameMap = new HashMap<>();
        Map<String, String> windowPathMap = new HashMap<>();
        Map<String, Rectangle> windowMap = Xwininfo.parseWindowTree(tree, false, zOrderMap, windowNameMap,
                windowPathMap);

        assertThat(windowMap, aMapWithSize(1));
        Rectangle rect = windowMap.get("0x2000001");
        assertThat(rect.x, is(0));
        assertThat(rect.y, is(0));
        assertThat(rect.width, is(320));
        assertThat(rect.height, is(240));

        // No name or path should be recorded for anonymous windows
        assertThat(windowNameMap.containsKey("0x2000001"), is(false));
        assertThat(windowPathMap.containsKey("0x2000001"), is(false));
    }
}
