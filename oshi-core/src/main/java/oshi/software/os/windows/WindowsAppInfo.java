/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.windows;

import oshi.software.os.AppInfo;
import oshi.util.ParseUtil;

import java.util.HashMap;
import java.util.Map;

public class WindowsAppInfo implements AppInfo {

    private final String name;
    private final String version;
    private final String vendor;
    private final long installDateEpoch; // Epoch-based
    private final Map<String, String> additionalInfo;

    public WindowsAppInfo(String name, String version, String vendor, String installDate, String installLocation,
            String installSource) {
        this.name = name;
        this.version = version;
        this.vendor = vendor;
        this.installDateEpoch = convertToEpoch(installDate);
        // Store additional fields in the map
        this.additionalInfo = new HashMap<>();
        additionalInfo.put("installLocation", installLocation);
        additionalInfo.put("installSource", installSource);
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
        return installDateEpoch;
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
        // Windows format is YYYYMMDD
        return ParseUtil.parseWindowsDateToEpoch(date);
    }

    @Override
    public String toString() {
        return "WindowsAppInfo{" + "name=" + name + ", version=" + version + ", vendor=" + vendor + ", installDate="
                + installDateEpoch + ", additionalInfo=" + additionalInfo + '}';
    }
}
