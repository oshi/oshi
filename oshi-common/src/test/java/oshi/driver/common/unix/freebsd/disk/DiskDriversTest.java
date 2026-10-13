/*
 * Copyright 2022-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.unix.freebsd.disk;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.hardware.HWPartition;
import oshi.util.tuples.Triplet;

@EnabledOnOs(OS.FREEBSD)
class DiskDriversTest {
    @Test
    void testDiskDrivers() {
        Map<String, Triplet<String, String, Long>> diskStringMap = GeomDiskList.queryDisks();
        assertThat("Disk string map should not be empty", diskStringMap, not(anEmptyMap()));

        Map<String, List<HWPartition>> partMap = GeomPartList.queryPartitions();
        assertThat("Partition map should not be empty", partMap, not(anEmptyMap()));

        Map<String, String> deviceMap = Mount.queryPartitionToMountMap();
        Set<String> diskNames = diskStringMap.keySet();
        for (Entry<String, List<HWPartition>> entry : partMap.entrySet()) {
            assertTrue(diskNames.contains(entry.getKey()), "Disk name should be in disk string map");
            for (HWPartition part : entry.getValue()) {
                // A partition can be mounted by its own name, its GPT label (the installer mounts the EFI partition as
                // /dev/gpt/efiboot0) or its GPT ID
                String expected = deviceMap.get(part.getIdentification());
                if (expected == null && !part.getLabel().isEmpty()) {
                    expected = deviceMap.get("gpt/" + part.getLabel());
                }
                if (expected == null) {
                    expected = deviceMap.getOrDefault("gptid/" + part.getUuid(), "");
                }
                assertEquals(expected, part.getMountPoint(), "Mount point of " + part.getIdentification());
            }
        }
    }
}
