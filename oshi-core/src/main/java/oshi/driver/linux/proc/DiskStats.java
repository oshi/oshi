/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.linux.proc;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.platform.linux.ProcPath;

/**
 * Utility to read disk statistics from {@code /proc/diskstats}
 */
@ThreadSafe
public final class DiskStats {

    /**
     * Enum corresponding to the fields in the output of {@code /proc/diskstats}
     */
    public enum IoStat {
        /**
         * The device major number.
         */
        MAJOR,
        /**
         * The device minor number.
         */
        MINOR,
        /**
         * The device name.
         */
        NAME,
        /**
         * The total number of reads completed successfully.
         */
        READS,
        /**
         * Reads which are adjacent to each other merged for efficiency.
         */
        READS_MERGED,
        /**
         * The total number of sectors read successfully.
         */
        READS_SECTOR,
        /**
         * The total number of milliseconds spent by all reads.
         */
        READS_MS,
        /**
         * The total number of writes completed successfully.
         */
        WRITES,
        /**
         * Writes which are adjacent to each other merged for efficiency.
         */
        WRITES_MERGED,
        /**
         * The total number of sectors written successfully.
         */
        WRITES_SECTOR,
        /**
         * The total number of milliseconds spent by all writes.
         */
        WRITES_MS,
        /**
         * Incremented as requests are given to appropriate struct request_queue and decremented as they finish.
         */
        IO_QUEUE_LENGTH,
        /**
         * The total number of milliseconds spent doing I/Os.
         */
        IO_MS,
        /**
         * Incremented at each I/O start, I/O completion, I/O merge, or read of these stats by the number of I/Os in
         * progress {@link #IO_QUEUE_LENGTH} times the number of milliseconds spent doing I/O since the last update of
         * this field.
         */
        IO_MS_WEIGHTED,
        /**
         * The total number of discards completed successfully.
         */
        DISCARDS,
        /**
         * Discards which are adjacent to each other merged for efficiency.
         */
        DISCARDS_MERGED,
        /**
         * The total number of sectors discarded successfully.
         */
        DISCARDS_SECTOR,
        /**
         * The total number of milliseconds spent by all discards.
         */
        DISCARDS_MS,
        /**
         * The total number of flush requests completed successfully.
         */
        FLUSHES,
        /**
         * The total number of milliseconds spent by all flush requests.
         */
        FLUSHES_MS;
    }

    private DiskStats() {
    }

    /**
     * Reads the statistics in {@code /proc/diskstats} and returns the results.
     *
     * @return A map with each disk's name as the key, and an EnumMap as the value, where the numeric values in
     *         {@link IoStat} are mapped to a {@link Long} value.
     */
    public static Map<String, Map<IoStat, Long>> getDiskStats() {
        Map<String, Map<IoStat, Long>> diskStatMap = new HashMap<>();
        IoStat[] enumArray = IoStat.class.getEnumConstants();
        List<String> diskStats = FileUtil.readFile(ProcPath.DISKSTATS);
        for (String stat : diskStats) {
            String[] split = ParseUtil.whitespaces.split(stat.trim());
            Map<IoStat, Long> statMap = new EnumMap<>(IoStat.class);
            String name = null;
            for (int i = 0; i < enumArray.length && i < split.length; i++) {
                if (enumArray[i] == IoStat.NAME) {
                    name = split[i];
                } else {
                    statMap.put(enumArray[i], ParseUtil.parseLongOrDefault(split[i], 0L));
                }
            }
            if (name != null) {
                diskStatMap.put(name, statMap);
            }
        }
        return diskStatMap;
    }
}
