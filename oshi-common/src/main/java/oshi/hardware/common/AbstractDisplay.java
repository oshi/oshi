/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.Display;
import oshi.hardware.DisplayInfo;
import oshi.hardware.DisplayInfoImpl;

/**
 * A Display
 */
@Immutable
public abstract class AbstractDisplay implements Display {

    private final DisplayInfo displayInfo;

    /**
     * Constructor for AbstractDisplay from a raw EDID byte array.
     *
     * @param edid a byte array representing a display EDID
     */
    protected AbstractDisplay(byte[] edid) {
        this.displayInfo = new DisplayInfoImpl(edid);
    }

    /**
     * Constructor for AbstractDisplay from decoded display information, used when a display reports its attributes
     * without providing an EDID. Pass a synthetic {@link DisplayInfo} to expose a synthesized EDID.
     *
     * @param displayInfo the decoded display information
     */
    protected AbstractDisplay(DisplayInfo displayInfo) {
        this.displayInfo = displayInfo;
    }

    @Deprecated
    @Override
    public byte[] getEdid() {
        return this.displayInfo.getEdid();
    }

    @Override
    public DisplayInfo getDisplayInfo() {
        return this.displayInfo;
    }

    @Override
    public String toString() {
        return this.displayInfo.toString();
    }
}
