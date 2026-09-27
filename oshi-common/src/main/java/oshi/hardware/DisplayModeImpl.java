/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import java.util.Locale;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.Immutable;

/**
 * The default {@link DisplayMode} implementation. This is not part of the OSHI public API and may change between minor
 * releases.
 */
@Immutable
public final class DisplayModeImpl implements DisplayMode {

    private final int width;
    private final int height;
    private final int pixelWidth;
    private final int pixelHeight;
    private final double refreshRate;
    private final int rotation;
    private final int x;
    private final int y;

    /**
     * Constructs a {@code DisplayModeImpl}. All sizes are in the display's current orientation.
     *
     * @param x           the x coordinate of the display's top-left corner on the desktop, in logical units
     * @param y           the y coordinate of the display's top-left corner on the desktop, in logical units
     * @param width       the logical width
     * @param height      the logical height
     * @param pixelWidth  the width in native pixels
     * @param pixelHeight the height in native pixels
     * @param refreshRate the refresh rate in hertz, or 0 if not known
     * @param rotation    the rotation in degrees clockwise; normalized to 0, 90, 180 or 270
     */
    public DisplayModeImpl(int x, int y, int width, int height, int pixelWidth, int pixelHeight, // NOSONAR java:S107
            double refreshRate, int rotation) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.pixelWidth = pixelWidth;
        this.pixelHeight = pixelHeight;
        this.refreshRate = refreshRate > 0 ? refreshRate : 0d;
        this.rotation = normalizeRotation(rotation);
    }

    /**
     * Normalizes a rotation to the nearest quarter turn in the range 0 to 270.
     *
     * @param degrees a rotation in degrees clockwise, possibly negative or a multiple of a full turn
     * @return 0, 90, 180 or 270
     */
    static int normalizeRotation(int degrees) {
        int quarterTurns = Math.round(degrees / 90f) % 4;
        return (quarterTurns < 0 ? quarterTurns + 4 : quarterTurns) * 90;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getPixelWidth() {
        return pixelWidth;
    }

    @Override
    public int getPixelHeight() {
        return pixelHeight;
    }

    @Override
    public double getRefreshRate() {
        return refreshRate;
    }

    @Override
    public int getRotation() {
        return rotation;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DisplayModeImpl)) {
            return false;
        }
        DisplayModeImpl other = (DisplayModeImpl) obj;
        return width == other.width && height == other.height && pixelWidth == other.pixelWidth
                && pixelHeight == other.pixelHeight && Double.compare(refreshRate, other.refreshRate) == 0
                && rotation == other.rotation && x == other.x && y == other.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(width, height, pixelWidth, pixelHeight, refreshRate, rotation, x, y);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(width).append('x').append(height);
        if (pixelWidth != width || pixelHeight != height) {
            sb.append(" (").append(pixelWidth).append('x').append(pixelHeight).append(" pixels)");
        }
        if (refreshRate > 0) {
            sb.append(String.format(Locale.ROOT, " @ %.2f Hz", refreshRate));
        }
        if (rotation != 0) {
            sb.append(", rotated ").append(rotation).append('°');
        }
        sb.append(" at (").append(x).append(',').append(y).append(')');
        return sb.toString();
    }
}
