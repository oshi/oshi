/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.mac;

import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static oshi.ffm.mac.CoreGraphicsFunctions.CGRectMakeWithDictionaryRepresentation;
import static oshi.ffm.mac.CoreGraphicsFunctions.CGWindowListCopyWindowInfo;
import static oshi.ffm.mac.MacSystem.CG_HEIGHT;
import static oshi.ffm.mac.MacSystem.CG_ORIGIN;
import static oshi.ffm.mac.MacSystem.CG_POINT;
import static oshi.ffm.mac.MacSystem.CG_RECT;
import static oshi.ffm.mac.MacSystem.CG_SIZE;
import static oshi.ffm.mac.MacSystem.CG_SIZE_ELEM;
import static oshi.ffm.mac.MacSystem.CG_WIDTH;
import static oshi.ffm.mac.MacSystem.CG_X;
import static oshi.ffm.mac.MacSystem.CG_Y;

import java.awt.Rectangle;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.mac.CoreFoundation.CFArrayRef;
import oshi.ffm.mac.CoreFoundation.CFBooleanRef;
import oshi.ffm.mac.CoreFoundation.CFDictionaryRef;
import oshi.ffm.mac.CoreFoundation.CFNumberRef;
import oshi.ffm.mac.CoreFoundation.CFStringRef;
import oshi.software.os.OSDesktopWindow;
import oshi.util.FormatUtil;
import oshi.util.platform.mac.CFUtilFFM;

/**
 * Utility to query desktop windows using FFM (no JNA dependency).
 */
@ThreadSafe
public final class WindowInfoFFM {

    // CGWindowListOption constants (CGWindow.h)
    private static final int kCGNullWindowID = 0;
    private static final int kCGWindowListOptionAll = 0;
    private static final int kCGWindowListOptionOnScreenOnly = 1 << 0;
    private static final int kCGWindowListExcludeDesktopElements = 1 << 4;

    private WindowInfoFFM() {
    }

    /**
     * Gets windows on the operating system's GUI desktop.
     *
     * @param visibleOnly Whether to restrict the list to only windows visible to the user.
     * @return A list of {@link OSDesktopWindow} objects representing the desktop windows.
     */
    public static List<OSDesktopWindow> queryDesktopWindows(boolean visibleOnly) {
        List<OSDesktopWindow> windowList = new ArrayList<>();
        try (Arena arena = Arena.ofConfined()) {
            int option = visibleOnly ? kCGWindowListOptionOnScreenOnly | kCGWindowListExcludeDesktopElements
                    : kCGWindowListOptionAll;
            MemorySegment rawArray = CGWindowListCopyWindowInfo(option, kCGNullWindowID);
            if (rawArray.equals(MemorySegment.NULL)) {
                return windowList;
            }
            CFArrayRef windowInfo = new CFArrayRef(rawArray);
            try {
                int numWindows = windowInfo.getCount();

                CFStringRef kCGWindowIsOnscreen = CFStringRef.createCFString("kCGWindowIsOnscreen");
                CFStringRef kCGWindowNumber = CFStringRef.createCFString("kCGWindowNumber");
                CFStringRef kCGWindowOwnerPID = CFStringRef.createCFString("kCGWindowOwnerPID");
                CFStringRef kCGWindowLayer = CFStringRef.createCFString("kCGWindowLayer");
                CFStringRef kCGWindowBounds = CFStringRef.createCFString("kCGWindowBounds");
                CFStringRef kCGWindowName = CFStringRef.createCFString("kCGWindowName");
                CFStringRef kCGWindowOwnerName = CFStringRef.createCFString("kCGWindowOwnerName");
                try {
                    for (int i = 0; i < numWindows; i++) {
                        MemorySegment dictSeg = windowInfo.getValueAtIndex(i);
                        if (dictSeg.equals(MemorySegment.NULL)) {
                            continue;
                        }
                        CFDictionaryRef windowRef = new CFDictionaryRef(dictSeg);

                        // kCGWindowIsOnscreen is optional — absent means visible
                        MemorySegment onscreenSeg = windowRef.getValue(kCGWindowIsOnscreen);
                        boolean visible = onscreenSeg.equals(MemorySegment.NULL)
                                || new CFBooleanRef(onscreenSeg).booleanValue();
                        if (visibleOnly && !visible) {
                            continue;
                        }

                        long windowNumber = new CFNumberRef(windowRef.getValue(kCGWindowNumber)).longValue();
                        long ownerPID = new CFNumberRef(windowRef.getValue(kCGWindowOwnerPID)).longValue();
                        int windowLayer = new CFNumberRef(windowRef.getValue(kCGWindowLayer)).intValue();

                        // Parse CGRect from the bounds dictionary
                        MemorySegment boundsSeg = windowRef.getValue(kCGWindowBounds);
                        MemorySegment rectBuf = arena.allocate(CG_RECT);
                        Rectangle windowBounds = new Rectangle();
                        if (!boundsSeg.equals(MemorySegment.NULL)
                                && CGRectMakeWithDictionaryRepresentation(boundsSeg, rectBuf)) {
                            MemorySegment origin = rectBuf.asSlice(CG_RECT.byteOffset(CG_ORIGIN), CG_POINT.byteSize());
                            MemorySegment size = rectBuf.asSlice(CG_RECT.byteOffset(CG_SIZE_ELEM), CG_SIZE.byteSize());
                            double x = origin.get(JAVA_DOUBLE, CG_POINT.byteOffset(CG_X));
                            double y = origin.get(JAVA_DOUBLE, CG_POINT.byteOffset(CG_Y));
                            double w = size.get(JAVA_DOUBLE, CG_SIZE.byteOffset(CG_WIDTH));
                            double h = size.get(JAVA_DOUBLE, CG_SIZE.byteOffset(CG_HEIGHT));
                            windowBounds = new Rectangle(FormatUtil.roundToInt(x), FormatUtil.roundToInt(y),
                                    FormatUtil.roundToInt(w), FormatUtil.roundToInt(h));
                        }

                        String windowName = CFUtilFFM.cfPointerToString(windowRef.getValue(kCGWindowName), false);
                        String ownerName = CFUtilFFM.cfPointerToString(windowRef.getValue(kCGWindowOwnerName), false);
                        if (windowName.isEmpty()) {
                            windowName = ownerName;
                        } else {
                            windowName = windowName + "(" + ownerName + ")";
                        }

                        windowList.add(new OSDesktopWindow(windowNumber, windowName, ownerName, windowBounds, ownerPID,
                                windowLayer, visible));
                    }
                } finally {
                    kCGWindowIsOnscreen.release();
                    kCGWindowNumber.release();
                    kCGWindowOwnerPID.release();
                    kCGWindowLayer.release();
                    kCGWindowBounds.release();
                    kCGWindowName.release();
                    kCGWindowOwnerName.release();
                }
            } finally {
                windowInfo.release();
            }
        } catch (Throwable e) {
            return windowList;
        }
        return windowList;
    }
}
