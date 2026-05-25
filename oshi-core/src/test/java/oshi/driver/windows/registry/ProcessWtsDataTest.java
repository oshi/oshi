/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.registry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.SystemInfo;
import oshi.driver.common.windows.registry.WtsInfo;

@EnabledOnOs(OS.WINDOWS)
class ProcessWtsDataTest {

    @Test
    void testQueryProcessWtsMapFromPerfMon() {
        int pid = new SystemInfo().getOperatingSystem().getProcessId();
        Map<Integer, WtsInfo> wtsMap = ProcessWtsData.queryProcessWtsMapFromPerfMon(Collections.singleton(pid));
        assertThat("Process WTS map should include the current process", wtsMap.containsKey(pid), is(true));
        assertWtsInfo(wtsMap.get(pid));
    }

    private static void assertWtsInfo(WtsInfo info) {
        assertThat("WTS info should not be null", info, is(notNullValue()));
        assertThat("Process name should not be null", info.getName(), is(notNullValue()));
        assertThat("Thread count should be positive", info.getThreadCount(), is(greaterThan(0)));
        assertThat("Virtual size should be nonnegative", info.getVirtualSize(), is(greaterThanOrEqualTo(0L)));
        assertThat("Kernel time should be nonnegative", info.getKernelTime(), is(greaterThanOrEqualTo(0L)));
        assertThat("User time should be nonnegative", info.getUserTime(), is(greaterThanOrEqualTo(0L)));
        assertThat("Open files should be nonnegative", info.getOpenFiles(), is(greaterThanOrEqualTo(0L)));
    }
}
