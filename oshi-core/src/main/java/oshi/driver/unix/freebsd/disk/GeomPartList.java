/*
 * MIT License
 *
 * Copyright (c) 2010 - 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.driver.unix.freebsd.disk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        Map<String, String> mountMap = Mount.queryPartitionToMountMap();
        // Map of device name to partitions, to be returned
        Map<String, List<HWPartition>> partitionMap = new HashMap<>();
        // The Disk Store associated with a partition, key to the map
        String diskName = null;
        // List to hold partitions, will be added as value to the map
        List<HWPartition> partList = new ArrayList<>();
        // Parameters needed for constructor.
        String partName = null; // Non-null identifies a valid partition
        String identification = Constants.UNKNOWN;
        String type = Constants.UNKNOWN;
        String uuid = Constants.UNKNOWN;
        long size = 0;
        String mountPoint = "";

        List<String> geom = ExecutingCommand.runNative(GEOM_PART_LIST);
        for (String line : geom) {
            line = line.trim();
            // Marks the DiskStore device for a partition.
            if (line.startsWith("Geom name:")) {
                // Save any previous partition list in the map
                if (diskName != null && !partList.isEmpty()) {
                    // Store map (old diskName)
                    partitionMap.put(diskName, partList);
                    // Reset the list
                    partList = new ArrayList<>();
                }
                // Now use new diskName
                diskName = line.substring(line.lastIndexOf(' ') + 1);
            }
            // If we don't have a valid store, don't bother parsing anything
            if (diskName != null) {
                // Marks the beginning of partition data
                if (line.contains("Name:")) {
                    // Add the current partition to the list, if any
                    if (partName != null) {
                        // FreeBSD Major # is 0.
                        // Minor # is filesize of /dev entry.
                        int minor = ParseUtil
                                .parseIntOrDefault(ExecutingCommand.getFirstAnswer(STAT_FILESIZE + partName), 0);
                        partList.add(new HWPartition(identification, partName, type, uuid, size, 0, minor, mountPoint));
                        partName = null;
                        identification = Constants.UNKNOWN;
                        type = Constants.UNKNOWN;
                        uuid = Constants.UNKNOWN;
                        size = 0;
                    }
                    // Verify new entry is a partition
                    // (will happen in 'providers' section)
                    String part = line.substring(line.lastIndexOf(' ') + 1);
                    if (part.startsWith(diskName)) {
                        partName = part;
                        identification = part;
                        mountPoint = mountMap.getOrDefault(part, "");
                    }
                }
                // If we don't have a valid partition, don't parse anything until we do.
                if (partName != null) {
                    String[] split = ParseUtil.whitespaces.split(line);
                    if (split.length >= 2) {
                        if (line.startsWith("Mediasize:")) {
                            size = ParseUtil.parseLongOrDefault(split[1], 0L);
                        } else if (line.startsWith("rawuuid:")) {
                            uuid = split[1];
                        } else if (line.startsWith("type:")) {
                            type = split[1];
                        }
                    }
                }
            }
        }
        if (diskName != null) {
            // Process last partition
            if (partName != null) {
                int minor = ParseUtil.parseIntOrDefault(ExecutingCommand.getFirstAnswer(STAT_FILESIZE + partName), 0);
                partList.add(new HWPartition(identification, partName, type, uuid, size, 0, minor, mountPoint));
            }
            // Process last diskstore
            if (!partList.isEmpty()) {
                partList = partList.stream().sorted(Comparator.comparing(HWPartition::getName))
                        .collect(Collectors.toList());
                partitionMap.put(diskName, partList);
            }
        }
        return partitionMap;
    }
}
