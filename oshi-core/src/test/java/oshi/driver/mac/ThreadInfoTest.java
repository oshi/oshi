/*
 * Copyright 2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.mac;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.SystemInfo;

@EnabledOnOs(OS.MAC)
class ThreadInfoTest {
    @Test
    void testQueryTaskThreads() {
        int pid = new SystemInfo().getOperatingSystem().getProcessId();
        assertThat("Processes should have at least one thread.", ThreadInfo.queryTaskThreads(pid).size(),
                greaterThan(0));
    }
}
