/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.linux;

import java.io.File;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.GlobalConfig;

/**
 * Provides constants for paths in the {@code /proc} filesystem on Linux.
 * <p>
 * If the user desires to configure a custom {@code /proc} path, it must be declared in the OSHI configuration file or
 * updated in the {@link GlobalConfig} class prior to initializing this class.
 */
@ThreadSafe
public final class ProcPath {

    /**
     * The /proc filesystem location.
     */
    public static final String PROC = queryProcConfig();

    /** Path to asound. */
    public static final String ASOUND = PROC + "/asound/";
    /** Path to auxv. */
    public static final String AUXV = PROC + "/self/auxv";
    /** Path to cpuinfo. */
    public static final String CPUINFO = PROC + "/cpuinfo";
    /** Path to diskstats. */
    public static final String DISKSTATS = PROC + "/diskstats";
    /** Path to loadavg. */
    public static final String LOADAVG = PROC + "/loadavg";
    /** Path to meminfo. */
    public static final String MEMINFO = PROC + "/meminfo";
    /** Path to model. */
    public static final String MODEL = PROC + "/device-tree/model";
    /** Path to mounts. */
    public static final String MOUNTS = PROC + "/mounts";
    /** Path to net. */
    public static final String NET = PROC + "/net";
    /** Path to /proc/[pid]/cmdline. */
    public static final String PID_CMDLINE = PROC + "/%d/cmdline";
    /** Path to /proc/[pid]/cwd. */
    public static final String PID_CWD = PROC + "/%d/cwd";
    /** Path to /proc/[pid]/exe. */
    public static final String PID_EXE = PROC + "/%d/exe";
    /** Path to /proc/[pid]/environ. */
    public static final String PID_ENVIRON = PROC + "/%d/environ";
    /** Path to /proc/[pid]/fd. */
    public static final String PID_FD = PROC + "/%d/fd";
    /** Path to /proc/[pid]/io. */
    public static final String PID_IO = PROC + "/%d/io";
    /** Path to /proc/[pid]/stat. */
    public static final String PID_STAT = PROC + "/%d/stat";
    /** Path to /proc/[pid]/statm. */
    public static final String PID_STATM = PROC + "/%d/statm";
    /** Path to /proc/[pid]/status. */
    public static final String PID_STATUS = PROC + "/%d/status";
    /** Path to /proc/self/stat. */
    public static final String SELF_STAT = PROC + "/self/stat";
    /** Path to snmp. */
    public static final String SNMP = NET + "/snmp";
    /** Path to snmp6. */
    public static final String SNMP6 = NET + "/snmp6";
    /** Path to stat. */
    public static final String STAT = PROC + "/stat";
    /** Path to /proc/sys/fs/file nr. */
    public static final String SYS_FS_FILE_NR = PROC + "/sys/fs/file-nr";
    /** Path to /proc/sys/fs/file max. */
    public static final String SYS_FS_FILE_MAX = PROC + "/sys/fs/file-max";
    /** Path to /proc/[pid]/task/path. */
    public static final String TASK_PATH = PROC + "/%d/task";
    /** Path to /proc/[pid]/task/comm. */
    public static final String TASK_COMM = TASK_PATH + "/%d/comm";
    /** Path to /proc/[pid]/task/status. */
    public static final String TASK_STATUS = TASK_PATH + "/%d/status";
    /** Path to /proc/[pid]/task/stat. */
    public static final String TASK_STAT = TASK_PATH + "/%d/stat";
    /** Path to thread self. */
    public static final String THREAD_SELF = PROC + "/thread-self";
    /** Path to uptime. */
    public static final String UPTIME = PROC + "/uptime";
    /** Path to version. */
    public static final String VERSION = PROC + "/version";
    /** Path to vmstat. */
    public static final String VMSTAT = PROC + "/vmstat";
    /** Path to /proc/self/cgroup. */
    public static final String SELF_CGROUP = PROC + "/self/cgroup";
    /** Path to filesystems. */
    public static final String FILESYSTEMS = PROC + "/filesystems";

    private ProcPath() {
    }

    private static String queryProcConfig() {
        String procPath = GlobalConfig.get(GlobalConfig.OSHI_UTIL_PROC_PATH, "/proc");
        // Ensure prefix begins with path separator, but doesn't end with one
        procPath = '/' + procPath.replaceAll("/$|^/", "");
        if (!new File(procPath).exists()) {
            throw new GlobalConfig.PropertyException(GlobalConfig.OSHI_UTIL_PROC_PATH, "The path does not exist");
        }
        return procPath;
    }
}
