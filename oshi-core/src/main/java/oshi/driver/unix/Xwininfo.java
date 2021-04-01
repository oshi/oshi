/*
 * MIT License
 *
 * Copyright (c) 2010 - 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.driver.unix;

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

    private Xwininfo() {
    }

    /**
     * Gets windows on the operating system's GUI desktop.
     *
     * @param visibleOnly
     *            Whether to restrict the list to only windows visible to the user.
     * @return A list of {@link oshi.software.os.OSDesktopWindow} objects
     *         representing the desktop windows.
     */
    public static List<OSDesktopWindow> queryXWindows(boolean visibleOnly) {
        // Attempted to implement using native X11 code. However, this produced native X
        // errors (e.g., BadValue) which cannot be caught on the Java side and
        // terminated the thread. Using x command lines which execute in a separate
        // process. Errors are caught by the terminal process and safely ignored.

        // Get visible windows in their Z order. Assign 1 to bottom and increment.
        // All other non visible windows will be assigned 0.
        Map<String, Integer> zOrderMap = new HashMap<>();
        int z = 0;

        // X commands don't work with LC_ALL
        List<String> stacking = ExecutingCommand.runNative(NET_CLIENT_LIST_STACKING, null);
        if (!stacking.isEmpty()) {
            String stack = stacking.get(0);
            int bottom = stack.indexOf("0x");
            if (bottom >= 0) {
                for (String id : stack.substring(bottom).split(", ")) {
                    zOrderMap.put(id, ++z);
                }
            }
        }
        // Get all windows along with title and path info
        Pattern windowPattern = Pattern.compile(
                "(0x\\S+) (?:\"(.+)\")?.*: \\((?:\"(.+)\" \".+\")?\\)  (\\d+)x(\\d+)\\+.+  \\+(-?\\d+)\\+(-?\\d+)");
        Map<String, String> windowNameMap = new HashMap<>();
        Map<String, String> windowPathMap = new HashMap<>();
        // This map will include all the windows, preserve the insertion order
        Map<String, Rectangle> windowMap = new LinkedHashMap<>();
        // X commands don't work with LC_ALL
        for (String line : ExecutingCommand.runNative(XWININFO_ROOT_TREE, null)) {
            Matcher m = windowPattern.matcher(line.trim());
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
        // Get info for each window
        // Prepare a list to return
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
