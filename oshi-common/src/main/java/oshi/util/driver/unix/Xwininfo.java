/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.unix;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.os.OSDesktopWindow;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.Util;

/**
 * Utility to query X11 windows
 */
@ThreadSafe
public final class Xwininfo {

    private static final String[] NET_CLIENT_LIST_STACKING = ParseUtil.whitespaces
            .split("xprop -root _NET_CLIENT_LIST_STACKING");
    private static final String[] XWININFO_ROOT_TREE = ParseUtil.whitespaces.split("xwininfo -root -tree");
    private static final String[] XPROP_NET_WM_PID_ID = ParseUtil.whitespaces.split("xprop _NET_WM_PID -id");

    private static final Pattern WINDOW_PATTERN = Pattern.compile(
            "(0x\\S+) (?:\"(.+)\")?.*: \\((?:\"(.+)\" \".+\")?\\) {2}(\\d+)x(\\d+)\\+.+ {2}\\+(-?\\d+)\\+(-?\\d+)");

    private Xwininfo() {
    }

    /**
     * Gets windows on the operating system's GUI desktop.
     *
     * @param visibleOnly Whether to restrict the list to only windows visible to the user.
     * @return A list of {@link oshi.software.os.OSDesktopWindow} objects representing the desktop windows.
     */
    public static List<OSDesktopWindow> queryXWindows(boolean visibleOnly) {
        // X commands don't work with LC_ALL
        List<String> stacking = ExecutingCommand.runNative(NET_CLIENT_LIST_STACKING, null);
        List<String> tree = ExecutingCommand.runNative(XWININFO_ROOT_TREE, null);

        Map<String, Integer> zOrderMap = parseZOrder(stacking);
        Map<String, String> windowNameMap = new HashMap<>();
        Map<String, String> windowPathMap = new HashMap<>();
        Map<String, Rectangle> windowMap = parseWindowTree(tree, visibleOnly, zOrderMap, windowNameMap, windowPathMap);

        // Get PID for each window and build result list
        List<OSDesktopWindow> windowList = new ArrayList<>();
        for (Entry<String, Rectangle> e : windowMap.entrySet()) {
            String id = e.getKey();
            long pid = queryPidFromId(id);
            boolean visible = zOrderMap.containsKey(id);
            windowList.add(new OSDesktopWindow(ParseUtil.hexStringToLong(id, 0L), windowNameMap.getOrDefault(id, ""),
                    windowPathMap.getOrDefault(id, ""), e.getValue(), pid, zOrderMap.getOrDefault(id, 0), visible));
        }
        return windowList;
    }

    /**
     * Parses the z-order stacking from xprop _NET_CLIENT_LIST_STACKING output.
     *
     * @param stacking the output lines from xprop
     * @return a map of window ID to z-order (1 = bottom)
     */
    static Map<String, Integer> parseZOrder(List<String> stacking) {
        Map<String, Integer> zOrderMap = new HashMap<>();
        int z = 0;
        if (!stacking.isEmpty()) {
            String stack = stacking.get(0);
            int bottom = stack.indexOf("0x");
            if (bottom >= 0) {
                for (String id : stack.substring(bottom).split(", ")) {
                    zOrderMap.put(id, ++z);
                }
            }
        }
        return zOrderMap;
    }

    /**
     * Parses the window tree from xwininfo -root -tree output.
     *
     * @param tree          the output lines from xwininfo
     * @param visibleOnly   whether to restrict to visible windows
     * @param zOrderMap     the z-order map for visibility filtering
     * @param windowNameMap populated with window names (output parameter)
     * @param windowPathMap populated with window paths (output parameter)
     * @return a map of window ID to rectangle bounds, in insertion order
     */
    static Map<String, Rectangle> parseWindowTree(List<String> tree, boolean visibleOnly,
            Map<String, Integer> zOrderMap, Map<String, String> windowNameMap, Map<String, String> windowPathMap) {
        Map<String, Rectangle> windowMap = new LinkedHashMap<>();
        for (String line : tree) {
            Matcher m = WINDOW_PATTERN.matcher(line.trim());
            if (m.matches()) {
                String id = m.group(1);
                if (!visibleOnly || zOrderMap.containsKey(id)) {
                    String windowName = m.group(2);
                    if (!Util.isBlank(windowName)) {
                        windowNameMap.put(id, windowName);
                    }
                    String windowPath = m.group(3);
                    if (!Util.isBlank(windowPath)) {
                        windowPathMap.put(id, windowPath);
                    }
                    windowMap.put(id, new Rectangle(ParseUtil.parseIntOrDefault(m.group(6), 0),
                            ParseUtil.parseIntOrDefault(m.group(7), 0), ParseUtil.parseIntOrDefault(m.group(4), 0),
                            ParseUtil.parseIntOrDefault(m.group(5), 0)));
                }
            }
        }
        return windowMap;
    }

    private static long queryPidFromId(String id) {
        // X commands don't work with LC_ALL
        String[] cmd = new String[XPROP_NET_WM_PID_ID.length + 1];
        System.arraycopy(XPROP_NET_WM_PID_ID, 0, cmd, 0, XPROP_NET_WM_PID_ID.length);
        cmd[XPROP_NET_WM_PID_ID.length] = id;
        List<String> pidStr = ExecutingCommand.runNative(cmd, null);
        if (pidStr.isEmpty()) {
            return 0;
        }
        return ParseUtil.getFirstIntValue(pidStr.get(0));
    }

}
