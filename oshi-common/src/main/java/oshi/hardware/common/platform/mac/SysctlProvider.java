/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

/**
 * Abstracts sysctl operations so that Mac base classes can query sysctl values without depending on JNA or FFM
 * directly.
 */
public interface SysctlProvider {

    /**
     * Gets a sysctl int value.
     *
     * @param name         the sysctl name
     * @param defaultValue the default value if not found
     * @return the sysctl value
     */
    int sysctlInt(String name, int defaultValue);

    /**
     * Gets a sysctl int value without logging warnings on failure.
     *
     * @param name         the sysctl name
     * @param defaultValue the default value if not found
     * @return the sysctl value
     */
    int sysctlIntNoWarn(String name, int defaultValue);

    /**
     * Gets a sysctl long value.
     *
     * @param name         the sysctl name
     * @param defaultValue the default value if not found
     * @return the sysctl value
     */
    long sysctlLong(String name, long defaultValue);

    /**
     * Gets a sysctl string value.
     *
     * @param name         the sysctl name
     * @param defaultValue the default value if not found
     * @return the sysctl value
     */
    String sysctlString(String name, String defaultValue);

    /**
     * Gets a sysctl string value without logging warnings on failure.
     *
     * @param name         the sysctl name
     * @param defaultValue the default value if not found
     * @return the sysctl value
     */
    String sysctlStringNoWarn(String name, String defaultValue);
}
