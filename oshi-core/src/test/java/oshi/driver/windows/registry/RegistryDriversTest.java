/*
 * Copyright 2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.registry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@EnabledOnOs(OS.WINDOWS)
class RegistryDriversTest {

    @Test
    void testProcessPerformanceData() {
        Map<Integer, ProcessPerformanceData.PerfCounterBlock> processMap = ProcessPerformanceData
                .buildProcessMapFromRegistry(null);
        assertNotNull(processMap);
        assertThat("Process map should not be empty", processMap, is(not(anEmptyMap())));
    }

    @Test
    void testThreadPerformanceData() {
        Map<Integer, ThreadPerformanceData.PerfCounterBlock> threadMap = ThreadPerformanceData
                .buildThreadMapFromRegistry(null);
        assertNotNull(threadMap);
        assertThat("Thread map should not be empty", threadMap, is(not(anEmptyMap())));
    }

    @Test
    void testSessionWtsData() {
        assertThat("Sessions list from registry should not be empty", HkeyUserData.queryUserSessions(),
                is(not(empty())));
        assertDoesNotThrow(SessionWtsData::queryUserSessions);
        assertDoesNotThrow(NetSessionData::queryUserSessions);
    }

    @Test
    void testProcessWtsData() {
        assertThat("Process WTS map should not be empty", ProcessWtsData.queryProcessWtsMap(null),
                is(not(anEmptyMap())));
    }
}
