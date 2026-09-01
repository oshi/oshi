/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix;

import static oshi.util.Memoizer.memoize;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.Display;
import oshi.hardware.common.AbstractDisplay;
import oshi.util.Constants;
import oshi.util.driver.unix.Xrandr;
import oshi.util.tuples.Triplet;

/**
 * A Display
 */
@ThreadSafe
public final class UnixDisplay extends AbstractDisplay {

    private final String devicePort;
    private final int connectorId;
    private final boolean primary;
    private final Supplier<Map<String, Triplet<Integer, byte[], Boolean>>> xrandrData;

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
        this(edid, devicePort, connectorId, primary, memoize(Xrandr::getDisplayData));
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
            Supplier<Map<String, Triplet<Integer, byte[], Boolean>>> xrandrData) {
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
        return Xrandr.findOutputName(this.xrandrData.get(), this.connectorId, this.getDisplayInfo().getEdid());
    }

    @Override
    public boolean isPrimary() {
        return this.primary;
    }

    /**
     * Gets Display Information from xrandr. Used as a fallback when DRM sysfs is not available.
     *
     * @return An array of Display objects representing monitors, etc.
     */
    public static List<Display> getDisplays() {
        Map<String, Triplet<Integer, byte[], Boolean>> data = Xrandr.getDisplayData();
        List<Display> displays = new ArrayList<>(data.size());
        // The data is already in hand, so these displays need no further xrandr query
        Supplier<Map<String, Triplet<Integer, byte[], Boolean>>> sharedData = () -> data;
        for (Map.Entry<String, Triplet<Integer, byte[], Boolean>> entry : data.entrySet()) {
            displays.add(new UnixDisplay(entry.getValue().getB(), entry.getKey(), entry.getValue().getA(),
                    entry.getValue().getC(), sharedData));
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
        return getDisplays(drmData, Xrandr::getDisplayData);
    }

    /**
     * Builds a batch of displays sharing one query for the xrandr data behind {@link #getOutputName()} and
     * {@link #isPrimary()}.
     * <p>
     * The query is memoized indefinitely, because a {@link Display} is an immutable snapshot: the output name matching
     * its connector and primary status cannot change over the object's lifetime. The hardware abstraction layer
     * re-queries displays on its own schedule, building a new batch with a new supplier, so a topology change is
     * picked up there.
     *
     * @param drmData     the DRM sysfs data to build displays from
     * @param xrandrQuery the query for xrandr display data, run at most once for the whole batch
     * @return An array of Display objects representing monitors, etc.
     */
    static List<Display> getDisplays(List<Triplet<String, Integer, byte[]>> drmData,
            Supplier<Map<String, Triplet<Integer, byte[], Boolean>>> xrandrQuery) {
        List<Display> displays = new ArrayList<>(drmData.size());
        Supplier<Map<String, Triplet<Integer, byte[], Boolean>>> sharedData = memoize(xrandrQuery);
        for (Triplet<String, Integer, byte[]> drm : drmData) {
            boolean primary = Xrandr.findPrimaryStatus(sharedData.get(), drm.getB(), drm.getC());
            displays.add(new UnixDisplay(drm.getC(), drm.getA(), drm.getB(), primary, sharedData));
        }
        return displays;
    }
}
