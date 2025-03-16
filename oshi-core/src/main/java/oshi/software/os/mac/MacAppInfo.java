/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.mac;

import oshi.software.os.AppInfo;
import oshi.util.ParseUtil;

import java.util.HashMap;
import java.util.Map;

public class MacAppInfo implements AppInfo {

    private final String name;
    private final String version;
    private final String vendor;
    private final long lastModifiedEpoch; // epoch-based
    private final Map<String, String> additionalInfo;

    public MacAppInfo(String name, String version, String vendor, String lastModified, String kind, String location,
            String getInfoString) {
        this.name = name;
        this.version = version;
        this.vendor = vendor;
        this.lastModifiedEpoch = convertToEpoch(lastModified);
        // Store additional fields in the map
        this.additionalInfo = new HashMap<>();
        additionalInfo.put("kind", kind);
        additionalInfo.put("location", location);
        additionalInfo.put("getInfoString", getInfoString);
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
        return vendor;
    }

    @Override
    public long getLastModifiedEpoch() {
        return lastModifiedEpoch;
    }

    @Override
    public Map<String, String> getAdditionalInfo() {
        return additionalInfo;
    }

    // Add a method to get a specific additional info field
    public String getAdditionalInfoValue(String key) {
        return additionalInfo.get(key);
    }

    private long convertToEpoch(String date) {
        // Mac format is DD/MM/YY, HH:mm
        return ParseUtil.parseMacDateToEpoch(date);
    }

    @Override
    public String toString() {
        return "MacAppInfo{" + "appName=" + name + ", version=" + version + ", vendor=" + vendor + ", lastModified="
                + lastModifiedEpoch + ", additionalInfo=" + additionalInfo + '}';
    }
}
