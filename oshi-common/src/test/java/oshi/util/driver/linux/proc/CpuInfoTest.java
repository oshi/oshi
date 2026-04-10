/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.linux.proc;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@EnabledOnOs(OS.LINUX)
class CpuInfoTest {
    @Test
    void testQueries() {
        assertDoesNotThrow(CpuInfo::queryCpuManufacturer);
        assertDoesNotThrow(CpuInfo::queryBoardInfo);
    }
}
