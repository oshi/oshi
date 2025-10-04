/*
 * Copyright 2021-2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.Xrandr;
import oshi.hardware.Display;
import oshi.hardware.common.AbstractDisplay;

/**
 * A Display
 */
@ThreadSafe
public final class UnixDisplay extends AbstractDisplay {

    /**
     * Constructor for UnixDisplay.
     *
     * @param edid           a byte array representing a display EDID
     * @param connectionPort the display connection port
     */
    UnixDisplay(byte[] edid, String connectionPort) {
        super(edid, connectionPort);
    }

    /**
     * Gets Display Information
     *
     * @return An array of Display objects representing monitors, etc.
     */
    public static List<Display> getDisplays() {

        List<byte[]> edidArrays = Xrandr.getEdidArrays();
        List<String> connectionPorts = Xrandr.getConnectionPorts();

        List<Display> displays = new ArrayList<>();

        for (int i = 0; i < edidArrays.size(); i++) {

            byte[] edid = edidArrays.get(i);
            String connectionPort = "Unknown";

            if (Objects.nonNull(connectionPorts.get(i)))
                connectionPort = connectionPorts.get(i);

            displays.add(new UnixDisplay(edid, connectionPort));
        }

        return displays;
    }
}
