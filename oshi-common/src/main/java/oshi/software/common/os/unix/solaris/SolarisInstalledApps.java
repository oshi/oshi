/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.solaris;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.os.ApplicationInfo;
import oshi.util.ExecutingCommand;

/**
 * Queries the packages installed on Solaris and illumos from the Image Packaging System, {@code pkg}.
 */
@ThreadSafe
public final class SolarisInstalledApps {

    /**
     * A {@code pkg list -Hv} line: the FMRI {@code pkg://publisher/name@version:timestamp}, then the state flags, as in
     * {@code pkg://solaris/archiver/gnu-tar@1.35-11.4.95.0.1.227.0:20260810T173115Z       i--}.
     */
    private static final Pattern FMRI = Pattern.compile("pkg://([^/]+)/(\\S+)@([^:\\s]+):(\\d{8}T\\d{6}Z)\\s+(\\S+)");
    /** The FMRI's timestamp, in ISO 8601 basic format and UTC. */
    private static final DateTimeFormatter FMRI_TIME = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX", Locale.ROOT);

    private SolarisInstalledApps() {
    }

    /**
     * Queries {@code pkg list} for the installed packages, which needs no privilege.
     *
     * @return the installed applications
     */
    public static List<ApplicationInfo> queryInstalledApps() {
        return parsePkgList(ExecutingCommand.runNative("pkg list -Hv"));
    }

    /**
     * Parses the output of {@code pkg list -Hv}. Package-private for testing.
     * <p>
     * The publisher, such as {@code solaris} or {@code openindiana.org}, is the vendor. IPS records no install time
     * that a listing can show, so the timestamp is the one in the FMRI, which is when that version of the package was
     * published.
     *
     * @param lines the lines emitted by {@code pkg list -Hv}
     * @return the installed applications
     */
    static List<ApplicationInfo> parsePkgList(List<String> lines) {
        List<ApplicationInfo> apps = new ArrayList<>();
        for (String line : lines) {
            Matcher m = FMRI.matcher(line.trim());
            if (!m.matches()) {
                continue;
            }
            Map<String, String> additionalInfo = new LinkedHashMap<>();
            additionalInfo.put("fmri", line.trim().split("\\s+", 2)[0]);
            additionalInfo.put("state", m.group(5));
            apps.add(
                    new ApplicationInfo(m.group(2), m.group(3), m.group(1), parseFmriTime(m.group(4)), additionalInfo));
        }
        return apps;
    }

    private static long parseFmriTime(String timestamp) {
        try {
            return OffsetDateTime.parse(timestamp, FMRI_TIME).toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            return 0L;
        }
    }
}
