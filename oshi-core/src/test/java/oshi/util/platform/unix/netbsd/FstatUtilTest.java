/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.unix.netbsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import oshi.SystemInfo;

/**
 * Test general utility methods for {@link FstatUtil}.
 */
@EnabledIfSystemProperty(named = "os.name", matches = "(?i)netbsd")
class FstatUtilTest {

    @Test
    void testFstat() {
        int pid = new SystemInfo().getOperatingSystem().getProcessId();

        assertThat("Number of open files must be nonnegative", FstatUtil.getOpenFiles(pid),
                is(greaterThanOrEqualTo(0L)));

        assertThat("Cwd should not be empty", FstatUtil.getCwd(pid), is(not(emptyString())));
    }
}
