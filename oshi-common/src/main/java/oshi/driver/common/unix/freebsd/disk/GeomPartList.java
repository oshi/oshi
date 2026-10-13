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
     * Parses the output of {@code geom part list}. Each geom lists its partitions under {@code Providers:} and the disk
     * holding them under {@code Consumers:}; only the providers are partitions.
     *
     * @param geom        the lines of output from {@code geom part list}
     * @param mountMap    a map of partition name to mount point
     * @param minorLookup returns the minor device number of a partition
     * @return A map with disk name as the key and a List of partitions, sorted by name, as the value
     */
    static Map<String, List<HWPartition>> parsePartitions(List<String> geom, Map<String, String> mountMap,
            ToIntFunction<String> minorLookup) {
        Map<String, List<HWPartition>> partitionMap = new HashMap<>();
        PartitionBuilder part = null;
        String diskName = null;
        boolean inProviders = false;
        for (String raw : geom) {
            String line = raw.trim();
            if (line.startsWith("Geom name:") || line.equals("Providers:") || line.equals("Consumers:")) {
                addPartition(partitionMap, diskName, part, mountMap, minorLookup);
                part = null;
                inProviders = line.equals("Providers:");
                if (line.startsWith("Geom name:")) {
                    diskName = line.substring(line.lastIndexOf(' ') + 1);
                }
            } else if (inProviders && diskName != null) {
                String[] split = ParseUtil.whitespaces.split(line, -1);
                if (line.contains("Name:")) {
                    addPartition(partitionMap, diskName, part, mountMap, minorLookup);
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
        addPartition(partitionMap, diskName, part, mountMap, minorLookup);
        for (List<HWPartition> partitions : partitionMap.values()) {
            partitions.sort(Comparator.comparing(HWPartition::getName));
        }
        return partitionMap;
    }

    private static void addPartition(Map<String, List<HWPartition>> partitionMap, @Nullable String diskName,
            @Nullable PartitionBuilder part, Map<String, String> mountMap, ToIntFunction<String> minorLookup) {
        if (diskName != null && part != null) {
            partitionMap.computeIfAbsent(diskName, d -> new ArrayList<>())
                    .add(new HWPartition(part.name, part.name, part.type, part.uuid, part.label, part.size, 0,
                            minorLookup.applyAsInt(part.name), mountMap.getOrDefault(part.name, "")));
        }
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
