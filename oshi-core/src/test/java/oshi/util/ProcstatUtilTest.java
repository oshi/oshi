/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;

import oshi.PlatformEnum;
import oshi.SystemInfo;
import oshi.util.platform.unix.freebsd.ProcstatUtil;

/**
 * Test general utility methods
 */
class ProcstatUtilTest {

    @Test
    void testProcstat() {
        if (SystemInfo.getCurrentPlatform().equals(PlatformEnum.FREEBSD)) {
            int pid = new SystemInfo().getOperatingSystem().getProcessId();

            assertThat("Open files must be nonnegative", ProcstatUtil.getOpenFiles(pid), is(greaterThanOrEqualTo(0L)));

            assertThat("CwdMap should have at least one element", ProcstatUtil.getCwdMap(-1), is(not(anEmptyMap())));

            assertThat("CwdMap with pid should have at least one element", ProcstatUtil.getCwdMap(pid),
                    is(not(anEmptyMap())));

            assertThat("Cwd should be nonempty", ProcstatUtil.getCwd(pid), is(not(emptyString())));
        }
    }
}
