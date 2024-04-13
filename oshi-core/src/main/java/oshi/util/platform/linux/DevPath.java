/*
 * Copyright 2024 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.linux;

import java.io.File;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.GlobalConfig;

/**
 * Provides constants for paths in the {@code /dev} filesystem on Linux.
 * <p>
 * If the user desires to configure a custom {@code /dev} path, it must be declared in the OSHI configuration file or
 * updated in the {@link GlobalConfig} class prior to initializing this class.
 */
@ThreadSafe
public final class DevPath {

    /**
     * The /dev filesystem location.
     */
    public static final String DEV = queryDevConfig();

    public static final String ROOT = DEV + "/";
    public static final String DISK_BY_UUID = ROOT + "disk/by-uuid";
    public static final String DM = ROOT + "dm";
    public static final String LOOP = ROOT + "loop";
    public static final String MAPPER = ROOT + "mapper/";
    public static final String RAM = ROOT + "ram";

    private DevPath() {
    }

    private static String queryDevConfig() {
        String devPath = GlobalConfig.get(GlobalConfig.OSHI_UTIL_DEV_PATH, "/dev");
        // Ensure prefix begins with path separator, but doesn't end with one
        devPath = '/' + devPath.replaceAll("/$|^/", "");
        if (!new File(devPath).exists()) {
            throw new GlobalConfig.PropertyException(GlobalConfig.OSHI_UTIL_DEV_PATH, "The path does not exist");
        }
        return devPath;
    }
}
