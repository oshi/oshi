/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os;

import java.util.Map;

/**
 * Represents common information about an installed application across different operating systems.
 * This interface provides standardized access to essential application details while allowing
 * flexibility for OS-specific fields via an additional information map.
 */
public interface ApplicationInfo {

    /**
     * Gets the name of the installed application.
     *
     * @return The application name, or an empty string if not available.
     */
    String getName();

    /**
     * Gets the version of the installed application.
     *
     * @return The application version, or an empty string if not available.
     */
    String getVersion();

    /**
     * Gets the vendor or publisher of the installed application.
     *
     * @return The vendor name, or an empty string if not available.
     */
    String getVendor();

    /**
     * Gets the last modified or installation time of the application.
     * The timestamp is represented in milliseconds since the Unix epoch (January 1, 1970, UTC).
     * <p>
     * - On Windows, this corresponds to the application's install date.
     * - On Linux, it represents the package's installation or last modified time.
     * - On macOS, it reflects the last modification timestamp of the application bundle.
     * </p>
     *
     * @return The last modified time in epoch milliseconds, or {@code 0} if unavailable.
     */
    long getLastModifiedTime();

    /**
     * Gets additional application details that are OS-specific and not covered by the main fields.
     * This map may include attributes like installation location, source, architecture, or other metadata.
     *
     * @return A map containing optional key-value pairs of application details.
     */
    Map<String, String> getAdditionalInfo(); // For optional fields
}
