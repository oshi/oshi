/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.common.platform.unix.netbsd;

import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

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
        // NetBSD: try /proc if mounted. fstat is no fallback: its wd row names the mount point and inode, not a path
        List<String> ls = ExecutingCommand.runNative("readlink /proc/" + pid + "/cwd");
        return ls.isEmpty() ? "" : ls.get(0);
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
     * Counts the open file descriptor rows in {@code fstat -p <pid>} output.
     * <p>
     * Only rows whose FD column is a number count. That skips the header and the rows {@code fstat} prints for a
     * process's text, working directory, root directory and trace file ({@code text}, {@code wd}, {@code root} and
     * {@code tr}), none of which is a descriptor. A descriptor shared with another process is marked with a trailing
     * {@code *}.
     *
     * @param fstatLines lines returned by {@code fstat -p <pid>}
     * @return the number of open file descriptors
     */
    public static long parseOpenFiles(List<String> fstatLines) {
        long fds = 0L;
        for (String line : fstatLines) {
            String[] split = ParseUtil.whitespaces.split(line.trim(), -1);
            if (split.length > 3 && !split[3].isEmpty() && Character.isDigit(split[3].charAt(0))) {
                fds++;
            }
        }
        return fds;
    }
}
