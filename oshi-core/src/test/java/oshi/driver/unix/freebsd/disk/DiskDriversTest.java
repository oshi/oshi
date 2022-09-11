/*
 * MIT License
 *
 * Copyright (c) 2022 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.not;
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
        Set<String> mountedPartitions = deviceMap.keySet();
        for (Entry<String, List<HWPartition>> entry : partMap.entrySet()) {
            assertTrue(diskNames.contains(entry.getKey()), "Disk name should be in disk string map");
            for (HWPartition part : entry.getValue()) {
                if (!part.getMountPoint().isEmpty()) {
                    assertTrue(mountedPartitions.contains(entry.getKey()), "Partition name should be in mount map");
                }
            }
        }
    }
}
