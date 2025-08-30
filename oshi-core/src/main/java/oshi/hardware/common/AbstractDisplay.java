/*
 * Copyright 2016-2025 The OSHI Project Contributors
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
    private final String connectionPort;

    /**
     * Constructor for AbstractDisplay.
     *
     * @param edid           a byte array representing a display EDID
     * @param connectionPort the display connection port
     */
    protected AbstractDisplay(byte[] edid, String connectionPort) {
        this.edid = Arrays.copyOf(edid, edid.length);
        this.connectionPort = connectionPort;
    }

    @Override
    public byte[] getEdid() {
        return Arrays.copyOf(this.edid, this.edid.length);
    }

    @Override
    public String getConnectionPort() {
        return this.connectionPort;
    }

    @Override
    public String toString() {
        return EdidUtil.toString(this.edid);
    }
}
