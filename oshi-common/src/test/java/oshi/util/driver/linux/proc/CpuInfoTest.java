/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.linux.proc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.util.tuples.Quartet;

@EnabledOnOs(OS.LINUX)
class CpuInfoTest {
    @Test
    void testQueries() {
        assertDoesNotThrow(CpuInfo::queryCpuManufacturer);
        assertDoesNotThrow(CpuInfo::queryBoardInfo);
    }

    @Test
    void testQueryBoardInfoAccessors() {
        Quartet<String, String, String, String> info = CpuInfo.queryBoardInfo();
        // On x86 these are typically null (ARM-specific fields), but accessors must not throw
        assertDoesNotThrow(info::getA);
        assertDoesNotThrow(info::getB);
        assertDoesNotThrow(info::getC);
        assertDoesNotThrow(info::getD);
    }

    @Test
    void testQueryFeatureFlagLines() {
        // Returns deduplicated full lines from /proc/cpuinfo starting with "flags" or "features"
        List<String> flagLines = CpuInfo.queryFeatureFlags();
        assertThat(flagLines, hasSize(greaterThan(0)));
    }
}
