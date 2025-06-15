/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.aix;

import oshi.software.os.ApplicationInfo;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

public final class AixInstalledApps {

    private static final Pattern COLON_PATTERN = Pattern.compile(":");

    private AixInstalledApps() {
    }

    public static List<ApplicationInfo> queryInstalledApps() {
        // https://www.ibm.com/docs/en/aix/7.1.0?topic=l-lslpp-command
        List<String> output = ExecutingCommand.runNative("lslpp -Lc");
        return parseAixAppInfo(output);
    }

    private static List<ApplicationInfo> parseAixAppInfo(List<String> lines) {
        Set<ApplicationInfo> appInfoSet = new LinkedHashSet<>();
        String architecture = System.getProperty("os.arch");
        boolean isFirstLine = true;
        for (String line : lines) {
            if (isFirstLine) {
                isFirstLine = false;
                continue; // Skip the first line as it consists column names
            }
            /*
             * Sample output: (1) devices.chrp.IBM.lhca:devices.chrp.IBM.lhca.rte:7.1.5.30: : :C:F:Infiniband Logical
             * HCA Runtime Environment: : : : : : :0:0:/:1837 (2) bash:bash-5.0.18-1:5.0.18-1: : :C:R:The GNU Bourne
             * Again shell (bash) version 5.0.18: :/bin/rpm -e bash: : : : :0: :(none):Fri Sep 18 15:53:11 2020
             */
            // split by the colon character
            String[] parts = COLON_PATTERN.split(line, -1); // -1 to keep empty fields
            String name = ParseUtil.getStringValueOrUnknown(parts[0]);
            if (name.equals(Constants.UNKNOWN)) {
                continue;
            }
            String version = ParseUtil.getStringValueOrUnknown(parts[2]);
            String vendor = Constants.UNKNOWN; // lslpp command does not provide vendor info, hence, assigning as
            // unknown
            // Build Date is of two formats YYWW and EEE MMM dd HH:mm:ss yyyy
            String buildDate = ParseUtil.getStringValueOrUnknown(parts[17]);
            long timestamp = 0;
            if (!buildDate.equals(Constants.UNKNOWN)) {
                if (buildDate.matches("\\d{4}")) {
                    // Convert to ISO week date string (e.g., 1125 -> 2011-W25-2 for Monday)
                    String isoWeekString = "20" + buildDate.substring(0, 2) + "-W" + buildDate.substring(2) + "-2";
                    timestamp = ParseUtil.parseDateToEpoch(isoWeekString, "YYYY-'W'ww-e");
                } else {
                    timestamp = ParseUtil.parseDateToEpoch(buildDate, "EEE MMM dd HH:mm:ss yyyy");
                }
            }
            String description = ParseUtil.getStringValueOrUnknown(parts[7].trim());
            String installPath = ParseUtil.getStringValueOrUnknown(parts[16].trim());
            Map<String, String> additionalInfo = new LinkedHashMap<>();
            additionalInfo.put("architecture", architecture);
            additionalInfo.put("description", description);
            additionalInfo.put("installPath", installPath);
            ApplicationInfo app = new ApplicationInfo(name, version, vendor, timestamp, additionalInfo);
            appInfoSet.add(app);
        }

        return new ArrayList<>(appInfoSet);
    }
}
