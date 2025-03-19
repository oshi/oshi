/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.windows;

import oshi.driver.windows.registry.InstalledAppsData;
import oshi.software.os.ApplicationInfo;
import oshi.util.Constants;
import oshi.util.ParseUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WindowsInstalledApps {

    public static List<ApplicationInfo> queryInstalledApps() {
        List<ApplicationInfo> appInfoList = new ArrayList<>();

        List<Map<InstalledAppsData.AppInfo, String>> registryResults = InstalledAppsData.queryInstalledApps();

        for (Map<InstalledAppsData.AppInfo, String> app : registryResults) {
            String installDate = ParseUtil.getValueOrUnknown(app, InstalledAppsData.AppInfo.INSTALLDATE);
            long installDateEpoch = 0;
            if (!installDate.equals(Constants.UNKNOWN)) {
                installDateEpoch = ParseUtil.parseDateToEpoch(installDate, "yyyyMMdd");
            }
            //Additional info map
            Map<String, String> additionalInfo = new HashMap<>();
            additionalInfo.put("installLocation", ParseUtil.getValueOrUnknown(app, InstalledAppsData.AppInfo.INSTALLLOCATION));
            additionalInfo.put("installSource", ParseUtil.getValueOrUnknown(app, InstalledAppsData.AppInfo.INSTALLSOURCE));

            appInfoList.add(new ApplicationInfo(ParseUtil.getValueOrUnknown(app, InstalledAppsData.AppInfo.NAME),
                    ParseUtil.getValueOrUnknown(app, InstalledAppsData.AppInfo.VENDOR),
                    ParseUtil.getValueOrUnknown(app, InstalledAppsData.AppInfo.VERSION),
                    installDateEpoch,
                    additionalInfo));
        }

        return appInfoList;
    }
}
