/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.freebsd;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.os.ApplicationInfo;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Queries the packages installed on FreeBSD and DragonFly BSD from {@code pkg}, the package manager both use.
 */
@ThreadSafe
public final class FreeBsdInstalledApps {

    /**
     * Name, version, install time in seconds, origin, flat size in bytes, ABI, homepage and maintainer, then the
     * comment last because it is free text that may itself contain the separator.
     */
    private static final String PKG_QUERY = "pkg query %n|%v|%t|%o|%sb|%q|%w|%m|%c";
    private static final int FIELDS = 9;

    private FreeBsdInstalledApps() {
    }

    /**
     * Queries {@code pkg} for the installed packages. Reading the package database needs no privilege.
     *
     * @return the installed applications, or an empty list if {@code pkg} is not installed
     */
    public static List<ApplicationInfo> queryInstalledApps() {
        return parsePkgQuery(ExecutingCommand.runNative(PKG_QUERY));
    }

    /**
     * Parses the output of {@code pkg query} in the format {@link #PKG_QUERY} requests. Package-private for testing.
     * <p>
     * The maintainer is the vendor, as a Debian package's maintainer is on Linux, and the install time is the
     * timestamp.
     *
     * @param lines the lines emitted by {@code pkg query}
     * @return the installed applications
     */
    static List<ApplicationInfo> parsePkgQuery(List<String> lines) {
        List<ApplicationInfo> apps = new ArrayList<>();
        for (String line : lines) {
            String[] parts = line.split("\\|", FIELDS);
            if (parts.length < FIELDS || parts[0].isEmpty()) {
                continue;
            }
            Map<String, String> additionalInfo = new LinkedHashMap<>();
            additionalInfo.put("architecture", ParseUtil.getStringValueOrUnknown(parts[5]));
            additionalInfo.put("installedSize", String.valueOf(ParseUtil.parseLongOrDefault(parts[4], 0L)));
            additionalInfo.put("source", ParseUtil.getStringValueOrUnknown(parts[3]));
            // pkg prints UNKNOWN for a port that names no homepage
            additionalInfo.put("homepage", ParseUtil.getStringValueOrUnknown(parts[6]));
            additionalInfo.put("description", ParseUtil.getStringValueOrUnknown(parts[8]));
            apps.add(new ApplicationInfo(parts[0], parts[1], parts[7],
                    ParseUtil.parseLongOrDefault(parts[2], 0L) * 1000L, additionalInfo));
        }
        return apps;
    }
}
