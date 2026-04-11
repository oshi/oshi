/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix;

import java.util.List;
import java.util.stream.Collectors;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.Display;
import oshi.hardware.common.AbstractDisplay;
import oshi.util.driver.unix.Xrandr;

/**
 * A Display
 */
@ThreadSafe
public final class UnixDisplay extends AbstractDisplay {

    /**
     * Constructor for UnixDisplay.
     *
     * @param edid a byte array representing a display EDID
     */
    UnixDisplay(byte[] edid) {
        super(edid);
    }

    /**
     * Gets Display Information
     *
     * @return An array of Display objects representing monitors, etc.
     */
    public static List<Display> getDisplays() {
        return Xrandr.getEdidArrays().stream().map(UnixDisplay::new).collect(Collectors.toList());
    }
}
