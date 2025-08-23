/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi;

/**
 * An enumeration of supported operating systems.
 */
public enum PlatformEnumFFM {
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

    PlatformEnumFFM(String name) {
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
