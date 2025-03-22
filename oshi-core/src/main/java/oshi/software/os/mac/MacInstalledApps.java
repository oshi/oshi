/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.mac;

import oshi.software.os.ApplicationInfo;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public final class MacInstalledApps {

    private static final String COLON = ":";

    private MacInstalledApps() {
    }

    public static List<ApplicationInfo> queryInstalledApps() {
        List<String> output = ExecutingCommand.runNative("system_profiler SPApplicationsDataType");
        return parseMacAppInfo(output);
    }

    private static List<ApplicationInfo> parseMacAppInfo(List<String> lines) {
        List<ApplicationInfo> appInfoList = new ArrayList<>();
        String appName = null;
        Map<String, String> appDetails = null;
        boolean collectingAppDetails = false;

        for (String line : lines) {
            line = line.trim();

            // Check for app name, ends with ":"
            if (line.endsWith(COLON)) {
                // When app and appDetails are not empty then we reached the next app, add it to the list
                if (appName != null && !appDetails.isEmpty()) {
                    appInfoList.add(createAppInfo(appName, appDetails));
                }

                // store app name and proceed with collecting app details
                appName = line.substring(0, line.length() - 1);
                appDetails = new HashMap<>();
                collectingAppDetails = true;
                continue;
            }

            // Process app details
            if (collectingAppDetails && line.contains(COLON)) {
                int colonIndex = line.indexOf(COLON);
                String key = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();
                appDetails.put(key, value);
            }
        }

        return appInfoList;
    }

    private static ApplicationInfo createAppInfo(String name, Map<String, String> details) {
        String obtainedFrom = ParseUtil.getValueOrUnknown(details, "Obtained from");
        String signedBy = ParseUtil.getValueOrUnknown(details, "Signed by");
        String vendor = (obtainedFrom.equals("Identified Developer")) ? signedBy : obtainedFrom;

        String lastModified = details.getOrDefault("Last Modified", Constants.UNKNOWN);
        long lastModifiedEpoch = ParseUtil.parseDateToEpoch(lastModified, "dd/MM/yy, HH:mm");

        // Additional info map
        Map<String, String> additionalInfo = new HashMap<>();
        additionalInfo.put("Kind", ParseUtil.getValueOrUnknown(details, "Kind"));
        additionalInfo.put("Location", ParseUtil.getValueOrUnknown(details, "Location"));
        additionalInfo.put("Get Info String", ParseUtil.getValueOrUnknown(details, "Get Info String"));

        return new ApplicationInfo(name, ParseUtil.getValueOrUnknown(details, "Version"), vendor, lastModifiedEpoch,
                additionalInfo);
    }
}
