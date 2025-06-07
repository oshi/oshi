/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.linux;

import oshi.software.os.ApplicationInfo;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.regex.Pattern;

public final class LinuxInstalledApps {

    private static final Pattern PIPE_PATTERN = Pattern.compile("\\|");
    private static final Map<String, String> PACKAGE_MANAGER_COMMANDS = initializePackageManagerCommands();

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
        List<String> output = fetchInstalledApps();
        return parseLinuxAppInfo(output);
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

    private static List<ApplicationInfo> parseLinuxAppInfo(List<String> output) {
        Set<ApplicationInfo> appInfoSet = new LinkedHashSet<>();

        for (String line : output) {
            // split by the pipe character
            String[] parts = PIPE_PATTERN.split(line, -1); // -1 to keep empty fields

            // Check if we have all 8 fields
            if (parts.length >= 8) {
                // Additional info map
                Map<String, String> additionalInfo = new LinkedHashMap<>();
                additionalInfo.put("architecture", ParseUtil.getStringValueOrUnknown(parts[2]));
                additionalInfo.put("installedSize", String.valueOf(ParseUtil.parseLongOrDefault(parts[3], 0L)));
                additionalInfo.put("source", ParseUtil.getStringValueOrUnknown(parts[6]));
                additionalInfo.put("homepage", ParseUtil.getStringValueOrUnknown(parts[7]));

                ApplicationInfo app = new ApplicationInfo(ParseUtil.getStringValueOrUnknown(parts[0]), // Package name
                        ParseUtil.getStringValueOrUnknown(parts[1]), // Version
                        ParseUtil.getStringValueOrUnknown(parts[5]), // Vendor
                        ParseUtil.parseLongOrDefault(parts[4], 0L), // Date Epoch
                        additionalInfo);

                appInfoSet.add(app);
            }
        }

        return new ArrayList<>(appInfoSet);
    }
}
