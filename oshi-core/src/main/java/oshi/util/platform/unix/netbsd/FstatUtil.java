/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.unix.netbsd;

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
        // NetBSD: use sysctl or /proc if mounted
        List<String> ls = ExecutingCommand.runNative("readlink /proc/" + pid + "/cwd");
        if (!ls.isEmpty() && !ls.get(0).isEmpty()) {
            return ls.get(0);
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
        List<String> fstat = ExecutingCommand.runNative("fstat -p " + pid);
        if (fstat.size() > 1) {
            // subtract 1 for header row
            return fstat.size() - 1L;
        }
        return 0L;
    }
}
