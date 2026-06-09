/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.common.platform.unix.netbsd;

import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.ExecutingCommand;

/**
 * Reads from fstat and related utilities on NetBSD.
 */
@ThreadSafe
public final class FstatUtil {
    private FstatUtil() {
    }

    /**
     * Gets current working directory info.
     *
     * @param pid a process ID
     * @return the current working directory for that process.
     */
    public static String getCwd(int pid) {
        // NetBSD: try /proc if mounted
        List<String> ls = ExecutingCommand.runNative("readlink /proc/" + pid + "/cwd");
        if (!ls.isEmpty() && !ls.get(0).isEmpty()) {
            return ls.get(0);
        }
        // Fallback: parse from fstat output (field after 'wd')
        return parseCwdFromFstat(ExecutingCommand.runNative("fstat -p " + pid));
    }

    /**
     * Parses {@code fstat -p <pid>} output to find the {@code wd} row and return the path column.
     *
     * @param fstatLines lines returned by {@code fstat -p <pid>}
     * @return the working directory string, or empty if no {@code wd} row is found
     */
    public static String parseCwdFromFstat(List<String> fstatLines) {
        for (String line : fstatLines) {
            String[] split = line.trim().split("\\s+");
            if (split.length > 4 && "wd".equals(split[3])) {
                return split[4];
            }
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
        // NetBSD fstat output: USER CMD PID FD MOUNT INUM MODE SZ|DV R/W
        return parseOpenFiles(ExecutingCommand.runNative("fstat -p " + pid));
    }

    /**
     * Counts open file descriptor rows in {@code fstat -p <pid>} output (ignoring the header).
     *
     * @param fstatLines lines returned by {@code fstat -p <pid>}
     * @return the number of open files (rows minus 1 for the header), or 0 if header alone or empty
     */
    public static long parseOpenFiles(List<String> fstatLines) {
        if (fstatLines.size() > 1) {
            // subtract 1 for header row
            return fstatLines.size() - 1L;
        }
        return 0L;
    }
}
