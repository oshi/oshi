/*
 * Copyright 2021-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.jna.Pointer;
import com.sun.jna.platform.DesktopWindow;
import com.sun.jna.platform.WindowUtils;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.HWND;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.ByRef.CloseableIntByReference;
import oshi.software.os.OSDesktopWindow;

/**
 * Utility to query Desktop windows
 */
@ThreadSafe
public final class EnumWindows {

    private static final DWORD GW_HWNDNEXT = new DWORD(2);

    private EnumWindows() {
    }

    /**
     * Gets windows on the operating system's GUI desktop.
     *
     * @param visibleOnly Whether to restrict the list to only windows visible to the user.
     * @return A list of {@link oshi.software.os.OSDesktopWindow} objects representing the desktop windows.
     */
    public static List<OSDesktopWindow> queryDesktopWindows(boolean visibleOnly) {
        // Get the windows using JNA's implementation
        List<DesktopWindow> windows = WindowUtils.getAllWindows(true);
        // Prepare a list to return
        List<OSDesktopWindow> windowList = new ArrayList<>();
        // Populate the list
        Map<HWND, Integer> zOrderMap = new HashMap<>();
        for (DesktopWindow window : windows) {
            HWND hWnd = window.getHWND();
            if (hWnd != null) {
                boolean visible = User32.INSTANCE.IsWindowVisible(hWnd);
                if (!visibleOnly || visible) {
                    if (!zOrderMap.containsKey(hWnd)) {
                        updateWindowZOrderMap(hWnd, zOrderMap);
                    }
                    try (CloseableIntByReference pProcessId = new CloseableIntByReference()) {
                        User32.INSTANCE.GetWindowThreadProcessId(hWnd, pProcessId);
                        windowList.add(new OSDesktopWindow(Pointer.nativeValue(hWnd.getPointer()), window.getTitle(),
                                window.getFilePath(), window.getLocAndSize(), pProcessId.getValue(),
                                zOrderMap.get(hWnd), visible));
                    }
                }
            }
        }
        return windowList;
    }

    private static void updateWindowZOrderMap(HWND hWnd, Map<HWND, Integer> zOrderMap) {
        if (hWnd != null) {
            int zOrder = 1;
            HWND h = new HWND(hWnd.getPointer());
            // First is highest, so decrement
            do {
                zOrderMap.put(h, --zOrder);
            } while ((h = User32.INSTANCE.GetWindow(h, GW_HWNDNEXT)) != null);
            // now add lowest value to all
            final int offset = zOrder * -1;
            zOrderMap.replaceAll((k, v) -> v + offset);
        }
    }
}
