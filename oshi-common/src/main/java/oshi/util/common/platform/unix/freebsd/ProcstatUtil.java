/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.common.platform.unix.freebsd;

import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Reads process working-directory and open-file information from {@code procstat}.
 */
@ThreadSafe
public final class ProcstatUtil {

    private ProcstatUtil() {
    }

    /**
     * Gets current working directory info
     *
     * @param pid a process ID
     * @return the current working directory for that process.
     */
    public static String getCwd(int pid) {
        return parseCwd(ExecutingCommand.runNative("procstat -f " + pid));
    }

    /**
     * Parses {@code procstat -f <pid>} output to return the working directory (the row whose third column is
     * {@code cwd}).
     *
     * @param procstatLines lines returned by {@code procstat -f <pid>}
     * @return the working directory string, or empty if no {@code cwd} row is found
     */
    public static String parseCwd(List<String> procstatLines) {
        for (String line : procstatLines) {
            String[] split = ParseUtil.whitespaces.split(line.trim(), 10);
            if (split.length == 10 && split[2].equals("cwd")) {
                return split[9];
            }
        }
        return "";
    }

    /**
     * Gets open files
     *
     * @param pid The process ID
     * @return the number of open files.
     */
    public static long getOpenFiles(int pid) {
        return parseOpenFiles(ExecutingCommand.runNative("procstat -f " + pid));
    }

    /**
     * Counts vnode and descriptor rows in {@code procstat -f <pid>} output, excluding rows whose fifth column is one of
     * {@code V}, {@code d}, or {@code -}.
     *
     * @param procstatLines lines returned by {@code procstat -f <pid>}
     * @return the number of open files
     */
    public static long parseOpenFiles(List<String> procstatLines) {
        long fd = 0L;
        for (String line : procstatLines) {
            String[] split = ParseUtil.whitespaces.split(line.trim(), 10);
            if (split.length == 10 && !"Vd-".contains(split[4])) {
                fd++;
            }
        }
        return fd;
    }
}
