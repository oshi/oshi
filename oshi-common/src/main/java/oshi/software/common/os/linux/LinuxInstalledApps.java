/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.linux;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import oshi.software.os.ApplicationInfo;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;

/**
 * Utility class for querying installed applications on Linux.
 */
public final class LinuxInstalledApps {

    private static final Map<String, String> PACKAGE_MANAGER_COMMANDS = initializePackageManagerCommands();

    /** Alpine's apk database of installed packages. */
    private static final String APK_INSTALLED = "/lib/apk/db/installed";
    /** Arch's pacman database, one directory of metadata files per installed package. */
    private static final String PACMAN_LOCAL = "/var/lib/pacman/local";

    private LinuxInstalledApps() {
    }

    private static Map<String, String> initializePackageManagerCommands() {
        Map<String, String> commands = new HashMap<>();

        if (isPackageManagerAvailable("dpkg")) {
            commands.put("dpkg",
                    "dpkg-query -W -f=${Package}|${Version}|${Architecture}|${Installed-Size}|${db-fsys:Last-Modified}|${Maintainer}|${Source}|${Homepage}\\n");
        } else if (isPackageManagerAvailable("rpm")) {
            commands.put("rpm",
                    "rpm -qa --queryformat %{NAME}|%{VERSION}-%{RELEASE}|%{ARCH}|%{SIZE}|%{INSTALLTIME}|%{PACKAGER}|%{SOURCERPM}|%{URL}\\n");
        }

        return commands;
    }

    /**
     * Retrieves the list of installed applications on a Linux system. This method determines the appropriate package
     * manager and parses the installed application details.
     *
     * @return A list of {@link ApplicationInfo} objects representing installed applications.
     */
    public static List<ApplicationInfo> queryInstalledApps() {
        if (!PACKAGE_MANAGER_COMMANDS.isEmpty()) {
            return parseLinuxAppInfo(fetchInstalledApps());
        }
        return queryPackageDatabases(APK_INSTALLED, PACMAN_LOCAL);
    }

    /**
     * Reads the installed packages from the apk or pacman database, which both keep as plain text, so no command is
     * needed. Package-private for testing.
     *
     * @param apkInstalled the path of apk's {@code installed} database file
     * @param pacmanLocal  the path of pacman's local database directory, holding one directory per package
     * @return the installed packages, or an empty list if neither database exists
     */
    static List<ApplicationInfo> queryPackageDatabases(String apkInstalled, String pacmanLocal) {
        if (new File(apkInstalled).isFile()) {
            return parseApkInstalled(FileUtil.readFile(apkInstalled, false));
        }
        File[] pacmanPackages = new File(pacmanLocal).listFiles(File::isDirectory);
        if (pacmanPackages != null) {
            List<ApplicationInfo> apps = new ArrayList<>();
            for (File pkg : pacmanPackages) {
                ApplicationInfo app = parsePacmanDesc(FileUtil.readFile(pkg.getPath() + "/desc", false));
                if (app != null) {
                    apps.add(app);
                }
            }
            return apps;
        }
        return new ArrayList<>();
    }

    /**
     * Fetches the list of installed applications by executing the appropriate package manager command. The package
     * manager is determined during class initialization and stored in {@code PACKAGE_MANAGER_COMMANDS}. If no supported
     * package manager is found, an empty list is returned.
     *
     * @return A list of strings, where each entry represents an installed application with its details. Returns an
     *         empty list if no supported package manager is available.
     */
    private static List<String> fetchInstalledApps() {
        if (PACKAGE_MANAGER_COMMANDS.isEmpty()) {
            return Collections.emptyList();
        }

        // Get the first available package manager's command
        String command = PACKAGE_MANAGER_COMMANDS.values().iterator().next();
        return ExecutingCommand.runNative(command);
    }

    private static boolean isPackageManagerAvailable(String packageManager) {
        List<String> result = ExecutingCommand.runNative(packageManager + " --version");
        // If the command executes fine the result is non-empty else empty
        return !result.isEmpty();
    }

    static List<ApplicationInfo> parseLinuxAppInfo(List<String> output) {
        Set<ApplicationInfo> appInfoSet = new LinkedHashSet<>();

        for (String line : output) {
            // split by the pipe character
            // Not a precompiled Pattern: a two-character backslash escape takes String.split's
            // regex-free fast path, which measures about 3x faster than Pattern.split
            String[] parts = line.split("\\|", -1); // -1 to keep empty fields

            // Check if we have all 8 fields
            if (parts.length >= 8) {
                // dpkg and rpm print epoch seconds; the API reports milliseconds
                appInfoSet.add(toAppInfo(parts[0], parts[1], parts[2], parts[3],
                        ParseUtil.parseLongOrDefault(parts[4], 0L) * 1000L, parts[5], parts[6], parts[7]));
            }
        }

        return new ArrayList<>(appInfoSet);
    }

    /**
     * Parses Alpine's {@code /lib/apk/db/installed}: one record per package, separated by blank lines, each line a
     * one-letter key, a colon and a value ({@code P} name, {@code V} version, {@code A} architecture, {@code I}
     * installed size, {@code m} maintainer, {@code o} origin package, {@code U} URL). The database records no install
     * time, and its {@code t} build time is not one, so the timestamp is 0.
     *
     * @param lines the lines of the database
     * @return the installed packages
     */
    static List<ApplicationInfo> parseApkInstalled(List<String> lines) {
        List<ApplicationInfo> apps = new ArrayList<>();
        Map<Character, String> record = new HashMap<>();
        for (String line : lines) {
            if (line.isEmpty()) {
                addApkRecord(apps, record);
            } else if (line.length() > 2 && line.charAt(1) == ':') {
                // Repeated keys (files, checksums) are not used, so keeping the last is harmless
                record.put(line.charAt(0), line.substring(2));
            }
        }
        addApkRecord(apps, record);
        return apps;
    }

    private static void addApkRecord(List<ApplicationInfo> apps, Map<Character, String> record) {
        String name = record.get('P');
        if (name != null) {
            apps.add(toAppInfo(name, record.get('V'), record.get('A'), record.get('I'), 0L, record.get('m'),
                    record.get('o'), record.get('U')));
        }
        record.clear();
    }

    /**
     * Parses one package's {@code desc} file from Arch's pacman database: sections headed {@code %KEY%}, each followed
     * by its value lines and a blank line.
     *
     * @param lines the lines of the file
     * @return the package, or {@code null} if the file has no name
     */
    static @Nullable ApplicationInfo parsePacmanDesc(List<String> lines) {
        Map<String, String> fields = new HashMap<>();
        String key = null;
        for (String line : lines) {
            if (line.length() > 2 && line.startsWith("%") && line.endsWith("%")) {
                key = line.substring(1, line.length() - 1);
            } else if (key != null && !line.isEmpty()) {
                // Only the first value line: multi-valued sections (licenses, dependencies) are not used
                fields.putIfAbsent(key, line);
            }
        }
        String name = fields.get("NAME");
        if (name == null) {
            return null;
        }
        // pacman prints epoch seconds
        return toAppInfo(name, fields.get("VERSION"), fields.get("ARCH"), fields.get("SIZE"),
                ParseUtil.parseLongOrDefault(fields.get("INSTALLDATE"), 0L) * 1000L, fields.get("PACKAGER"),
                fields.get("BASE"), fields.get("URL"));
    }

    private static ApplicationInfo toAppInfo(@Nullable String name, @Nullable String version, @Nullable String arch,
            @Nullable String size, long timestampMillis, @Nullable String vendor, @Nullable String source,
            @Nullable String homepage) {
        Map<String, String> additionalInfo = new LinkedHashMap<>();
        additionalInfo.put("architecture", ParseUtil.getStringValueOrUnknown(arch));
        additionalInfo.put("installedSize", String.valueOf(ParseUtil.parseLongOrDefault(size, 0L)));
        additionalInfo.put("source", ParseUtil.getStringValueOrUnknown(source));
        additionalInfo.put("homepage", ParseUtil.getStringValueOrUnknown(homepage));
        return new ApplicationInfo(ParseUtil.getStringValueOrUnknown(name), ParseUtil.getStringValueOrUnknown(version),
                ParseUtil.getStringValueOrUnknown(vendor), timestampMillis, additionalInfo);
    }
}
