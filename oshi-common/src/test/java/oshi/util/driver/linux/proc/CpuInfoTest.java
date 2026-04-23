/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.linux.proc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
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
    void testQueryBoardInfoReturnsQuartet() {
        Quartet<String, String, String, String> info = CpuInfo.queryBoardInfo();
        assertThat(info, is(notNullValue()));
        // On x86 systems, board info fields are typically null (ARM-specific)
        // but the quartet itself should always be non-null
    }

    @Test
    void testQueryFeatureFlags() {
        List<String> flags = CpuInfo.queryFeatureFlags();
        // On x86 Linux, /proc/cpuinfo should have a "flags" line
        assertThat(flags, hasSize(greaterThan(0)));
    }
}
