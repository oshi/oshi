/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.linux;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.software.os.AbstractInstalledApps;
import oshi.software.os.AppInfo;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LinuxInstalledApps extends AbstractInstalledApps {
    private static final Logger LOG = LoggerFactory.getLogger(LinuxInstalledApps.class);

    private enum LinuxFlavor {
        DEBIAN, RPM, UNKNOWN
    }

    @Override
    public List<AppInfo> getInstalledApps() {
        LinuxFlavor flavor = detectLinuxFlavor();
        List<String> output = fetchInstalledApps(flavor);
        return parseLinuxAppInfo(output);
    }

    private LinuxFlavor detectLinuxFlavor() {
        byte[] fileBytes = FileUtil.readAllBytes("/etc/os-release", true);
        List<String> lines = ParseUtil.parseByteArrayToStrings(fileBytes);

        for (String line : lines) {
            if (line.startsWith("ID_LIKE")) {
                if (line.contains("debian")) {
                    return LinuxFlavor.DEBIAN;
                } else if (line.contains("fedora")) {
                    return LinuxFlavor.RPM;
                }
            }
        }

        return LinuxFlavor.UNKNOWN;
    }

    private List<String> fetchInstalledApps(LinuxFlavor flavor) {
        String command;

        switch (flavor) {
        case DEBIAN:
            command = "dpkg-query -W -f='${Package}|${Version}|${Architecture}|${Installed-Size}|${db-fsys:Last-Modified}|${Maintainer}|${Source}|${Homepage}\\n'";
            break;
        case RPM:
            command = "rpm -qa --queryformat '%{NAME}|%{VERSION}-%{RELEASE}|%{ARCH}|%{SIZE}|%{INSTALLTIME}|%{PACKAGER}|%{SOURCERPM}|%{URL}\\n'";
            break;
        default:
            LOG.warn("Installed Apps stats are not supported for this flavor of Linux OS");
            return Collections.emptyList();
        }

        return ExecutingCommand.runNative(command);
    }

    private List<AppInfo> parseLinuxAppInfo(List<String> output) {
        List<AppInfo> appInfoList = new ArrayList<>();

        for (String line : output) {
            // split by the pipe character
            String[] parts = line.split("\\|", -1); // -1 to keep empty fields

            // Check if we have all 8 fields
            if (parts.length >= 8) {
                LinuxAppInfo app = new LinuxAppInfo(parts[0].isEmpty() ? Constants.UNKNOWN : parts[0], // Package Name
                        parts[1].isEmpty() ? Constants.UNKNOWN : parts[1], // Version
                        parts[2].isEmpty() ? Constants.UNKNOWN : parts[2], // Architecture
                        ParseUtil.parseLongOrDefault(parts[3], 0L), // Installed Size
                        ParseUtil.parseLongOrDefault(parts[4], 0L), // Date Epoch
                        parts[5].isEmpty() ? Constants.UNKNOWN : parts[5], // Maintainer
                        parts[6].isEmpty() ? Constants.UNKNOWN : parts[6], // Source Package
                        parts[7].isEmpty() ? Constants.UNKNOWN : parts[7] // Homepage / URL
                );
                appInfoList.add(app);
            }
        }

        return appInfoList;
    }
}
