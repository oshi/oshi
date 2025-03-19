/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents common information about an installed application across different operating systems.
 * This class provides standardized access to essential application details while allowing
 * flexibility for OS-specific fields via an additional information map.
 */
public class ApplicationInfo {

    private final String name;
    private final String version;
    private final String vendor;
    private final long lastModifiedTime;
    private final Map<String, String> additionalInfo;

    public ApplicationInfo(String name, String version, String vendor, long lastModifiedTime, Map<String, String> additionalInfo) {
        this.name = name;
        this.version = version;
        this.vendor = vendor;
        this.lastModifiedTime = lastModifiedTime;
        this.additionalInfo = additionalInfo != null ? new HashMap<>(additionalInfo) : new HashMap<>();
    }

    /**
     * Gets the name of the installed application.
     *
     * @return The application name, or an empty string if not available.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the version of the installed application.
     *
     * @return The application version, or an empty string if not available.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Gets the vendor or publisher of the installed application.
     *
     * @return The vendor name, or an empty string if not available.
     */
    public String getVendor() {
        return vendor;
    }

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
    public long getLastModifiedTime() {
        return lastModifiedTime;
    }

    /**
     * Gets additional application details that are OS-specific and not covered by the main fields.
     * This map may include attributes like installation location, source, architecture, or other metadata.
     *
     * @return A map containing optional key-value pairs of application details.
     */
    public Map<String, String> getAdditionalInfo() {
        return additionalInfo;
    }

    @Override
    public String toString() {
        return "AppInfo{" + "name=" + name + ", version=" + version + ", vendor=" + vendor
            + ", lastModified=" + lastModifiedTime + ", additionalInfo=" + additionalInfo + '}';
    }
}
