/*
 * Copyright 2021-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.jna.platform.mac;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.platform.mac.CoreFoundation.CFArrayRef;
import com.sun.jna.platform.mac.CoreFoundation.CFDictionaryRef;

import oshi.util.Util;

/**
 * The Core Graphics framework is based on the Quartz advanced drawing engine. It provides low-level, lightweight 2D
 * rendering with unmatched output fidelity. You use this framework to handle path-based drawing, transformations, color
 * management, offscreen rendering, patterns, gradients and shadings, image data management, image creation, and image
 * masking, as well as PDF document creation, display, and parsing.
 * <p>
 * In macOS, Core Graphics also includes services for working with display hardware, low-level user input events, and
 * the windowing system.
 */
public interface CoreGraphics extends Library {

    CoreGraphics INSTANCE = Native.load("CoreGraphics", CoreGraphics.class);

    int kCGNullWindowID = 0;

    int kCGWindowListOptionAll = 0;
    int kCGWindowListOptionOnScreenOnly = 1 << 0;
    int kCGWindowListOptionOnScreenAboveWindow = 1 << 1;
    int kCGWindowListOptionOnScreenBelowWindow = 1 << 2;
    int kCGWindowListOptionIncludingWindow = 1 << 3;
    int kCGWindowListExcludeDesktopElements = 1 << 4;

    /**
     * A point with X and Y coordinates
     */
    @FieldOrder({ "x", "y" })
    class CGPoint extends Structure {
        public double x;
        public double y;

    }

    /**
     * A size with width and height
     */
    @FieldOrder({ "width", "height" })
    class CGSize extends Structure {
        public double width;
        public double height;
    }

    /**
     * A rectangle with origin and size
     */
    @FieldOrder({ "origin", "size" })
    class CGRect extends Structure implements AutoCloseable {
        public CGPoint origin;
        public CGSize size;

        @Override
        public void close() {
            Util.freeMemory(getPointer());
        }
    }

    CFArrayRef CGWindowListCopyWindowInfo(int option, int relativeToWindow);

    boolean CGRectMakeWithDictionaryRepresentation(CFDictionaryRef dict, CGRect rect);
}
