/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.unix.dragonflybsd;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Provides process information on DragonFlyBSD using /proc and fstat
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
        Map<Integer, String> cwdMap = new HashMap<>();
        if (pid >= 0) {
            String cwd = getCwd(pid);
            if (!cwd.isEmpty()) {
                cwdMap.put(pid, cwd);
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
        // Try /proc first
        try {
            Path link = Paths.get("/proc/" + pid + "/cwd");
            if (Files.isSymbolicLink(link)) {
                return Files.readSymbolicLink(link).toString();
            }
        } catch (IOException e) {
            // fall through to fstat
        }
        // Fallback to fstat
        List<String> fstat = ExecutingCommand.runNative("fstat -p " + pid);
        for (String line : fstat) {
            String[] split = ParseUtil.whitespaces.split(line.trim());
            if (split.length >= 8 && "wd".equals(split[4])) {
                return split[split.length - 1];
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
        // Try /proc first
        Path fdDir = Paths.get("/proc/" + pid + "/fd");
        if (Files.isDirectory(fdDir)) {
            try (Stream<Path> stream = Files.list(fdDir)) {
                return stream.count();
            } catch (IOException e) {
                // fall through to fstat
            }
        }
        // Fallback to fstat
        long fd = 0L;
        List<String> fstat = ExecutingCommand.runNative("fstat -p " + pid);
        for (String line : fstat) {
            String[] split = ParseUtil.whitespaces.split(line.trim());
            if (split.length >= 8 && !"wd".equals(split[4]) && !"root".equals(split[4]) && !"text".equals(split[4])) {
                fd++;
            }
        }
        // Subtract header line if present
        return fd > 0 ? fd - 1 : 0;
    }
}
