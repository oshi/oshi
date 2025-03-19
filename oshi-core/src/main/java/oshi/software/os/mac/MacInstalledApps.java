/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.mac;

import oshi.software.os.ApplicationInfo;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class MacInstalledApps {

    private final String colon = ":";

    public List<ApplicationInfo> getInstalledApps() {
        List<String> output = ExecutingCommand.runNative("system_profiler SPApplicationsDataType");
        return parseMacAppInfo(output);
    }

    private List<ApplicationInfo> parseMacAppInfo(List<String> lines) {
        List<ApplicationInfo> appInfoList = new ArrayList<>();
        String appName = null;
        Map<String, String> appDetails = null;
        boolean collectingAppDetails = false;

        for (String line : lines) {
            line = line.trim();

            // Check for app name, ends with ":"
            if (line.endsWith(colon)) {
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
            if (collectingAppDetails && line.contains(colon)) {
                int colonIndex = line.indexOf(colon);
                String key = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();
                appDetails.put(key, value);
            }
        }

        return appInfoList;
    }

    private MacApplicationInfo createAppInfo(String name, Map<String, String> details) {
        String obtainedFrom = ParseUtil.getValueOrUnknown(details, "Obtained from");
        String signedBy = ParseUtil.getValueOrUnknown(details, "Signed by");
        String vendor = (obtainedFrom.equals("Identified Developer")) ? signedBy : obtainedFrom;
        return new MacApplicationInfo(name, ParseUtil.getValueOrUnknown(details, "Version"), vendor,
                ParseUtil.getValueOrUnknown(details, "Last Modified"), ParseUtil.getValueOrUnknown(details, "Kind"),
                ParseUtil.getValueOrUnknown(details, "Location"),
                ParseUtil.getValueOrUnknown(details, "Get Info String"));
    }
}
