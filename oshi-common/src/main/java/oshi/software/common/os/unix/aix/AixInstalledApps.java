/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.aix;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import oshi.software.os.ApplicationInfo;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Utility to query installed applications on AIX from {@code lslpp} output.
 */
public final class AixInstalledApps {

    /** A four-digit YYWW build date, as opposed to the full {@code EEE MMM dd HH:mm:ss yyyy} form. */
    private static final Pattern YYWW_DATE = Pattern.compile("\\d{4}");

    private AixInstalledApps() {
    }

    /**
     * Queries {@code lslpp} for the installed filesets.
     *
     * @return the installed applications
     */
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
            // Not a precompiled Pattern: String.split takes a regex-free fast path for a single
            // non-metacharacter separator, which measures about 3x faster than Pattern.split
            String[] parts = line.split(":", -1); // -1 to keep empty fields
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
                if (YYWW_DATE.matcher(buildDate).matches()) {
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
