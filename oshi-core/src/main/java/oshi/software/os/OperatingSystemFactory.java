/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os;

public interface OperatingSystemFactory {
    /**
     * Returns the minimum supported Java version (e.g., 8, 11, 24).
     */
    int getMinimumSupportedJavaVersion();
}
