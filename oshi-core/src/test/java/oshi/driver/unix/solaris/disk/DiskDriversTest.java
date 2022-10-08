/*
 * Copyright 2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.unix.solaris.disk;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

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
    }
}
