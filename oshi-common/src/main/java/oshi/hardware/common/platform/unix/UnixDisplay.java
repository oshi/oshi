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
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

/**
 * A Display
 */
@ThreadSafe
public final class UnixDisplay extends AbstractDisplay {

    private final String devicePort;
    private final int connectorId;
    private final Supplier<Map<String, Pair<Integer, byte[]>>> xrandrData;

    /**
     * Constructor for UnixDisplay.
     *
     * @param edid a byte array representing a display EDID
     */
    public UnixDisplay(byte[] edid) {
        this(edid, Constants.UNKNOWN, -1);
    }

    /**
     * Constructor for UnixDisplay with device port and DRM connector ID.
     *
     * @param edid        a byte array representing a display EDID
     * @param devicePort  the DRM connector name (e.g. {@code HDMI-A-1})
     * @param connectorId the DRM connector ID ({@code -1} if not available)
     */
    public UnixDisplay(byte[] edid, String devicePort, int connectorId) {
        this(edid, devicePort, connectorId, memoize(Xrandr::getDisplayData));
    }

    /**
     * Constructor for UnixDisplay sharing xrandr data with the rest of its batch.
     *
     * @param edid        a byte array representing a display EDID
     * @param devicePort  the DRM connector name (e.g. {@code HDMI-A-1})
     * @param connectorId the DRM connector ID ({@code -1} if not available)
     * @param xrandrData  the display's source of xrandr data, expected to be memoized or already realized
     */
    private UnixDisplay(byte[] edid, String devicePort, int connectorId,
            Supplier<Map<String, Pair<Integer, byte[]>>> xrandrData) {
        super(edid);
        this.devicePort = devicePort;
        this.connectorId = connectorId;
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

    /**
     * Gets Display Information from xrandr. Used as a fallback when DRM sysfs is not available.
     *
     * @return An array of Display objects representing monitors, etc.
     */
    public static List<Display> getDisplays() {
        Map<String, Pair<Integer, byte[]>> data = Xrandr.getDisplayData();
        List<Display> displays = new ArrayList<>(data.size());
        // The data is already in hand, so these displays need no further xrandr query
        Supplier<Map<String, Pair<Integer, byte[]>>> sharedData = () -> data;
        for (Map.Entry<String, Pair<Integer, byte[]>> entry : data.entrySet()) {
            displays.add(new UnixDisplay(entry.getValue().getB(), entry.getKey(), entry.getValue().getA(), sharedData));
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
     * Builds a batch of displays sharing one query for the xrandr data behind {@link #getOutputName()}.
     * <p>
     * The query is memoized indefinitely, because a {@link Display} is an immutable snapshot: the output name matching
     * its connector cannot change over the object's lifetime. The hardware abstraction layer re-queries displays on its
     * own schedule, building a new batch with a new supplier, so a topology change is picked up there.
     *
     * @param drmData     the DRM sysfs data to build displays from
     * @param xrandrQuery the query for xrandr display data, run at most once for the whole batch
     * @return An array of Display objects representing monitors, etc.
     */
    static List<Display> getDisplays(List<Triplet<String, Integer, byte[]>> drmData,
            Supplier<Map<String, Pair<Integer, byte[]>>> xrandrQuery) {
        List<Display> displays = new ArrayList<>(drmData.size());
        Supplier<Map<String, Pair<Integer, byte[]>>> sharedData = memoize(xrandrQuery);
        for (Triplet<String, Integer, byte[]> drm : drmData) {
            displays.add(new UnixDisplay(drm.getC(), drm.getA(), drm.getB(), sharedData));
        }
        return displays;
    }
}
