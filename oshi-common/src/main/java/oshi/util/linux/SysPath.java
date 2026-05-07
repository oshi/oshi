/*
 * Copyright 2024-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.linux;

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

    /** Path to CPU devices. */
    public static final String CPU = SYS + "devices/system/cpu/";
    /** Path to DMI identification. */
    public static final String DMI_ID = SYS + "devices/virtual/dmi/id/";
    /** Path to network interfaces. */
    public static final String NET = SYS + "class/net/";
    /** Path to device tree model. */
    public static final String MODEL = SYS + "firmware/devicetree/base/model";
    /** Path to power supply class. */
    public static final String POWER_SUPPLY = SYS + "class/power_supply";
    /** Path to hardware monitoring. */
    public static final String HWMON = SYS + "class/hwmon/";
    /** Path to thermal class. */
    public static final String THERMAL = SYS + "class/thermal/";
    /** Path to cgroup filesystem. */
    public static final String CGROUP = SYS + "fs/cgroup/";

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
