/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.mac;

import static oshi.jna.platform.mac.CoreFoundation.CFDateFormatterStyle.kCFDateFormatterShortStyle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.jna.platform.mac.CoreFoundation.CFStringRef;
import com.sun.jna.platform.mac.CoreFoundation.CFIndex;

import oshi.jna.platform.mac.CoreFoundation;
import oshi.jna.platform.mac.CoreFoundation.CFDateFormatter;
import oshi.jna.platform.mac.CoreFoundation.CFDateFormatterStyle;
import oshi.jna.platform.mac.CoreFoundation.CFLocale;
import oshi.software.os.ApplicationInfo;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

public final class MacInstalledApps {

    private static final String COLON = ":";
    private static final CoreFoundation CF = CoreFoundation.INSTANCE;

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
        String dateFormat = getLocaleDateTimeFormat(kCFDateFormatterShortStyle);

        for (String line : lines) {
            line = line.trim();

            // Check for app name, ends with ":"
            if (line.endsWith(COLON)) {
                // When app and appDetails are not empty then we reached the next app, add it to the list
                if (appName != null && !appDetails.isEmpty()) {
                    appInfoList.add(createAppInfo(appName, appDetails, dateFormat));
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

    private static ApplicationInfo createAppInfo(String name, Map<String, String> details, String dateFormat) {
        String obtainedFrom = ParseUtil.getValueOrUnknown(details, "Obtained from");
        String signedBy = ParseUtil.getValueOrUnknown(details, "Signed by");
        String vendor = (obtainedFrom.equals("Identified Developer")) ? signedBy : obtainedFrom;

        String lastModified = details.getOrDefault("Last Modified", Constants.UNKNOWN);
        long lastModifiedEpoch = ParseUtil.parseDateToEpoch(lastModified, dateFormat);

        // Additional info map
        Map<String, String> additionalInfo = new HashMap<>();
        additionalInfo.put("Kind", ParseUtil.getValueOrUnknown(details, "Kind"));
        additionalInfo.put("Location", ParseUtil.getValueOrUnknown(details, "Location"));
        additionalInfo.put("Get Info String", ParseUtil.getValueOrUnknown(details, "Get Info String"));

        return new ApplicationInfo(name, ParseUtil.getValueOrUnknown(details, "Version"), vendor, lastModifiedEpoch,
                additionalInfo);
    }

    private static String getLocaleDateTimeFormat(CFDateFormatterStyle style) {
        CFIndex styleIndex = style.index();
        CFLocale locale = CF.CFLocaleCopyCurrent();
        try {
            CFDateFormatter formatter = CF.CFDateFormatterCreate(null, locale, styleIndex, styleIndex);
            if (formatter == null) {
                return "";
            }
            try {
                CFStringRef format = CF.CFDateFormatterGetFormat(formatter);
                return (format == null) ? "" : format.stringValue();
            } finally {
                CF.CFRelease(formatter);
            }
        } finally {
            CF.CFRelease(locale);
        }
    }
}
