/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.windows;

import oshi.driver.windows.registry.InstalledAppsData;
import oshi.software.os.AbstractInstalledApps;
import oshi.software.os.AppInfo;
import oshi.util.ParseUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WindowsInstalledApps extends AbstractInstalledApps {

    @Override
    public List<AppInfo> getInstalledApps() {
        List<AppInfo> appInfoList = new ArrayList<>();

        List<Map<InstalledAppsData.AppInfo, String>> registryResults = InstalledAppsData.queryInstalledApps();

        for (Map<InstalledAppsData.AppInfo, String> app : registryResults) {
            appInfoList.add(new WindowsAppInfo(ParseUtil.getValueOrUnknown(app, InstalledAppsData.AppInfo.NAME),
                    ParseUtil.getValueOrUnknown(app, InstalledAppsData.AppInfo.VENDOR),
                    ParseUtil.getValueOrUnknown(app, InstalledAppsData.AppInfo.VERSION),
                    ParseUtil.getValueOrUnknown(app, InstalledAppsData.AppInfo.INSTALLDATE),
                    ParseUtil.getValueOrUnknown(app, InstalledAppsData.AppInfo.INSTALLLOCATION),
                    ParseUtil.getValueOrUnknown(app, InstalledAppsData.AppInfo.INSTALLSOURCE)));
        }

        return appInfoList;
    }
}
