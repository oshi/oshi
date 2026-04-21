/*
 * Copyright 2019-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import java.util.Properties;

import oshi.annotation.concurrent.NotThreadSafe;
import oshi.hardware.CentralProcessor;

/**
 * The global configuration utility. See {@code src/main/resources/oshi.properties} for default values.
 * <p>
 * Configuration values set as Java System Properties using {@link System#setProperty(String, String)} will override
 * values from the {@code oshi.properties} file, but may then be later altered using {@link #set(String, Object)} or
 * {@link #remove(String)}.
 * <p>
 * This class is not thread safe if methods manipulating the configuration are used. These methods are intended for use
 * by a single thread at startup, before instantiation of any other OSHI classes. OSHI does not guarantee re- reading of
 * any configuration changes.
 */
@NotThreadSafe
public final class GlobalConfig {

    private static final String OSHI_PROPERTIES = "oshi.properties";

    private static final Properties CONFIG = FileUtil.readPropertiesFromFilename(OSHI_PROPERTIES);
    static {
        System.getProperties().forEach((k, v) -> {
            String key = k.toString();
            if (key.startsWith("oshi.")) {
                set(key, v);
            }
        });
    }

    /**
     * Memoizer default expiration in milliseconds. Cached return values will be refreshed after this interval. Must be
     * positive (a negative value will never refresh). Should be less than 1 second. Default is 300 milliseconds.
     */
    public static final String OSHI_UTIL_MEMOIZER_EXPIRATION = "oshi.util.memoizer.expiration";
    /**
     * The WMI query timeout in milliseconds. Default is -1 (no timeout).
     */
    public static final String OSHI_UTIL_WMI_TIMEOUT = "oshi.util.wmi.timeout";
    /**
     * The {@code /proc} filesystem location. Some containers enable alternate locations to provide container-level
     * output in preference to system-level output. Default is {@code /proc}.
     */
    public static final String OSHI_UTIL_PROC_PATH = "oshi.util.proc.path";
    /**
     * The {@code /sys} filesystem location. Some containers enable alternate locations to provide container-level
     * output in preference to system-level output. Default is {@code /sys}.
     */
    public static final String OSHI_UTIL_SYS_PATH = "oshi.util.sys.path";
    /**
     * The {@code /dev} filesystem location. Some containers enable alternate locations to provide container-level
     * output in preference to system-level output. Default is {@code /dev}.
     */
    public static final String OSHI_UTIL_DEV_PATH = "oshi.util.dev.path";

    /**
     * A comma-separated list of filesystem types to treat as pseudo-filesystems and exclude from file store listings.
     * See {@code oshi.properties} for the full default list.
     */
    public static final String OSHI_PSEUDO_FILESYSTEM_TYPES = "oshi.pseudo.filesystem.types";
    /**
     * A comma-separated list of filesystem types that are network-based and should be excluded from local-only file
     * store listings. Default includes {@code afs}, {@code cifs}, {@code smbfs}, {@code nfs}, {@code nfs4}, and others.
     */
    public static final String OSHI_NETWORK_FILESYSTEM_TYPES = "oshi.network.filesystem.types";

    /**
     * Whether to use udev on Linux for loading hardware information such as USB devices, power sources, and disk
     * information. Some details can be loaded via sysfs as a fallback, but others require udev. Default is
     * {@code true}.
     */
    public static final String OSHI_OS_LINUX_ALLOWUDEV = "oshi.os.linux.allowudev";
    /**
     * Whether to use systemd on Linux for loading session information when utmp is unavailable or deprecated. Set to
     * {@code false} to fall back to file-based session parsing or the {@code who} command. Default is {@code true}.
     */
    public static final String OSHI_OS_LINUX_ALLOWSYSTEMD = "oshi.os.linux.allowsystemd";
    /**
     * Whether to log warnings when failing to read process information from the {@code /proc} pseudo-filesystem on
     * Linux. When running without elevated permissions, frequent errors reading process environment files are expected.
     * Default is {@code false}.
     */
    public static final String OSHI_OS_LINUX_PROCFS_LOGWARNING = "oshi.os.linux.procfs.logwarning";

    /**
     * Whether to log warnings when failing to read process information via sysctl on macOS. When running without
     * elevated permissions, frequent errors reading process environment are expected. Default is {@code false}.
     */
    public static final String OSHI_OS_MAC_SYSCTL_LOGWARNING = "oshi.os.mac.sysctl.logwarning";

    /**
     * The name of the Windows System event log containing bootup event IDs 12 and 6005, used for a one-time calculation
     * of system boot time that is consistent across process runs regardless of sleep/hibernate cycles. If set to the
     * empty string, boot time is calculated by subtracting uptime from the current time (faster but less accurate).
     * Default is {@code System}.
     */
    public static final String OSHI_OS_WINDOWS_EVENTLOG = "oshi.os.windows.eventlog";
    /**
     * Whether to update the process state on Windows to {@code SUSPENDED} if all its threads are suspended. This
     * requires querying thread states and can significantly impact performance when querying process lists. Default is
     * {@code false}.
     */
    public static final String OSHI_OS_WINDOWS_PROCSTATE_SUSPENDED = "oshi.os.windows.procstate.suspended";
    /**
     * Whether to perform WMI queries for command lines in a batch for all running processes. Individual WMI queries
     * take about 50ms each while querying the entire process list takes about 200ms. Enable this if you regularly query
     * command lines for more than a few processes. Default is {@code false}.
     */
    public static final String OSHI_OS_WINDOWS_COMMANDLINE_BATCH = "oshi.os.windows.commandline.batch";
    /**
     * Whether to attempt to fetch Windows performance counter data for processes and threads from
     * {@code HKEY_PERFORMANCE_DATA} in the registry. This is faster than the PDH API but more subject to registry
     * corruption and counter deletion when changing language settings. Set to {@code false} to skip the registry check
     * and use the API-recommended (but slower) performance counter API. Default is {@code true}.
     */
    public static final String OSHI_OS_WINDOWS_HKEYPERFDATA = "oshi.os.windows.hkeyperfdata";
    /**
     * Whether to use the Legacy Processor performance counters for System CPU ticks instead of Processor Information
     * (since Windows 7). Legacy counters are not processor-group aware and may give incorrect results on systems with
     * more than 64 logical processors. Default is {@code false}.
     */
    public static final String OSHI_OS_WINDOWS_LEGACY_SYSTEM_COUNTERS = "oshi.os.windows.legacy.system.counters";
    /**
     * Whether to start a daemon thread to provide load averages on Windows. While Windows does not natively provide
     * this metric, the Processor Queue Length performance counter combined with recent processor usage provides a
     * similar metric. Default is {@code false}.
     *
     * @see CentralProcessor#getSystemLoadAverage(int)
     */
    public static final String OSHI_OS_WINDOWS_LOADAVERAGE = "oshi.os.windows.loadaverage";
    /**
     * Whether to use "Processor Utility" for System and per-processor CPU Load ticks (on Windows 8 and higher) to match
     * CPU usage with the Windows Task Manager.
     * <p>
     * By default, OSHI uses "Processor Time" which measures active and idle times, matching Unix-based systems and
     * Windows 7 Task Manager. Setting this to {@code true} switches to "Processor Utility" which measures work
     * completed relative to nominal frequency. Because of features like Intel Turbo Boost and AMD Precision Boost, this
     * value can exceed 100% (though Task Manager caps it). OSHI does not cap the value.
     * <p>
     * For this calculation to produce meaningful data, the ticks must come from the same CentralProcessor instance and
     * the first polling interval must be less than 7 minutes. Default is {@code false}.
     *
     * @see CentralProcessor#getSystemCpuLoadTicks()
     */
    public static final String OSHI_OS_WINDOWS_CPU_UTILITY = "oshi.os.windows.cpu.utility";

    /**
     * Whether PerfDisk performance counters are disabled. If counters are intentionally disabled (e.g., for gaming
     * performance) or the application does not need disk read/write/queue metrics, set to {@code true} to skip querying
     * and suppress log warnings. Default is unset (OSHI auto-detects from the registry).
     */
    public static final String OSHI_OS_WINDOWS_PERFDISK_DISABLED = "oshi.os.windows.perfdisk.disabled";
    /**
     * Whether PerfOS performance counters are disabled. These counters are used for CPU ticks, swap file usage, page
     * swaps, context switches, and interrupts. Set to {@code true} to skip querying and suppress log warnings. Default
     * is unset (OSHI auto-detects from the registry).
     */
    public static final String OSHI_OS_WINDOWS_PERFOS_DISABLED = "oshi.os.windows.perfos.disabled";
    /**
     * Whether PerfProc performance counters are disabled. These counters are used for process and thread priority,
     * time, I/O, and memory usage. Set to {@code true} to skip querying and suppress log warnings. Default is unset
     * (OSHI auto-detects from the registry).
     *
     * @see #OSHI_OS_WINDOWS_HKEYPERFDATA
     */
    public static final String OSHI_OS_WINDOWS_PERFPROC_DISABLED = "oshi.os.windows.perfproc.disabled";

    /**
     * Whether to assume any performance counter failure means all counters will fail and revert to WMI backup. Default
     * is {@code false}.
     */
    public static final String OSHI_OS_WINDOWS_PERF_DISABLE_ALL_ON_FAILURE = "oshi.os.windows.perf.disable.all.on.failure";

    /**
     * Whether to use the Posix-standard {@code who} command for session information instead of native code. The native
     * implementation ({@code getutxent}) is not thread safe; while OSHI synchronizes its own access, other OS code may
     * access the same data structures. The command-line variant may use reentrant code on some platforms. Default is
     * {@code false}.
     */
    public static final String OSHI_OS_UNIX_WHOCOMMAND = "oshi.os.unix.whoCommand";
    /**
     * Whether to allow use of the kstat2 API on Solaris 11.4+. The new API offers additional features but may have a
     * file descriptor leak when parallel GC is in use. Set to {@code false} to always use the original kstat API.
     * Default is {@code true}.
     */
    public static final String OSHI_OS_SOLARIS_ALLOWKSTAT2 = "oshi.os.solaris.allowKstat2";

    /**
     * The command prefix to prepend for privileged command execution on Linux (e.g., {@code "sudo -n"} for
     * non-interactive sudo). This supports the Principle of Least Privilege by allowing fine-grained access to specific
     * commands without running the entire application as root. The prefix is not applied when already running as root.
     * Default is the empty string (no privilege escalation).
     *
     * @see #OSHI_OS_LINUX_PRIVILEGED_ALLOWLIST
     * @see #OSHI_OS_LINUX_PRIVILEGED_FILE_ALLOWLIST
     */
    public static final String OSHI_OS_LINUX_PRIVILEGED_PREFIX = "oshi.os.linux.privileged.prefix";
    /**
     * A comma-separated list of commands eligible for privilege escalation on Linux. Use full paths matching your
     * sudoers configuration (e.g., {@code /usr/sbin/dmidecode}) or bare names to match regardless of path. The
     * allowlist should mirror the {@code NOPASSWD} entries in your sudoers file. Default is the empty string (no
     * commands allowed).
     *
     * @see #OSHI_OS_LINUX_PRIVILEGED_PREFIX
     */
    public static final String OSHI_OS_LINUX_PRIVILEGED_ALLOWLIST = "oshi.os.linux.privileged.allowlist";
    /**
     * A comma-separated list of file paths eligible for privileged read via {@code sudo cat} fallback on Linux when
     * normal file read fails due to permissions. Supports Java glob patterns ({@code *}, {@code ?}, {@code [abc]}).
     * <p>
     * <b>Security warning:</b> Take care when adding paths. Allowing privileged reads of sensitive files such as
     * {@code /proc/*
     /
    environ} can expose credentials to unprivileged users. Default is the empty
     *
     * string (no files allowed).
     *
     * @see #OSHI_OS_LINUX_PRIVILEGED_PREFIX
     */

    public static final String OSHI_OS_LINUX_PRIVILEGED_FILE_ALLOWLIST = "oshi.os.linux.privileged.file.allowlist";

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
