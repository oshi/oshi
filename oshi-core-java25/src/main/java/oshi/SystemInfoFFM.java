/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi;

/**
 * System information entry point for the FFM module.
 *
 * @deprecated Use {@link oshi.ffm.SystemInfo} instead.
 */
@Deprecated
public class SystemInfoFFM extends oshi.ffm.SystemInfo {

    /**
     * Create a new instance of {@link SystemInfoFFM}.
     *
     * @deprecated Use {@link oshi.ffm.SystemInfo} instead.
     */
    @Deprecated
    public SystemInfoFFM() {
        super();
    }

    /**
     * Gets the {@link PlatformEnumFFM} value representing this system.
     *
     * @return Returns the current platform
     * @deprecated Use {@link PlatformEnum#getCurrentPlatform()} instead.
     */
    @Deprecated
    public static PlatformEnumFFM getCurrentPlatform() {
        return PlatformEnumFFM.getCurrentPlatform();
    }
}
