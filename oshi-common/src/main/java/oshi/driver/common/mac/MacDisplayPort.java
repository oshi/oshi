/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.mac;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.Constants;
import oshi.util.ParseUtil;

/**
 * Derives a display's device port from the IOKit strings the macOS display twins already read, so both the JNA and FFM
 * implementations share one parser.
 */
@ThreadSafe
public final class MacDisplayPort {

    private MacDisplayPort() {
    }

    /**
     * Derives the port from an {@code IOPortTransportStateDisplayPort} node's {@code TransportDescription}, which names
     * the physical port and the transport it carries (e.g. {@code Port-HDMI@1/DisplayPort}). The port is the part
     * before the {@code /}.
     *
     * @param transportDescription the {@code TransportDescription} string, or {@code null}
     * @return the port (e.g. {@code Port-HDMI@1}), or {@link Constants#UNKNOWN} if not available
     */
    public static String fromTransportDescription(@Nullable String transportDescription) {
        return beforeDelimiter(transportDescription, '/');
    }

    /**
     * Derives the port from a framebuffer node's {@code IONameMatched}, which is the device tree name followed by the
     * SoC identifier (e.g. {@code disp0,t6030}). The port is the part before the {@code ,}.
     *
     * @param deviceTreeName the {@code IONameMatched} string, or {@code null}
     * @return the port (e.g. {@code disp0}), or {@link Constants#UNKNOWN} if not available
     */
    public static String fromDeviceTreeName(@Nullable String deviceTreeName) {
        return beforeDelimiter(deviceTreeName, ',');
    }

    private static String beforeDelimiter(@Nullable String value, char delimiter) {
        if (value == null) {
            return Constants.UNKNOWN;
        }
        int idx = value.indexOf(delimiter);
        // Normalizes both an empty input and a value that leads with the delimiter to the sentinel.
        return ParseUtil.getStringValueOrUnknown(idx < 0 ? value : value.substring(0, idx));
    }
}
