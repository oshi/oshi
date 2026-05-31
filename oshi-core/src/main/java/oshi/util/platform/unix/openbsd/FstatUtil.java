/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.unix.openbsd;

import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Reads from fstat.
 */
@ThreadSafe
public final class FstatUtil {
    private FstatUtil() {
    }

    /**
     * Gets current working directory info (using {@code ps} actually).
     *
     * @param pid a process ID
     * @return the current working directory for that process.
     */
    public static String getCwd(int pid) {
        List<String> ps = ExecutingCommand.runNative("ps -axwwo cwd -p " + pid);
        if (ps.size() > 1) {
            return ps.get(1);
        }
        return "";
    }

    /**
     * Gets open number of files.
     *
     * @param pid The process ID
     * @return the number of open files.
     */
    public static long getOpenFiles(int pid) {
        return parseOpenFiles(ExecutingCommand.runNative("fstat -sp " + pid));
    }

    /**
     * Counts open file descriptor rows in {@code fstat -sp <pid>} output (OpenBSD format), excluding rows whose fifth
     * column matches {@code pipe} or {@code unix}, and subtracting the header row.
     *
     * @param fstatLines lines returned by {@code fstat -sp <pid>}
     * @return the number of open files (counted rows minus 1 for the header); {@code 0} for empty or header-only input
     */
    public static long parseOpenFiles(List<String> fstatLines) {
        long fd = 0L;
        for (String line : fstatLines) {
            String[] split = ParseUtil.whitespaces.split(line.trim(), 11);
            if (split.length == 11 && !"pipe".contains(split[4]) && !"unix".contains(split[4])) {
                fd++;
            }
        }
        // Subtract 1 for the header row, but never return a negative count.
        return fd > 0 ? fd - 1 : 0;
    }
}
