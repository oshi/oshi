/*
 * Copyright 2016-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import java.util.Arrays;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.Display;
import oshi.util.EdidUtil;

/**
 * A Display
 */
@Immutable
public abstract class AbstractDisplay implements Display {

    private final byte[] edid;

    /**
     * Constructor for AbstractDisplay.
     *
     * @param edid a byte array representing a display EDID
     */
    protected AbstractDisplay(byte[] edid) {
        this.edid = Arrays.copyOf(edid, edid.length);
    }

    @Override
    public byte[] getEdid() {
        return Arrays.copyOf(this.edid, this.edid.length);
    }

    @Override
    public String toString() {
        return EdidUtil.toString(this.edid);
    }
}
