/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.jna.platform.mac;

import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.platform.mac.CoreGraphics;

/**
 * Extensions to JNA's {@link CoreGraphics} for functions not yet bound upstream. This class should be considered
 * non-API as it may be removed if/when its code is incorporated into the JNA project.
 */
public interface CoreGraphicsExt extends CoreGraphics {

    CoreGraphicsExt INSTANCE = Native.load("/System/Library/Frameworks/CoreGraphics.framework/CoreGraphics",
            CoreGraphicsExt.class);

    /**
     * A {@link com.sun.jna.platform.mac.CoreGraphics.CGSize CGSize} returned by value from native functions.
     */
    @FieldOrder({ "width", "height" })
    class CGSizeByValue extends Structure implements Structure.ByValue {
        public double width;
        public double height;
    }

    /**
     * Returns the physical size of the display in millimeters.
     *
     * @param display The display identifier.
     * @return A {@link CGSizeByValue} containing the width and height in millimeters.
     */
    CGSizeByValue CGDisplayScreenSize(int display);
}
