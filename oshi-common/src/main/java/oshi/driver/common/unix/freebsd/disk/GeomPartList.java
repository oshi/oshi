/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.unix.freebsd.disk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.HWPartition;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Utility to query geom part list
 */
@ThreadSafe
public final class GeomPartList {

    private static final String GEOM_PART_LIST = "geom part list";
    private static final String STAT_FILESIZE = "stat -f %i /dev/";

    private GeomPartList() {
    }

    /**
     * Queries partition data using geom, mount, and stat commands
     *
     * @return A map with disk name as the key and a List of partitions as the value
     */
    public static Map<String, List<HWPartition>> queryPartitions() {
        // FreeBSD has no major numbers. The inode of a devfs entry is the device's minor number.
        return parsePartitions(ExecutingCommand.runNative(GEOM_PART_LIST), Mount.queryPartitionToMountMap(),
                part -> ParseUtil.parseIntOrDefault(ExecutingCommand.getFirstAnswer(STAT_FILESIZE + part), 0));
    }

    /**
     * Parses the output of {@code geom part list}. Each geom lists its partitions under {@code Providers:} and the
     * device holding them under {@code Consumers:}; only the providers are partitions. A partition can itself hold a
     * scheme, as an MBR slice holds a BSD label, so its geom is named after the slice; its partitions are listed under
     * the physical disk.
     *
     * @param geom        the lines of output from {@code geom part list}
     * @param mountMap    a map of {@code /dev} device name to mount point
     * @param minorLookup returns the minor device number of a partition
     * @return A map with disk name as the key and a List of partitions, sorted by name, as the value
     */
    static Map<String, List<HWPartition>> parsePartitions(List<String> geom, Map<String, String> mountMap,
            ToIntFunction<String> minorLookup) {
        // Partitions by the geom listing them, and the geom providing each partition
        Map<String, List<HWPartition>> byGeom = new HashMap<>();
        Map<String, String> geomOf = new HashMap<>();
        PartitionBuilder part = null;
        String geomName = null;
        boolean inProviders = false;
        for (String raw : geom) {
            String line = raw.trim();
            if (line.startsWith("Geom name:") || line.equals("Providers:") || line.equals("Consumers:")) {
                addPartition(byGeom, geomOf, geomName, part, mountMap, minorLookup);
                part = null;
                inProviders = line.equals("Providers:");
                if (line.startsWith("Geom name:")) {
                    geomName = line.substring(line.lastIndexOf(' ') + 1);
                }
            } else if (inProviders && geomName != null) {
                String[] split = ParseUtil.whitespaces.split(line, -1);
                if (line.contains("Name:")) {
                    addPartition(byGeom, geomOf, geomName, part, mountMap, minorLookup);
                    part = new PartitionBuilder(split[split.length - 1]);
                } else if (part != null && split.length >= 2) {
                    if (line.startsWith("Mediasize:")) {
                        part.size = ParseUtil.parseLongOrDefault(split[1], 0L);
                    } else if (line.startsWith("rawuuid:")) {
                        part.uuid = split[1];
                    } else if (line.startsWith("type:")) {
                        part.type = split[1];
                    } else if (line.startsWith("label:") && !"(null)".equals(split[1])) {
                        part.label = split[1];
                    }
                }
            }
        }
        addPartition(byGeom, geomOf, geomName, part, mountMap, minorLookup);

        Map<String, List<HWPartition>> partitionMap = new HashMap<>();
        for (Map.Entry<String, List<HWPartition>> entry : byGeom.entrySet()) {
            partitionMap.computeIfAbsent(physicalDisk(entry.getKey(), geomOf), d -> new ArrayList<>())
                    .addAll(entry.getValue());
        }
        for (List<HWPartition> partitions : partitionMap.values()) {
            partitions.sort(Comparator.comparing(HWPartition::getName));
        }
        return partitionMap;
    }

    /** Follows a geom named after a partition, such as an MBR slice holding a BSD label, up to the disk. */
    private static String physicalDisk(String geomName, Map<String, String> geomOf) {
        String disk = geomName;
        // Bounded, in case the output is malformed into a cycle
        for (int depth = 0; depth < 8; depth++) {
            String parent = geomOf.get(disk);
            if (parent == null) {
                break;
            }
            disk = parent;
        }
        return disk;
    }

    private static void addPartition(Map<String, List<HWPartition>> byGeom, Map<String, String> geomOf,
            @Nullable String geomName, @Nullable PartitionBuilder part, Map<String, String> mountMap,
            ToIntFunction<String> minorLookup) {
        if (geomName != null && part != null) {
            geomOf.put(part.name, geomName);
            byGeom.computeIfAbsent(geomName, g -> new ArrayList<>())
                    .add(new HWPartition(part.name, part.name, part.type, part.uuid, part.label, part.size, 0,
                            minorLookup.applyAsInt(part.name), mountPoint(part, mountMap)));
        }
    }

    /** The mount point of a partition, mounted by its own name or by its GPT label or GPT ID. */
    private static String mountPoint(PartitionBuilder part, Map<String, String> mountMap) {
        String mountPoint = mountMap.get(part.name);
        if (mountPoint == null && !part.label.isEmpty()) {
            mountPoint = mountMap.get("gpt/" + part.label);
        }
        if (mountPoint == null && !Constants.UNKNOWN.equals(part.uuid)) {
            mountPoint = mountMap.get("gptid/" + part.uuid);
        }
        return mountPoint == null ? "" : mountPoint;
    }

    /** The fields of a partition collected from its lines of {@code geom} output. */
    private static final class PartitionBuilder {
        private final String name;
        private String type = Constants.UNKNOWN;
        private String uuid = Constants.UNKNOWN;
        private String label = "";
        private long size;

        private PartitionBuilder(String name) {
            this.name = name;
        }
    }
}
