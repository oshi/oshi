/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.linux.proc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.util.driver.linux.proc.DiskStats.IoStat;

class DiskStatsTest {

    @Test
    void testParseDiskStats() {
        // /proc/diskstats: major minor name reads reads_merged sectors_read ms_read writes writes_merged ...
        List<String> diskstats = Arrays.asList("   8       0 sda 100 5 2000 50 200 10 4000 80 0 130 130",
                "   8       1 sda1 40 2 800 20 90 3 1200 30 0 45 45");
        Map<String, Map<IoStat, Long>> map = DiskStats.parseDiskStats(diskstats);
        assertThat(map.keySet(), containsInAnyOrder("sda", "sda1"));
        Map<IoStat, Long> sda = map.get("sda");
        assertThat(sda.get(IoStat.MAJOR), is(8L));
        assertThat(sda.get(IoStat.READS), is(100L));
        assertThat(sda.get(IoStat.READS_SECTOR), is(2000L));
        assertThat(sda.get(IoStat.WRITES), is(200L));
        assertThat(sda.get(IoStat.WRITES_SECTOR), is(4000L));
        // NAME is used as the map key, not stored in the per-device map
        assertThat(sda.containsKey(IoStat.NAME), is(false));
        assertThat(map.get("sda1").get(IoStat.READS), is(40L));
    }

    @Test
    void testParseDiskStatsEmpty() {
        assertThat(DiskStats.parseDiskStats(Collections.emptyList()), is(anEmptyMap()));
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void testGetDiskStats() {
        Map<String, Map<IoStat, Long>> map = DiskStats.getDiskStats();
        assertNotNull(map, "DiskStats should not be null");

        map.forEach((key, value) -> {
            assertNotNull(value, "Entry should not have a null map!");
            assertInstanceOf(EnumMap.class, value, "Value should be enum map!");
        });
    }
}
