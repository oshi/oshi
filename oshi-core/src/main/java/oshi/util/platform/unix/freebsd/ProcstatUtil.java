/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.unix.freebsd;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Reads from procstat into a map
 */
@ThreadSafe
public final class ProcstatUtil {

    private ProcstatUtil() {
    }

    /**
     * Gets a map containing current working directory info
     *
     * @param pid a process ID, optional
     * @return a map of process IDs to their current working directory. If {@code pid} is a negative number, all
     *         processes are returned; otherwise the map may contain only a single element for {@code pid}
     */
    public static Map<Integer, String> getCwdMap(int pid) {
        List<String> procstat = ExecutingCommand.runNative("procstat -f " + (pid < 0 ? "-a" : pid));
        Map<Integer, String> cwdMap = new HashMap<>();
        for (String line : procstat) {
            String[] split = ParseUtil.whitespaces.split(line.trim(), 10);
            if (split.length == 10 && split[2].equals("cwd")) {
                cwdMap.put(ParseUtil.parseIntOrDefault(split[0], -1), split[9]);
            }
        }
        return cwdMap;
    }

    /**
     * Gets current working directory info
     *
     * @param pid a process ID
     * @return the current working directory for that process.
     */
    public static String getCwd(int pid) {
        List<String> procstat = ExecutingCommand.runNative("procstat -f " + pid);
        for (String line : procstat) {
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
        long fd = 0L;
        List<String> procstat = ExecutingCommand.runNative("procstat -f " + pid);
        for (String line : procstat) {
            String[] split = ParseUtil.whitespaces.split(line.trim(), 10);
            if (split.length == 10 && !"Vd-".contains(split[4])) {
                fd++;
            }
        }
        return fd;
    }
}
