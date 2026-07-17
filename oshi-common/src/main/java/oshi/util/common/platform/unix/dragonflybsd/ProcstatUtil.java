/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.common.platform.unix.dragonflybsd;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
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
        return parseCwdFromFstat(ExecutingCommand.runNative("fstat -p " + pid));
    }

    /**
     * Parses {@code fstat -p <pid>} output (DragonFlyBSD format) to find the {@code wd} row and return the path column
     * (the last whitespace-separated token of that row).
     *
     * @param fstatLines lines returned by {@code fstat -p <pid>}
     * @return the working directory string, or empty if no {@code wd} row is found
     */
    public static String parseCwdFromFstat(List<String> fstatLines) {
        for (String line : fstatLines) {
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
        return parseOpenFiles(ExecutingCommand.runNative("fstat -p " + pid));
    }

    /**
     * Counts open file descriptor rows in {@code fstat -p <pid>} output (DragonFlyBSD format), excluding {@code wd},
     * {@code root}, and {@code text} rows and the header line.
     *
     * @param fstatLines lines returned by {@code fstat -p <pid>}
     * @return the number of open files (counted rows minus 1 header), or 0 if none counted
     */
    public static long parseOpenFiles(List<String> fstatLines) {
        long fd = 0L;
        for (String line : fstatLines) {
            String[] split = ParseUtil.whitespaces.split(line.trim());
            if (split.length >= 8 && !"wd".equals(split[4]) && !"root".equals(split[4]) && !"text".equals(split[4])) {
                fd++;
            }
        }
        // Subtract header line if present
        return fd > 0 ? fd - 1 : 0;
    }
}
