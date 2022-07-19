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
package oshi.driver.unix.solaris.disk;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.hardware.HWPartition;
import oshi.software.os.unix.solaris.SolarisOperatingSystem;
import oshi.util.tuples.Quintet;

@EnabledOnOs(OS.SOLARIS)
class DiskDriversTest {
    @Test
    void testDiskQueries() {
        Map<String, String> deviceMap = Iostat.queryPartitionToMountMap();
        assertThat("Partition to mount map should not be empty", deviceMap, is(not(anEmptyMap())));

        Map<String, Quintet<String, String, String, String, Long>> deviceStringMap = Iostat
                .queryDeviceStrings(deviceMap.keySet());
        assertThat("Device string map should not be empty", deviceStringMap, is(not(anEmptyMap())));

        // For partitions, requires root permissions
        if (new SolarisOperatingSystem().isElevated()) {
            for (String disk : deviceStringMap.keySet()) {
                List<HWPartition> partList = Prtvtoc.queryPartitions(disk, 0);
                assertThat("Partition List should not be empty", partList.size(), greaterThan(0));
            }
        }
    }
}
