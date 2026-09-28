/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.mac.CoreGraphicsDisplay;
import oshi.hardware.Display;
import oshi.hardware.DisplayInfo;
import oshi.hardware.DisplayMode;
import oshi.hardware.common.AbstractDisplay;

/**
 * Common logic for a macOS {@link Display}. Both bindings enumerate displays through IOKit and query CoreGraphics for
 * their modes; matching one to the other is shared.
 * <p>
 * The CoreGraphics query is shared by every display in a batch and is not run until a display first needs it. It is
 * then kept for the life of the batch, because a {@link Display} is an immutable snapshot: the hardware abstraction
 * layer re-queries displays on its own schedule, building a new batch with a new supplier.
 */
@ThreadSafe
public abstract class MacDisplay extends AbstractDisplay {

    private final String devicePort;
    private final @Nullable Boolean builtIn;
    private final Supplier<List<CoreGraphicsDisplay>> coreGraphicsDisplays;

    /**
     * Constructor for a display that reports a real EDID.
     *
     * @param edid                 a byte array representing a display EDID
     * @param devicePort           the device port this display is attached to
     * @param builtIn              whether the display is built in, if the way it was enumerated shows it; {@code null}
     *                             to ask CoreGraphics
     * @param coreGraphicsDisplays the batch's source of CoreGraphics displays, expected to be memoized
     */
    protected MacDisplay(byte[] edid, String devicePort, @Nullable Boolean builtIn,
            Supplier<List<CoreGraphicsDisplay>> coreGraphicsDisplays) {
        super(edid);
        this.devicePort = devicePort;
        this.builtIn = builtIn;
        this.coreGraphicsDisplays = coreGraphicsDisplays;
    }

    /**
     * Constructor for the Apple Silicon built-in panel, which has no EDID and is described by a synthetic
     * {@link DisplayInfo}.
     *
     * @param displayInfo          the synthesized display info
     * @param devicePort           the device port this display is attached to
     * @param coreGraphicsDisplays the batch's source of CoreGraphics displays, expected to be memoized
     */
    protected MacDisplay(DisplayInfo displayInfo, String devicePort,
            Supplier<List<CoreGraphicsDisplay>> coreGraphicsDisplays) {
        super(displayInfo);
        this.devicePort = devicePort;
        this.builtIn = Boolean.TRUE;
        this.coreGraphicsDisplays = coreGraphicsDisplays;
    }

    @Override
    public String getDevicePort() {
        return this.devicePort;
    }

    @Override
    public Optional<DisplayMode> getCurrentMode() {
        return findCoreGraphicsDisplay().flatMap(CoreGraphicsDisplay::getMode);
    }

    @Override
    public Optional<Boolean> isBuiltIn() {
        if (this.builtIn != null) {
            return Optional.of(this.builtIn);
        }
        return findCoreGraphicsDisplay().map(CoreGraphicsDisplay::isBuiltIn);
    }

    @Override
    public boolean isPrimary() {
        return findCoreGraphicsDisplay().map(CoreGraphicsDisplay::isMain).orElse(false);
    }

    private Optional<CoreGraphicsDisplay> findCoreGraphicsDisplay() {
        List<CoreGraphicsDisplay> displays = this.coreGraphicsDisplays.get();
        // Only the Apple Silicon built-in panel lacks an EDID to match on
        if (getDisplayInfo().isEdidSynthetic()) {
            return CoreGraphicsDisplay.matchBuiltIn(displays);
        }
        return CoreGraphicsDisplay.matchEdid(displays, getDisplayInfo().getEdid());
    }
}
