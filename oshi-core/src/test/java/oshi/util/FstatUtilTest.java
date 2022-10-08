/*
 * Copyright 2021-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static oshi.PlatformEnum.OPENBSD;

import org.junit.jupiter.api.Test;

import oshi.SystemInfo;
import oshi.util.platform.unix.openbsd.FstatUtil;

/**
 * Test general utility methods for {@link FstatUtil}.
 */
class FstatUtilTest {

    @Test
    void testFstat() {
        if (SystemInfo.getCurrentPlatform().equals(OPENBSD)) {
            int pid = new SystemInfo().getOperatingSystem().getProcessId();

            assertThat("Number of open files must be nonnegative", FstatUtil.getOpenFiles(pid),
                    is(greaterThanOrEqualTo(0L)));

            assertThat("Cwd should not be empty", FstatUtil.getCwd(pid), is(not(emptyString())));
        }
    }
}
