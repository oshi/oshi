/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix;

import static oshi.util.Memoizer.memoize;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.Display;
import oshi.hardware.DisplayMode;
import oshi.hardware.common.AbstractDisplay;
import oshi.util.Constants;
import oshi.util.driver.unix.Xrandr;
import oshi.util.driver.unix.Xrandr.Output;
import oshi.util.tuples.Triplet;

/**
 * A Display
 */
@ThreadSafe
public final class UnixDisplay extends AbstractDisplay {

    private static final String[] BUILT_IN_CONNECTORS = { "eDP", "LVDS", "DSI" };
    // The amdgpu and radeon X drivers spell DisplayPort out (DisplayPort-0) where DRM and other drivers use DP
    private static final String[] EXTERNAL_CONNECTORS = { "DP", "DisplayPort", "HDMI", "DVI", "VGA", "TV", "Composite",
            "SVIDEO", "S-video", "Component", "DIN", "USB" };

    private final String devicePort;
    private final int connectorId;
    private final boolean primary;
    private final Supplier<List<Output>> xrandrData;

    /**
     * Constructor for UnixDisplay.
     *
     * @param edid a byte array representing a display EDID
     */
    public UnixDisplay(byte[] edid) {
        this(edid, Constants.UNKNOWN, -1, false);
    }

    /**
     * Constructor for UnixDisplay with device port and DRM connector ID.
     *
     * @param edid        a byte array representing a display EDID
     * @param devicePort  the DRM connector name (e.g. {@code HDMI-A-1})
     * @param connectorId the DRM connector ID ({@code -1} if not available)
     */
    public UnixDisplay(byte[] edid, String devicePort, int connectorId) {
        this(edid, devicePort, connectorId, false);
    }

    /**
     * Constructor for UnixDisplay with device port, DRM connector ID, and primary status.
     *
     * @param edid        a byte array representing a display EDID
     * @param devicePort  the DRM connector name (e.g. {@code HDMI-A-1})
     * @param connectorId the DRM connector ID ({@code -1} if not available)
     * @param primary     whether this display is the primary display
     */
    public UnixDisplay(byte[] edid, String devicePort, int connectorId, boolean primary) {
        this(edid, devicePort, connectorId, primary, memoize(Xrandr::getOutputs));
    }

    /**
     * Constructor for UnixDisplay sharing xrandr data with the rest of its batch.
     *
     * @param edid        a byte array representing a display EDID
     * @param devicePort  the DRM connector name (e.g. {@code HDMI-A-1})
     * @param connectorId the DRM connector ID ({@code -1} if not available)
     * @param primary     whether this display is the primary display
     * @param xrandrData  the display's source of xrandr data, expected to be memoized or already realized
     */
    private UnixDisplay(byte[] edid, String devicePort, int connectorId, boolean primary,
            Supplier<List<Output>> xrandrData) {
        super(edid);
        this.devicePort = devicePort;
        this.connectorId = connectorId;
        this.primary = primary;
        this.xrandrData = xrandrData;
    }

    @Override
    public String getDevicePort() {
        return this.devicePort;
    }

    @Override
    public Optional<String> getOutputName() {
        return findOutput().map(Output::getName);
    }

    @Override
    public Optional<DisplayMode> getCurrentMode() {
        return findOutput().flatMap(Output::getMode);
    }

    @Override
    public Optional<Boolean> isBuiltIn() {
        if (!Constants.UNKNOWN.equals(this.devicePort)) {
            return isBuiltInConnector(this.devicePort);
        }
        return getOutputName().flatMap(UnixDisplay::isBuiltInConnector);
    }

    @Override
    public boolean isPrimary() {
        return this.primary;
    }

    private Optional<Output> findOutput() {
        return Xrandr.findOutput(this.xrandrData.get(), this.connectorId, this.getDisplayInfo().getEdid());
    }

    /**
     * Classifies a connector as built in or external by its name. DRM names connectors by type ({@code eDP-1},
     * {@code HDMI-A-1}), and X drivers follow the same convention with or without the hyphen ({@code eDP1},
     * {@code HDMI1}).
     *
     * @param connector a DRM connector name or xrandr output name
     * @return {@code true} for an embedded panel connector ({@code eDP}, {@code LVDS}, {@code DSI}), {@code false} for
     *         an external connector type, or empty for a name that does not identify its type, such as a virtual output
     */
    static Optional<Boolean> isBuiltInConnector(String connector) {
        for (String prefix : BUILT_IN_CONNECTORS) {
            if (connector.startsWith(prefix)) {
                return Optional.of(Boolean.TRUE);
            }
        }
        // DPI (parallel RGB) drives both embedded panels and external adapters, and shares a prefix with DP
        if (connector.startsWith("DPI")) {
            return Optional.empty();
        }
        for (String prefix : EXTERNAL_CONNECTORS) {
            if (connector.startsWith(prefix)) {
                return Optional.of(Boolean.FALSE);
            }
        }
        return Optional.empty();
    }

    /**
     * Gets Display Information from xrandr. Used as a fallback when DRM sysfs is not available.
     *
     * @return An array of Display objects representing monitors, etc.
     */
    public static List<Display> getDisplays() {
        List<Output> outputs = Xrandr.getOutputs();
        List<Display> displays = new ArrayList<>(outputs.size());
        // The data is already in hand, so these displays need no further xrandr query
        Supplier<List<Output>> sharedData = () -> outputs;
        for (Output output : outputs) {
            displays.add(new UnixDisplay(output.getEdid(), output.getName(), output.getConnectorId(),
                    output.isPrimary(), sharedData));
        }
        return displays;
    }

    /**
     * Gets Display objects from DRM sysfs data, sharing one {@code xrandr --verbose} invocation among them rather than
     * running it once per display per call.
     *
     * @param drmData a list of {@link Triplet} of DRM connector name, DRM connector ID, and EDID byte array, as read
     *                from DRM sysfs
     * @return An array of Display objects representing monitors, etc.
     */
    public static List<Display> getDisplays(List<Triplet<String, Integer, byte[]>> drmData) {
        return getDisplays(drmData, Xrandr::getOutputs);
    }

    /**
     * Builds a batch of displays sharing one query for the xrandr data behind {@link #getOutputName()},
     * {@link #getCurrentMode()} and {@link #isPrimary()}.
     * <p>
     * The query is memoized indefinitely, because a {@link Display} is an immutable snapshot: the output matching its
     * connector, and the mode and primary status read with it, do not change over the object's lifetime. The hardware abstraction layer
     * re-queries displays on its own schedule, building a new batch with a new supplier, so a topology change is picked
     * up there.
     *
     * @param drmData     the DRM sysfs data to build displays from
     * @param xrandrQuery the query for xrandr display data, run at most once for the whole batch
     * @return An array of Display objects representing monitors, etc.
     */
    static List<Display> getDisplays(List<Triplet<String, Integer, byte[]>> drmData,
            Supplier<List<Output>> xrandrQuery) {
        List<Display> displays = new ArrayList<>(drmData.size());
        Supplier<List<Output>> sharedData = memoize(xrandrQuery);
        for (Triplet<String, Integer, byte[]> drm : drmData) {
            boolean primary = Xrandr.findPrimaryStatus(sharedData.get(), drm.getB(), drm.getC());
            displays.add(new UnixDisplay(drm.getC(), drm.getA(), drm.getB(), primary, sharedData));
        }
        return displays;
    }
}
