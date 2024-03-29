/*
 * Copyright 2020-2024 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.linux.proc;

import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.CentralProcessor.TickType;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.platform.linux.ProcPath;

/**
 * Utility to read CPU statistics from {@code /proc/stat}
 */
@ThreadSafe
public final class CpuStat {

    private CpuStat() {
    }

    /**
     * Gets the System CPU ticks array from {@code /proc/stat}
     *
     * @return Array of CPU ticks
     */
    public static long[] getSystemCpuLoadTicks() {
        long[] ticks = new long[TickType.values().length];
        // /proc/stat expected format
        // first line is overall user,nice,system,idle,iowait,irq, etc.
        // cpu 3357 0 4313 1362393 ...
        String tickStr;
        List<String> procStat = FileUtil.readLines(ProcPath.STAT, 1);
        if (procStat.isEmpty()) {
            return ticks;
        }
        tickStr = procStat.get(0);

        // Split the line. Note the first (0) element is "cpu" so remaining
        // elements are offset by 1 from the enum index
        String[] tickArr = ParseUtil.whitespaces.split(tickStr);
        if (tickArr.length <= TickType.IDLE.getIndex()) {
            // If ticks don't at least go user/nice/system/idle, abort
            return ticks;
        }
        // Note tickArr is offset by 1 because first element is "cpu"
        for (int i = 0; i < TickType.values().length; i++) {
            ticks[i] = ParseUtil.parseLongOrDefault(tickArr[i + 1], 0L);
        }
        // Ignore guest or guest_nice, they are included in user/nice
        return ticks;
    }

    /**
     * Gets an arrya of Processor CPU ticks array from /proc/stat
     *
     * @param logicalProcessorCount The number of logical processors, which corresponds to the number of lines to read
     *                              from the file.
     * @return Array of CPU ticks for each processor
     */
    public static long[][] getProcessorCpuLoadTicks(int logicalProcessorCount) {
        long[][] ticks = new long[logicalProcessorCount][TickType.values().length];
        // /proc/stat expected format
        // first line is overall user,nice,system,idle, etc.
        // cpu 3357 0 4313 1362393 ...
        // per-processor subsequent lines for cpu0, cpu1, etc.
        int cpu = 0;
        List<String> procStat = FileUtil.readFile(ProcPath.STAT);
        for (String stat : procStat) {
            if (stat.startsWith("cpu") && !stat.startsWith("cpu ")) {
                // Split the line. Note the first (0) element is "cpu" so
                // remaining
                // elements are offset by 1 from the enum index
                String[] tickArr = ParseUtil.whitespaces.split(stat);
                if (tickArr.length <= TickType.IDLE.getIndex()) {
                    // If ticks don't at least go user/nice/system/idle, abort
                    return ticks;
                }
                // Note tickArr is offset by 1
                for (int i = 0; i < TickType.values().length; i++) {
                    ticks[cpu][i] = ParseUtil.parseLongOrDefault(tickArr[i + 1], 0L);
                }
                // Ignore guest or guest_nice, they are included in
                if (++cpu >= logicalProcessorCount) {
                    break;
                }
            }
        }
        return ticks;
    }

    /**
     * Gets the number of context switches from /proc/stat
     *
     * @return The number of context switches if available, -1 otherwise
     */
    public static long getContextSwitches() {
        List<String> procStat = FileUtil.readFile(ProcPath.STAT);
        for (String stat : procStat) {
            if (stat.startsWith("ctxt ")) {
                String[] ctxtArr = ParseUtil.whitespaces.split(stat);
                if (ctxtArr.length == 2) {
                    return ParseUtil.parseLongOrDefault(ctxtArr[1], 0);
                }
            }
        }
        return 0L;
    }

    /**
     * Gets the number of interrupts from /proc/stat
     *
     * @return The number of interrupts if available, -1 otherwise
     */
    public static long getInterrupts() {
        List<String> procStat = FileUtil.readFile(ProcPath.STAT);
        for (String stat : procStat) {
            if (stat.startsWith("intr ")) {
                String[] intrArr = ParseUtil.whitespaces.split(stat);
                if (intrArr.length > 2) {
                    return ParseUtil.parseLongOrDefault(intrArr[1], 0);
                }
            }
        }
        return 0L;
    }

    /**
     * Gets the boot time from /proc/stat
     *
     * @return The boot time if available, 0 otherwise
     */
    public static long getBootTime() {
        // Boot time given by btime variable in /proc/stat.
        List<String> procStat = FileUtil.readFile(ProcPath.STAT);
        for (String stat : procStat) {
            if (stat.startsWith("btime")) {
                String[] bTime = ParseUtil.whitespaces.split(stat);
                return ParseUtil.parseLongOrDefault(bTime[1], 0L);
            }
        }
        return 0;
    }
}
