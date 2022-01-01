/*
 * MIT License
 *
 * Copyright (c) 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.jna.platform.mac;

import com.sun.jna.Library; // NOSONAR squid:S1191
import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.platform.mac.CoreFoundation.CFArrayRef;
import com.sun.jna.platform.mac.CoreFoundation.CFDictionaryRef;

/**
 * The Core Graphics framework is based on the Quartz advanced drawing engine.
 * It provides low-level, lightweight 2D rendering with unmatched output
 * fidelity. You use this framework to handle path-based drawing,
 * transformations, color management, offscreen rendering, patterns, gradients
 * and shadings, image data management, image creation, and image masking, as
 * well as PDF document creation, display, and parsing.
 * <p>
 * In macOS, Core Graphics also includes services for working with display
 * hardware, low-level user input events, and the windowing system.
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
    class CGRect extends Structure {
        public CGPoint origin;
        public CGSize size;
    }

    CFArrayRef CGWindowListCopyWindowInfo(int option, int relativeToWindow);

    boolean CGRectMakeWithDictionaryRepresentation(CFDictionaryRef dict, CGRect rect);
}
