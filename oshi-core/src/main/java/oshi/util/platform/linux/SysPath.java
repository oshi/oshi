/*
 * Copyright 2024 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.linux;

import java.io.File;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.GlobalConfig;

/**
 * Provides constants for paths in the {@code /sys} filesystem on Linux.
 * <p>
 * If the user desires to configure a custom {@code /sys} path, it must be declared in the OSHI configuration file or
 * updated in the {@link GlobalConfig} class prior to initializing this class.
 */
@ThreadSafe
public final class SysPath {

    /**
     * The /sys filesystem location.
     */
    public static final String SYS = querySysConfig() + "/";

    public static final String CPU = SYS + "devices/system/cpu/";
    public static final String DMI_ID = SYS + "devices/virtual/dmi/id/";
    public static final String NET = SYS + "class/net/";
    public static final String MODEL = SYS + "firmware/devicetree/base/model";
    public static final String POWER_SUPPLY = SYS + "class/power_supply";
    public static final String HWMON = SYS + "class/hwmon/";
    public static final String THERMAL = SYS + "class/thermal/";

    private SysPath() {
    }

    private static String querySysConfig() {
        String sysPath = GlobalConfig.get(GlobalConfig.OSHI_UTIL_SYS_PATH, "/sys");
        // Ensure prefix begins with path separator, but doesn't end with one
        sysPath = '/' + sysPath.replaceAll("/$|^/", "");
        if (!new File(sysPath).exists()) {
            throw new GlobalConfig.PropertyException(GlobalConfig.OSHI_UTIL_SYS_PATH, "The path does not exist");
        }
        return sysPath;
    }
}
