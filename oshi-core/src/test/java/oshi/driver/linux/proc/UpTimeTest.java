/*
 * Copyright 2021-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.linux.proc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@EnabledOnOs(OS.LINUX)
class UpTimeTest {

    @Test
    void testGetSystemUptimeSeconds() {
        double uptime = UpTime.getSystemUptimeSeconds();
        assertThat("Uptime should be nonnegative", uptime, greaterThanOrEqualTo(0d));
    }
}
