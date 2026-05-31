/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.unix.bsd.disk;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.HWPartition;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Quartet;

/**
 * Utility class parsing partition information from the BSD {@code disklabel} command. Shared by OpenBSD and NetBSD,
 * which produce the same disklabel output format.
 */
@ThreadSafe
public final class Disklabel {

    private static final String TOTAL_MARKER = "total sectors:";
    private static final String BPS_MARKER = "bytes/sector:";
    private static final String LABEL_MARKER = "label:";
    private static final String DUID_MARKER = "duid:";

    private Disklabel() {
    }

    /**
     * Gets disk and partition information by running {@code disklabel} and parsing its output. Falls back to {@code df}
     * if no partitions are returned (e.g., when not running as root).
     *
     * @param diskName The disk to fetch partition information from
     * @return A quartet containing the disk's name/label, DUID, size, and a list of partitions
     */
    public static Quartet<String, String, Long, List<HWPartition>> getDiskParams(String diskName) {
        Quartet<String, String, Long, List<HWPartition>> result = parseDiskParams(diskName,
                ExecutingCommand.runNative("disklabel -n " + diskName), Disklabel::getMajorMinor);
        if (result.getD().isEmpty()) {
            return parseDfFallback(diskName, ExecutingCommand.runNative("df"), Disklabel::getMajorMinor);
        }
        return result;
    }

    /**
     * Parses {@code disklabel -n} output to extract disk metadata and partitions.
     *
     * @param diskName         the disk being labeled
     * @param disklabelLines   raw output from {@code disklabel -n <diskName>}
     * @param majorMinorLookup function that resolves a (disk, partition-letter) pair to a (major, minor) device number
     *                         pair; tests may pass a stub that returns (0,0) without invoking {@code stat}.
     * @return A quartet of label, DUID, size in bytes, and partitions. An empty partition list signals the caller
     *         should fall back to {@link #parseDfFallback}.
     */
    public static Quartet<String, String, Long, List<HWPartition>> parseDiskParams(String diskName,
            List<String> disklabelLines, BiFunction<String, String, Pair<Integer, Integer>> majorMinorLookup) {
        // disklabel (requires root) supports 15 configurable partitions, `a' through `p', excluding `c'.
        // The `c' partition describes the entire physical disk. By convention `a' of the boot disk is root,
        // `b' is swap, and `i' is usually the boot record.
        List<HWPartition> partitions = new ArrayList<>();
        long totalSectors = 1L;
        int bytesPerSector = 1;
        String label = "";
        String duid = "";
        for (String line : disklabelLines) {
            if (line.contains(TOTAL_MARKER)) {
                // Parse as long to avoid int overflow for disks larger than ~2 TiB at 512-byte sectors.
                totalSectors = ParseUtil.parseLongOrDefault(line.split(TOTAL_MARKER)[1].trim(), 1L);
            } else if (line.contains(BPS_MARKER)) {
                bytesPerSector = ParseUtil.getFirstIntValue(line);
            } else if (line.contains(LABEL_MARKER)) {
                label = line.split(LABEL_MARKER)[1].trim();
            } else if (line.contains(DUID_MARKER)) {
                duid = line.split(DUID_MARKER)[1].trim();
            }
            if (line.trim().indexOf(':') == 1) {
                // Partition rows have a single letter followed by a colon:
                // a: 2097152 1024 4.2BSD 2048 16384 12958 # /
                String[] split = ParseUtil.whitespaces.split(line.trim(), 9);
                String name = split[0].substring(0, 1);
                Pair<Integer, Integer> majorMinor = majorMinorLookup.apply(diskName, name);
                if (split.length > 4) {
                    partitions.add(new HWPartition(diskName + name, name, split[3], duid + "." + name,
                            ParseUtil.parseLongOrDefault(split[1], 0L) * bytesPerSector, majorMinor.getA(),
                            majorMinor.getB(), split.length > 5 ? split[split.length - 1] : ""));
                }
            }
        }
        return new Quartet<>(label, duid, totalSectors * bytesPerSector, partitions);
    }

    /**
     * Parses {@code df} output as a fallback when {@code disklabel} returns no partitions (typically because the caller
     * lacks root privileges).
     *
     * @param diskName         the disk whose partitions are being enumerated
     * @param dfLines          raw output from {@code df}
     * @param majorMinorLookup function that resolves a (disk, partition-letter) pair to a (major, minor) device number
     *                         pair
     * @return A quartet of UNKNOWN label/DUID, 0 size, and partitions discovered from {@code df}.
     */
    public static Quartet<String, String, Long, List<HWPartition>> parseDfFallback(String diskName,
            List<String> dfLines, BiFunction<String, String, Pair<Integer, Integer>> majorMinorLookup) {
        List<HWPartition> partitions = new ArrayList<>();
        for (String line : dfLines) {
            if (line.startsWith("/dev/" + diskName)) {
                String[] split = ParseUtil.whitespaces.split(line);
                String name = split[0].substring(5 + diskName.length());
                Pair<Integer, Integer> majorMinor = majorMinorLookup.apply(diskName, name);
                if (split.length > 5) {
                    long partSize = ParseUtil.parseLongOrDefault(split[1], 1L) * 512L;
                    partitions.add(new HWPartition(split[0], split[0].substring(5), Constants.UNKNOWN,
                            Constants.UNKNOWN, partSize, majorMinor.getA(), majorMinor.getB(), split[5]));
                }
            }
        }
        return new Quartet<>(Constants.UNKNOWN, Constants.UNKNOWN, 0L, partitions);
    }

    private static Pair<Integer, Integer> getMajorMinor(String diskName, String name) {
        int major = 0;
        int minor = 0;
        String majorMinor = ExecutingCommand.getFirstAnswer("stat -f %Hr,%Lr /dev/" + diskName + name);
        int comma = majorMinor.indexOf(',');
        if (comma > 0 && comma < majorMinor.length()) {
            major = ParseUtil.parseIntOrDefault(majorMinor.substring(0, comma), 0);
            minor = ParseUtil.parseIntOrDefault(majorMinor.substring(comma + 1), 0);
        }
        return new Pair<>(major, minor);
    }
}
