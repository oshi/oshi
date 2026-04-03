/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi;

/**
 * An enumeration of supported operating systems for the FFM module.
 *
 * @deprecated Use {@link PlatformEnum} directly. {@link PlatformEnum#getCurrentPlatform()} returns the current
 *             platform; FFM support is available for {@link PlatformEnum#MACOS}, {@link PlatformEnum#LINUX}, and
 *             {@link PlatformEnum#WINDOWS}.
 */
@Deprecated
public enum PlatformEnumFFM {
    /**
     * macOS
     */
    MACOS,
    /**
     * A flavor of Linux
     */
    LINUX,
    /**
     * Microsoft Windows
     */
    WINDOWS,
    /**
     * Unsupported OS
     */
    UNSUPPORTED;

    private static final PlatformEnumFFM CURRENT_PLATFORM;

    static {
        switch (PlatformEnum.getCurrentPlatform()) {
            case MACOS:
                CURRENT_PLATFORM = MACOS;
                break;
            case LINUX:
                CURRENT_PLATFORM = LINUX;
                break;
            case WINDOWS:
                CURRENT_PLATFORM = WINDOWS;
                break;
            default:
                CURRENT_PLATFORM = UNSUPPORTED;
        }
    }

    /**
     * Gets the name of the platform.
     *
     * @return the name of the platform
     * @deprecated Use {@link PlatformEnum#getCurrentPlatform()} and call {@link PlatformEnum#getName()} on the result.
     */
    @Deprecated
    public String getName() {
        return PlatformEnum.getCurrentPlatform().getName();
    }

    /**
     * Gets the current platform, detected from system properties.
     *
     * @return the current platform
     * @deprecated Use {@link PlatformEnum#getCurrentPlatform()} instead.
     */
    @Deprecated
    public static PlatformEnumFFM getCurrentPlatform() {
        return CURRENT_PLATFORM;
    }
}
