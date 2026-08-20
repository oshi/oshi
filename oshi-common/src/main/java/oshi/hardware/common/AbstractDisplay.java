/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.Display;
import oshi.hardware.DisplayInfo;
import oshi.hardware.DisplayInfoImpl;
import oshi.util.Constants;

/**
 * A Display
 */
@Immutable
public abstract class AbstractDisplay implements Display {

    private final DisplayInfo displayInfo;
    private final String devicePort;

    /**
     * Constructor for AbstractDisplay from a raw EDID byte array, with no device port information.
     *
     * @param edid a byte array representing a display EDID
     */
    protected AbstractDisplay(byte[] edid) {
        this(edid, Constants.UNKNOWN);
    }

    /**
     * Constructor for AbstractDisplay from a raw EDID byte array.
     *
     * @param edid       a byte array representing a display EDID
     * @param devicePort the system-level device port identifier (e.g. DRM connector name {@code HDMI-A-1})
     */
    protected AbstractDisplay(byte[] edid, String devicePort) {
        this.displayInfo = new DisplayInfoImpl(edid);
        this.devicePort = devicePort;
    }

    /**
     * Constructor for AbstractDisplay from decoded display information, with no device port information.
     *
     * @param displayInfo the decoded display information
     */
    protected AbstractDisplay(DisplayInfo displayInfo) {
        this(displayInfo, Constants.UNKNOWN);
    }

    /**
     * Constructor for AbstractDisplay from decoded display information, used when a display reports its attributes
     * without providing an EDID. Pass a synthetic {@link DisplayInfo} to expose a synthesized EDID.
     *
     * @param displayInfo the decoded display information
     * @param devicePort  the system-level device port identifier (e.g. DRM connector name {@code HDMI-A-1})
     */
    protected AbstractDisplay(DisplayInfo displayInfo, String devicePort) {
        this.displayInfo = displayInfo;
        this.devicePort = devicePort;
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
    public String getDevicePort() {
        return this.devicePort;
    }

    @Override
    public String toString() {
        return this.displayInfo.toString();
    }
}
