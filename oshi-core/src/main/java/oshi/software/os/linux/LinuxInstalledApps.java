/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.linux;

import oshi.software.os.ApplicationInfo;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LinuxInstalledApps {

    /**
     * Retrieves the list of installed applications on a Linux system.
     * This method determines the appropriate package manager (DPKG or RPM)
     * and parses the installed application details.
     *
     * @return A list of {@link ApplicationInfo} objects representing installed applications.
     */
    public static List<ApplicationInfo> queryInstalledApps() {
        List<String> output = fetchInstalledApps();
        return parseLinuxAppInfo(output);
    }

    /**
     * Fetches installed application details from the system using the appropriate package manager.
     * It first checks for the availability of 'dpkg' (Debian-based) or 'rpm' (RedHat-based).
     * If neither is found, it returns an empty list.
     *
     * @return A list of strings representing installed applications, formatted by the package manager.
     */
    private static List<String> fetchInstalledApps() {
        String command;

        if(isPackageManagerAvailable("dpkg")) {
            command = "dpkg-query -W -f='${Package}|${Version}|${Architecture}|${Installed-Size}|${db-fsys:Last-Modified}|${Maintainer}|${Source}|${Homepage}\\n'";
        } else if (isPackageManagerAvailable("rpm")) {
            command = "rpm -qa --queryformat '%{NAME}|%{VERSION}-%{RELEASE}|%{ARCH}|%{SIZE}|%{INSTALLTIME}|%{PACKAGER}|%{SOURCERPM}|%{URL}\\n'";
        } else {
            return Collections.emptyList();
        }

        return ExecutingCommand.runNative(command);
    }

    private static boolean isPackageManagerAvailable(String packageManager) {
        List<String> result = ExecutingCommand.runNative(packageManager + " --version");
        // If the command executes fine the result is non-empty else empty
        return !result.isEmpty();
    }

    private static List<ApplicationInfo> parseLinuxAppInfo(List<String> output) {
        List<ApplicationInfo> appInfoList = new ArrayList<>();

        for (String line : output) {
            // split by the pipe character
            String[] parts = line.split("\\|", -1); // -1 to keep empty fields

            // Check if we have all 8 fields
            if (parts.length >= 8) {
                // Additional info map
                Map<String, String> additionalInfo = new HashMap<>();
                additionalInfo.put("architecture", parts[2].isEmpty() ? Constants.UNKNOWN : parts[2]);
                additionalInfo.put("installedSize", String.valueOf(ParseUtil.parseLongOrDefault(parts[3], 0L)));
                additionalInfo.put("source", parts[6].isEmpty() ? Constants.UNKNOWN : parts[6]);
                additionalInfo.put("homepage", parts[7].isEmpty() ? Constants.UNKNOWN : parts[7]);
                ApplicationInfo app = new ApplicationInfo(parts[0].isEmpty() ? Constants.UNKNOWN : parts[0], // Package Name
                        parts[1].isEmpty() ? Constants.UNKNOWN : parts[1], // Version
                        parts[5].isEmpty() ? Constants.UNKNOWN : parts[5], // Vendor
                        ParseUtil.parseLongOrDefault(parts[4], 0L), // Date Epoch
                        additionalInfo
                );
                appInfoList.add(app);
            }
        }

        return appInfoList;
    }
}
