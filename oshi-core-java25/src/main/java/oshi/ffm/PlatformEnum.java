/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm;

/**
 * An enumeration of supported operating systems.
 */
public enum PlatformEnum {
    /**
     * macOS
     */
    MACOS("macOS"),
    /**
     * A flavor of Linux
     */
    LINUX("Linux"),
    /**
     * Microsoft Windows
     */
    WINDOWS("Windows"),
    /**
     * Unsupported OS
     */
    UNSUPPORTED("Unsupported Operating System");

    private final String name;

    PlatformEnum(String name) {
        this.name = name;
    }

    /**
     * Gets the name of the platform
     *
     * @return the name of the platform
     */
    public String getName() {
        return this.name;
    }
}
