/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.linux;

import oshi.software.os.AppInfo;

import java.util.HashMap;
import java.util.Map;

public class LinuxAppInfo implements AppInfo {

    private final String name;
    private final String version;
    private final String maintainer; // Used as vendor
    private final long installedSize;
    private final long installTimeEpoch; // Direct epoch from RPM/Debian
    private final Map<String, String> additionalInfo;

    public LinuxAppInfo(String name, String version, String architecture, long installedSize, long installTimeEpoch,
            String maintainer, String source, String homepage) {
        this.name = name;
        this.version = version;
        this.maintainer = maintainer;
        this.installedSize = installedSize;
        this.installTimeEpoch = installTimeEpoch;
        // Store additional fields in the map
        this.additionalInfo = new HashMap<>();
        additionalInfo.put("architecture", architecture);
        additionalInfo.put("source", source);
        additionalInfo.put("homepage", homepage);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getVendor() {
        return maintainer;
    }

    // Add getter for installed size
    public long getInstalledSize() {
        return installedSize;
    }

    @Override
    public long getLastModifiedEpoch() {
        return installTimeEpoch;
    }

    @Override
    public Map<String, String> getAdditionalInfo() {
        return additionalInfo;
    }

    // Add a method to get a specific additional info field
    public String getAdditionalInfoValue(String key) {
        return additionalInfo.get(key);
    }

    @Override
    public String toString() {
        return "LinuxAppInfo{" + "packageName=" + name + ", version=" + version + ", maintainer=" + maintainer
                + ", lastModified=" + installTimeEpoch + ", installedSize=" + installedSize + ", additionalInfo="
                + additionalInfo + '}';
    }
}
