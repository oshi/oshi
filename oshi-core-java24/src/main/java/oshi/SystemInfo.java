/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi;

public class SystemInfo {

    private static final PlatformEnum CURRENT_PLATFORM;

    static {
        String osName = System.getProperty("os.name");
        CURRENT_PLATFORM = switch (osName) {
        case String name when name.startsWith("Linux") -> PlatformEnum.LINUX;
        case String name when name.startsWith("Mac") || name.startsWith("Darwin") -> PlatformEnum.MACOS;
        case String name when name.startsWith("Windows") -> PlatformEnum.WINDOWS;
        default -> PlatformEnum.UNSUPPORTED;
        };
    }

    /**
     * Constructs a new SystemInfo instance.
     */
    public SystemInfo() {
        // Empty constructor for API consistency
    }

    /**
     * Gets the {@link PlatformEnum} value representing this system.
     *
     * @return Returns the current platform
     */
    public static PlatformEnum getCurrentPlatform() {
        return CURRENT_PLATFORM;
    }
}
