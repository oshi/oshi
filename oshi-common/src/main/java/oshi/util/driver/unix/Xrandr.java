/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.unix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;

/**
 * Utility to query xrandr
 */
@ThreadSafe
public final class Xrandr {

    private static final String[] XRANDR_VERBOSE = { "xrandr", "--verbose" };

    /**
     * Property names an X server may publish the EDID under, each as it appears in {@code xrandr --verbose} output,
     * including the trailing colon. {@code EDID} is the name in randrproto 1.3 and later; {@code RANDR_EDID} is the
     * name it replaced; {@code EDID_DATA} is the driver-side atom X.Org Server used through 1.6.
     */
    private static final String[] EDID_PROPERTIES = { "EDID:", "RANDR_EDID:", "EDID_DATA:" };

    private Xrandr() {
    }

    /**
     * Tests whether a property line names the EDID, under any of the property names an X server may use for it.
     *
     * @param trimmed a whitespace-trimmed line of {@code xrandr --verbose} output
     * @return true if the line is the header of an EDID property block
     */
    private static boolean isEdidProperty(String trimmed) {
        for (String property : EDID_PROPERTIES) {
            if (property.equals(trimmed)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets EDID byte arrays from the running X server via xrandr.
     *
     * @return a list of EDID byte arrays
     */
    public static List<byte[]> getEdidArrays() {
        // Special handling for X commands, don't use LC_ALL
        return getEdidArrays(runXrandr());
    }

    /**
     * Parse EDID arrays from xrandr verbose output.
     *
     * @param xrandr output of {@code xrandr --verbose}
     * @return a list of EDID byte arrays (at least 128 bytes each)
     */
    static List<byte[]> getEdidArrays(List<String> xrandr) {
        Map<String, Pair<Integer, byte[]>> data = getDisplayData(xrandr);
        List<byte[]> edids = new ArrayList<>(data.size());
        for (Pair<Integer, byte[]> pair : data.values()) {
            edids.add(pair.getB());
        }
        return Collections.unmodifiableList(edids);
    }

    /**
     * Gets display data from the running X server via xrandr, mapping each connected output's xrandr port name to its
     * connector ID and EDID.
     *
     * @return an ordered map of xrandr port name to {@link Pair} of connector ID ({@code -1} if not available) and EDID
     *         byte array
     */
    public static Map<String, Pair<Integer, byte[]>> getDisplayData() {
        return getDisplayData(runXrandr());
    }

    /**
     * Parse display data from xrandr verbose output. For each connected output, extracts the xrandr port name (the
     * first whitespace-delimited token on the output header line), the {@code CONNECTOR_ID} property (if present,
     * requires Linux 6.5+), and the EDID byte array. The parser is order-independent: {@code CONNECTOR_ID} may appear
     * before or after {@code EDID:}.
     *
     * @param xrandr output of {@code xrandr --verbose}
     * @return an ordered map of xrandr port name to {@link Pair} of connector ID ({@code -1} if not available) and EDID
     *         byte array (at least 128 bytes). Only connected outputs with a valid EDID are included.
     */
    static Map<String, Pair<Integer, byte[]>> getDisplayData(List<String> xrandr) {
        if (xrandr.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Pair<Integer, byte[]>> results = new LinkedHashMap<>();
        String currentPort = "";
        boolean currentConnected = false;
        int currentConnectorId = -1;
        byte[] currentEdid = null;
        StringBuilder sb = null;
        for (String s : xrandr) {
            // Output headers start at column 0; properties and modes are always indented
            if (!s.isEmpty() && !Character.isWhitespace(s.charAt(0))) {
                // Flush the previous output before starting a new one
                if (currentConnected && currentEdid != null) {
                    results.put(currentPort, new Pair<>(currentConnectorId, currentEdid));
                }
                String[] words = ParseUtil.whitespaces.split(s.trim(), -1);
                currentPort = words[0];
                currentConnected = words.length > 1 && "connected".equals(words[1]);
                currentConnectorId = -1;
                currentEdid = null;
                sb = null;
                continue;
            }
            String trimmed = s.trim();
            if (trimmed.startsWith("CONNECTOR_ID:")) {
                currentConnectorId = ParseUtil.parseLastInt(trimmed, -1);
            } else if (isEdidProperty(trimmed)) {
                sb = new StringBuilder();
            } else if (sb != null) {
                sb.append(trimmed);
                if (sb.length() < 256) {
                    continue;
                }
                currentEdid = ParseUtil.hexStringToByteArray(sb.toString());
                if (currentEdid.length < 128) {
                    currentEdid = null;
                }
                sb = null;
            }
        }
        // Flush the last output
        if (currentConnected && currentEdid != null) {
            results.put(currentPort, new Pair<>(currentConnectorId, currentEdid));
        }
        return Collections.unmodifiableMap(results);
    }

    /**
     * Finds the xrandr output name for a display identified by its DRM connector ID and/or EDID, matching by
     * {@code CONNECTOR_ID} first (Linux 6.5+) and falling back to EDID comparison.
     *
     * @param xrandrData  xrandr display data as returned by {@link #getDisplayData()}, which the caller is expected to
     *                    share among the displays it is naming rather than querying per display
     * @param connectorId the DRM connector ID ({@code -1} if not available)
     * @param edid        the EDID byte array from DRM sysfs
     * @return an {@link Optional} containing the xrandr output name, or empty if no X server is available or no match
     *         is found
     */
    public static Optional<String> findOutputName(Map<String, Pair<Integer, byte[]>> xrandrData, int connectorId,
            byte[] edid) {
        if (xrandrData.isEmpty()) {
            return Optional.empty();
        }
        // First try matching by CONNECTOR_ID (Linux 6.5+)
        if (connectorId >= 0) {
            for (Map.Entry<String, Pair<Integer, byte[]>> entry : xrandrData.entrySet()) {
                if (entry.getValue().getA() == connectorId) {
                    return Optional.of(entry.getKey());
                }
            }
        }
        // Fallback: match by first 128 bytes of EDID
        if (edid.length >= 128) {
            byte[] edid128 = Arrays.copyOf(edid, 128);
            for (Map.Entry<String, Pair<Integer, byte[]>> entry : xrandrData.entrySet()) {
                byte[] xrandrEdid = entry.getValue().getB();
                if (xrandrEdid.length >= 128 && Arrays.equals(edid128, Arrays.copyOf(xrandrEdid, 128))) {
                    return Optional.of(entry.getKey());
                }
            }
        }
        return Optional.empty();
    }

    private static List<String> runXrandr() {
        if (System.getenv("DISPLAY") == null) {
            return Collections.emptyList();
        }
        return ExecutingCommand.runNative(XRANDR_VERBOSE, null);
    }
}
