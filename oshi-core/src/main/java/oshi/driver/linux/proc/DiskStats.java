/*
 * MIT License
 *
 * Copyright (c) 2020-2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
         * Incremented as requests are given to appropriate struct request_queue and
         * decremented as they finish.
         */
        IO_QUEUE_LENGTH,
        /**
         * The total number of milliseconds spent doing I/Os.
         */
        IO_MS,
        /**
         * Incremented at each I/O start, I/O completion, I/O merge, or read of these
         * stats by the number of I/Os in progress {@link #IO_QUEUE_LENGTH} times the
         * number of milliseconds spent doing I/O since the last update of this field.
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
     * @return A map with each disk's name as the key, and an EnumMap as the value,
     *         where the numeric values in {@link IoStat} are mapped to a
     *         {@link Long} value.
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
