/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.unix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.Immutable;
import oshi.annotation.concurrent.NotThreadSafe;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.DisplayMode;
import oshi.hardware.DisplayModeImpl;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

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

    /** An output's area on the X screen as its header line gives it, e.g. {@code 1920x1080+1920+0}. */
    private static final Pattern GEOMETRY = Pattern.compile("(\\d+)x(\\d+)\\+(-?\\d+)\\+(-?\\d+)");

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
        List<Output> outputs = getOutputs(xrandr);
        List<byte[]> edids = new ArrayList<>(outputs.size());
        for (Output output : outputs) {
            edids.add(output.getEdid());
        }
        return Collections.unmodifiableList(edids);
    }

    /**
     * Gets the connected outputs of the running X server via xrandr.
     *
     * @return the connected outputs that report a valid EDID, in the order xrandr lists them
     */
    public static List<Output> getOutputs() {
        return getOutputs(runXrandr());
    }

    /**
     * Parse the connected outputs from xrandr verbose output. For each connected output, extracts the xrandr port name
     * (the first whitespace-delimited token on the output header line), the {@code CONNECTOR_ID} property (if present,
     * requires Linux 6.5+), the EDID byte array, the current mode, and the primary status. The parser is
     * order-independent:
     * {@code CONNECTOR_ID} may appear before or after {@code EDID:}.
     * <p>
     * The current mode combines the output header, which gives the output's area on the X screen and its rotation (e.g.
     * {@code HDMI-1 connected primary 1920x1080+1920+0 (0x46) left (normal left inverted right ...)}), with the mode
     * block marked {@code *current}, whose {@code h:} and {@code v:} lines give the unrotated pixel size and refresh
     * rate. An output that is connected but disabled has no geometry on its header line and no current mode.
     *
     * @param xrandr output of {@code xrandr --verbose}
     * @return the connected outputs with a valid EDID (at least 128 bytes), in the order xrandr lists them
     */
    static List<Output> getOutputs(List<String> xrandr) {
        if (xrandr.isEmpty()) {
            return Collections.emptyList();
        }
        List<Output> results = new ArrayList<>();
        OutputBuilder current = null;
        StringBuilder sb = null;
        for (String s : xrandr) {
            // Output headers start at column 0; properties and modes are always indented
            if (!s.isEmpty() && !Character.isWhitespace(s.charAt(0))) {
                // Flush the previous output before starting a new one
                addIfValid(results, current);
                String[] words = ParseUtil.whitespaces.split(s.trim(), -1);
                current = words.length > 1 && "connected".equals(words[1]) ? new OutputBuilder(words) : null;
                sb = null;
                continue;
            }
            if (current == null) {
                continue;
            }
            String trimmed = s.trim();
            if (trimmed.startsWith("CONNECTOR_ID:")) {
                current.connectorId = ParseUtil.parseLastInt(trimmed, -1);
            } else if (isEdidProperty(trimmed)) {
                sb = new StringBuilder();
            } else if (sb != null) {
                sb.append(trimmed);
                if (sb.length() < 256) {
                    continue;
                }
                byte[] edid = ParseUtil.hexStringToByteArray(sb.toString());
                current.edid = edid.length < 128 ? null : edid;
                sb = null;
            } else {
                current.parseModeLine(trimmed);
            }
        }
        // Flush the last output
        addIfValid(results, current);
        return Collections.unmodifiableList(results);
    }

    private static void addIfValid(List<Output> results, @Nullable OutputBuilder builder) {
        if (builder != null && builder.edid != null) {
            results.add(builder.build(builder.edid));
        }
    }

    /**
     * Finds the xrandr output for a display identified by its DRM connector ID and/or EDID, matching by
     * {@code CONNECTOR_ID} first (Linux 6.5+) and falling back to EDID comparison.
     *
     * @param outputs     xrandr outputs as returned by {@link #getOutputs()}, which the caller is expected to share
     *                    among the displays it is matching rather than querying per display
     * @param connectorId the DRM connector ID ({@code -1} if not available)
     * @param edid        the EDID byte array from DRM sysfs
     * @return an {@link Optional} containing the matching output, or empty if no X server is available or no match is
     *         found
     */
    public static Optional<Output> findOutput(List<Output> outputs, int connectorId, byte[] edid) {
        // First try matching by CONNECTOR_ID (Linux 6.5+)
        if (connectorId >= 0) {
            for (Output output : outputs) {
                if (output.getConnectorId() == connectorId) {
                    return Optional.of(output);
                }
            }
        }
        // Fallback: match by first 128 bytes of EDID
        if (edid.length >= 128) {
            byte[] edid128 = Arrays.copyOf(edid, 128);
            for (Output output : outputs) {
                byte[] xrandrEdid = output.edid;
                if (xrandrEdid.length >= 128 && Arrays.equals(edid128, Arrays.copyOf(xrandrEdid, 128))) {
                    return Optional.of(output);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Finds the primary status for a display identified by its DRM connector ID and/or EDID, matching by
     * {@code CONNECTOR_ID} first (Linux 6.5+) and falling back to EDID comparison.
     *
     * @param outputs     xrandr outputs as returned by {@link #getOutputs()}, which the caller is expected to share
     *                    among the displays it is naming rather than querying per display
     * @param connectorId the DRM connector ID ({@code -1} if not available)
     * @param edid        the EDID byte array from DRM sysfs
     * @return {@code true} if the matched xrandr output is marked as primary, {@code false} otherwise
     */
    public static boolean findPrimaryStatus(List<Output> outputs, int connectorId, byte[] edid) {
        if (outputs.isEmpty()) {
            return false;
        }
        // First try matching by CONNECTOR_ID (Linux 6.5+)
        if (connectorId >= 0) {
            for (Output output : outputs) {
                if (output.getConnectorId() == connectorId) {
                    return output.isPrimary();
                }
            }
        }
        // Fallback: match by first 128 bytes of EDID
        if (edid.length >= 128) {
            byte[] edid128 = Arrays.copyOf(edid, 128);
            for (Output output : outputs) {
                byte[] xrandrEdid = output.edid;
                if (xrandrEdid.length >= 128 && Arrays.equals(edid128, Arrays.copyOf(xrandrEdid, 128))) {
                    return output.isPrimary();
                }
            }
        }
        return false;
    }

    private static List<String> runXrandr() {
        if (System.getenv("DISPLAY") == null) {
            return Collections.emptyList();
        }
        return ExecutingCommand.runNative(XRANDR_VERBOSE, null);
    }

    /**
     * A connected output reported by xrandr.
     */
    @Immutable
    public static final class Output {

        private final String name;
        private final int connectorId;
        private final byte[] edid;
        private final @Nullable DisplayMode mode;
        private final boolean primary;

        /**
         * Constructor for Output.
         *
         * @param name        the xrandr output name
         * @param connectorId the DRM connector ID, or {@code -1} if not available
         * @param edid        the EDID byte array
         * @param mode        the current mode, or {@code null} if the output is not enabled
         * @param primary     whether the output is marked primary
         */
        public Output(String name, int connectorId, byte[] edid, @Nullable DisplayMode mode, boolean primary) {
            this.name = name;
            this.connectorId = connectorId;
            this.edid = Arrays.copyOf(edid, edid.length);
            this.mode = mode;
            this.primary = primary;
        }

        /**
         * The xrandr output name, e.g. {@code HDMI-1}.
         *
         * @return the output name
         */
        public String getName() {
            return name;
        }

        /**
         * The DRM connector ID, published by Linux 6.5 and later.
         *
         * @return the connector ID, or {@code -1} if not available
         */
        public int getConnectorId() {
            return connectorId;
        }

        /**
         * The EDID the output reports.
         *
         * @return a copy of the EDID byte array
         */
        public byte[] getEdid() {
            return Arrays.copyOf(edid, edid.length);
        }

        /**
         * The mode the output is currently driven in.
         *
         * @return the current mode, or empty if the output is connected but not enabled
         */
        public Optional<DisplayMode> getMode() {
            return Optional.ofNullable(mode);
        }

        /**
         * Whether the output is marked primary.
         *
         * @return {@code true} if the output is marked primary
         */
        public boolean isPrimary() {
            return primary;
        }
    }

    /**
     * Accumulates one output's fields while its block of xrandr output is parsed.
     */
    @NotThreadSafe
    private static final class OutputBuilder {

        private final String name;
        private int connectorId = -1;
        private byte @Nullable [] edid;
        private final boolean primary;

        // From the header line; a width of 0 means the output has no geometry and is not enabled
        private int width;
        private int height;
        private int x;
        private int y;
        private int rotation;

        // From the mode block marked *current; unrotated
        private boolean inCurrentMode;
        private int modeWidth;
        private int modeHeight;
        private double refreshRate;

        private OutputBuilder(String[] header) {
            this.name = header[0];
            this.primary = header.length > 2 && "primary".equals(header[2]);
            // Tokens after "connected": an optional "primary", the geometry, the mode ID (verbose only), the rotation,
            // an optional reflection, then the parenthesized list of supported rotations
            for (int i = 2; i < header.length; i++) {
                String token = header[i];
                Matcher m = GEOMETRY.matcher(token);
                if (m.matches()) {
                    this.width = ParseUtil.parseIntOrDefault(m.group(1), 0);
                    this.height = ParseUtil.parseIntOrDefault(m.group(2), 0);
                    this.x = ParseUtil.parseIntOrDefault(m.group(3), 0);
                    this.y = ParseUtil.parseIntOrDefault(m.group(4), 0);
                } else if (token.startsWith("(") && !token.startsWith("(0x")) {
                    break;
                } else if (this.width > 0) {
                    this.rotation = parseRotation(token, this.rotation);
                }
            }
        }

        /**
         * Reads the lines of the mode list. A mode line (e.g. {@code 1920x1080 (0x46) 148.500MHz +HSync +VSync
         * *current +preferred}) starts each mode's block, followed by its {@code h:} and {@code v:} timing lines.
         *
         * @param trimmed a whitespace-trimmed line from within the output's block
         */
        private void parseModeLine(String trimmed) {
            if (trimmed.startsWith("h:")) {
                if (inCurrentMode) {
                    // h: width 1920 start 2008 end 2052 total 2200 skew 0 clock 67.50KHz
                    this.modeWidth = parseTimingValue(trimmed, "width");
                }
            } else if (trimmed.startsWith("v:")) {
                if (inCurrentMode) {
                    // v: height 1080 start 1084 end 1089 total 1125 clock 60.00Hz
                    this.modeHeight = parseTimingValue(trimmed, "height");
                    String last = trimmed.substring(trimmed.lastIndexOf(' ') + 1);
                    if (last.endsWith("Hz") && !last.endsWith("KHz") && !last.endsWith("MHz")) {
                        this.refreshRate = ParseUtil.parseDoubleOrDefault(last.substring(0, last.length() - 2), 0d);
                    }
                }
            } else if (trimmed.contains("MHz")) {
                this.inCurrentMode = trimmed.contains("*current");
            }
        }

        private Output build(byte[] edidBytes) {
            DisplayMode mode = null;
            if (width > 0 && height > 0) {
                int pixelWidth = width;
                int pixelHeight = height;
                if (modeWidth > 0 && modeHeight > 0) {
                    // The mode is unrotated; the header geometry is in the output's current orientation
                    boolean quarterTurn = rotation == 90 || rotation == 270;
                    pixelWidth = quarterTurn ? modeHeight : modeWidth;
                    pixelHeight = quarterTurn ? modeWidth : modeHeight;
                }
                mode = new DisplayModeImpl(x, y, width, height, pixelWidth, pixelHeight, refreshRate, rotation);
            }
            return new Output(name, connectorId, edidBytes, mode, primary);
        }

        private static int parseTimingValue(String line, String key) {
            String[] tokens = ParseUtil.whitespaces.split(line, -1);
            for (int i = 1; i < tokens.length - 1; i++) {
                if (key.equals(tokens[i])) {
                    return ParseUtil.parseIntOrDefault(tokens[i + 1], 0);
                }
            }
            return 0;
        }

        // RandR names rotations of the output's contents: "left" is a quarter turn counterclockwise
        private static int parseRotation(String token, int defaultRotation) {
            switch (token) {
                case "normal":
                    return 0;
                case "right":
                    return 90;
                case "inverted":
                    return 180;
                case "left":
                    return 270;
                default:
                    return defaultRotation;
            }
        }
    }
}
