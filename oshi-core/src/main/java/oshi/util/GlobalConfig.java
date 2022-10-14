/*
 * Copyright 2019-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import java.util.Properties;

import oshi.annotation.concurrent.NotThreadSafe;

/**
 * The global configuration utility. See {@code src/main/resources/oshi.properties} for default values.
 * <p>
 * This class is not thread safe if methods manipulating the configuration are used. These methods are intended for use
 * by a single thread at startup, before instantiation of any other OSHI classes. OSHI does not guarantee re- reading of
 * any configuration changes.
 */
@NotThreadSafe
public final class GlobalConfig {

    private static final String OSHI_PROPERTIES = "oshi.properties";

    private static final Properties CONFIG = FileUtil.readPropertiesFromFilename(OSHI_PROPERTIES);

    public static final String OSHI_UTIL_MEMOIZER_EXPIRATION = "oshi.util.memoizer.expiration";
    public static final String OSHI_UTIL_WMI_TIMEOUT = "oshi.util.wmi.timeout";
    public static final String OSHI_UTIL_PROC_PATH = "oshi.util.proc.path";

    public static final String OSHI_PSEUDO_FILESYSTEM_TYPES = "oshi.pseudo.filesystem.types";
    public static final String OSHI_NETWORK_FILESYSTEM_TYPES = "oshi.network.filesystem.types";

    public static final String OSHI_OS_LINUX_PROCFS_LOGWARNING = "oshi.os.linux.procfs.logwarning";

    public static final String OSHI_OS_MAC_SYSCTL_LOGWARNING = "oshi.os.mac.sysctl.logwarning";

    public static final String OSHI_OS_WINDOWS_EVENTLOG = "oshi.os.windows.eventlog";
    public static final String OSHI_OS_WINDOWS_PROCSTATE_SUSPENDED = "oshi.os.windows.procstate.suspended";
    public static final String OSHI_OS_WINDOWS_COMMANDLINE_BATCH = "oshi.os.windows.commandline.batch";
    public static final String OSHI_OS_WINDOWS_HKEYPERFDATA = "oshi.os.windows.hkeyperfdata";
    public static final String OSHI_OS_WINDOWS_LOADAVERAGE = "oshi.os.windows.loadaverage";
    public static final String OSHI_OS_WINDOWS_CPU_UTILITY = "oshi.os.windows.cpu.utility";

    public static final String OSHI_OS_WINDOWS_PERFDISK_DIABLED = "oshi.os.windows.perfdisk.disabled";
    public static final String OSHI_OS_WINDOWS_PERFOS_DIABLED = "oshi.os.windows.perfos.disabled";
    public static final String OSHI_OS_WINDOWS_PERFPROC_DIABLED = "oshi.os.windows.perfproc.disabled";

    public static final String OSHI_OS_UNIX_WHOCOMMAND = "oshi.os.unix.whoCommand";

    private GlobalConfig() {
    }

    /**
     * Get the property associated with the given key.
     *
     * @param key The property key
     * @return The property value if it exists, or null otherwise
     */
    public static String get(String key) {
        return CONFIG.getProperty(key);
    }

    /**
     * Get the {@code String} property associated with the given key.
     *
     * @param key The property key
     * @param def The default value
     * @return The property value or the given default if not found
     */
    public static String get(String key, String def) {
        return CONFIG.getProperty(key, def);
    }

    /**
     * Get the {@code int} property associated with the given key.
     *
     * @param key The property key
     * @param def The default value
     * @return The property value or the given default if not found
     */
    public static int get(String key, int def) {
        String value = CONFIG.getProperty(key);
        return value == null ? def : ParseUtil.parseIntOrDefault(value, def);
    }

    /**
     * Get the {@code double} property associated with the given key.
     *
     * @param key The property key
     * @param def The default value
     * @return The property value or the given default if not found
     */
    public static double get(String key, double def) {
        String value = CONFIG.getProperty(key);
        return value == null ? def : ParseUtil.parseDoubleOrDefault(value, def);
    }

    /**
     * Get the {@code boolean} property associated with the given key.
     *
     * @param key The property key
     * @param def The default value
     * @return The property value or the given default if not found
     */
    public static boolean get(String key, boolean def) {
        String value = CONFIG.getProperty(key);
        return value == null ? def : Boolean.parseBoolean(value);
    }

    /**
     * Set the given property, overwriting any existing value. If the given value is {@code null}, the property is
     * removed.
     *
     * @param key The property key
     * @param val The new value
     */
    public static void set(String key, Object val) {
        if (val == null) {
            CONFIG.remove(key);
        } else {
            CONFIG.setProperty(key, val.toString());
        }
    }

    /**
     * Reset the given property to its default value.
     *
     * @param key The property key
     */
    public static void remove(String key) {
        CONFIG.remove(key);
    }

    /**
     * Clear the configuration.
     */
    public static void clear() {
        CONFIG.clear();
    }

    /**
     * Load the given {@link java.util.Properties} into the global configuration.
     *
     * @param properties The new properties
     */
    public static void load(Properties properties) {
        CONFIG.putAll(properties);
    }

    /**
     * Indicates that a configuration value is invalid.
     */
    public static class PropertyException extends RuntimeException {

        private static final long serialVersionUID = -7482581936621748005L;

        /**
         * @param property The property name
         */
        public PropertyException(String property) {
            super("Invalid property: \"" + property + "\" = " + GlobalConfig.get(property, null));
        }

        /**
         * @param property The property name
         * @param message  An exception message
         */
        public PropertyException(String property, String message) {
            super("Invalid property \"" + property + "\": " + message);
        }
    }
}
