/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.Display;
import oshi.hardware.common.AbstractDisplay;
import oshi.util.driver.unix.Xrandr;
import oshi.util.tuples.Pair;

/**
 * A Display
 */
@ThreadSafe
public final class UnixDisplay extends AbstractDisplay {

    private final int connectorId;

    /**
     * Constructor for UnixDisplay.
     *
     * @param edid a byte array representing a display EDID
     */
    public UnixDisplay(byte[] edid) {
        super(edid);
        this.connectorId = -1;
    }

    /**
     * Constructor for UnixDisplay with device port and DRM connector ID.
     *
     * @param edid        a byte array representing a display EDID
     * @param devicePort  the DRM connector name (e.g. {@code HDMI-A-1})
     * @param connectorId the DRM connector ID ({@code -1} if not available)
     */
    public UnixDisplay(byte[] edid, String devicePort, int connectorId) {
        super(edid, devicePort);
        this.connectorId = connectorId;
    }

    @Override
    public Optional<String> getOutputName() {
        return Xrandr.findOutputName(this.connectorId, this.getDisplayInfo().getEdid());
    }

    /**
     * Gets Display Information from xrandr. Used as a fallback when DRM sysfs is not available.
     *
     * @return An array of Display objects representing monitors, etc.
     */
    public static List<Display> getDisplays() {
        Map<String, Pair<Integer, byte[]>> data = Xrandr.getDisplayData();
        List<Display> displays = new ArrayList<>(data.size());
        for (Map.Entry<String, Pair<Integer, byte[]>> entry : data.entrySet()) {
            displays.add(new UnixDisplay(entry.getValue().getB(), entry.getKey(), entry.getValue().getA()));
        }
        return displays;
    }
}
