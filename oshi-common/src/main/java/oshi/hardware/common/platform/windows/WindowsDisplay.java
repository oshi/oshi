/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.windows;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.Immutable;
import oshi.driver.common.windows.DisplayConnector.Connector;
import oshi.hardware.Display;
import oshi.hardware.DisplayMode;
import oshi.hardware.common.AbstractDisplay;
import oshi.util.Constants;

/**
 * Common logic for a Windows {@link Display}. Both bindings read the EDID from the registry and describe its connector
 * from the Connecting and Configuring Displays (CCD) API; only the native calls differ.
 */
@Immutable
public abstract class WindowsDisplay extends AbstractDisplay {

    private final @Nullable Connector connector;

    /**
     * Constructor for WindowsDisplay.
     *
     * @param edid      a byte array representing a display EDID
     * @param connector the connector this display is attached to, or {@code null} if Windows cannot resolve it
     */
    protected WindowsDisplay(byte[] edid, @Nullable Connector connector) {
        super(edid);
        this.connector = connector;
    }

    @Override
    public String getDevicePort() {
        return connector == null ? Constants.UNKNOWN : connector.getName();
    }

    @Override
    public Optional<DisplayMode> getCurrentMode() {
        return connector == null ? Optional.empty() : connector.getMode();
    }

    @Override
    public Optional<Boolean> isBuiltIn() {
        return connector == null ? Optional.empty() : Optional.of(connector.isBuiltIn());
    }

    @Override
    public boolean isPrimary() {
        // Windows defines the primary display as the one whose desktop origin is (0, 0). Displays cloning it share its
        // source mode and position, so each of them reports primary too.
        return getCurrentMode().map(mode -> mode.getX() == 0 && mode.getY() == 0).orElse(false);
    }
}
