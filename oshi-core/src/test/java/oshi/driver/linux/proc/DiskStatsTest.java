/*
 * Copyright 2021-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.linux.proc;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.EnumMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@EnabledOnOs(OS.LINUX)
class DiskStatsTest {

    @Test
    void testGetDiskStats() {
        Map<String, Map<DiskStats.IoStat, Long>> map = DiskStats.getDiskStats();
        assertNotNull(map, "DiskStats should not be null");

        map.forEach((key, value) -> {
            assertNotNull(value, "Entry should not have a null map!");
            assertInstanceOf(EnumMap.class, value, "Value should be enum map!");
        });
    }
}
