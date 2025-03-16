/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os;

import oshi.software.os.linux.LinuxInstalledApps;
import oshi.software.os.mac.MacInstalledApps;
import oshi.software.os.windows.WindowsInstalledApps;

public class InstalledAppsFactory {

    public static AbstractInstalledApps getInstalledApps() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            return new WindowsInstalledApps();
        } else if (os.contains("linux")) {
            return new LinuxInstalledApps();
        } else if (os.contains("mac")) {
            return new MacInstalledApps();
        } else {
            throw new UnsupportedOperationException("OS not supported: " + os);
        }
    }
}
