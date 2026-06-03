/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.util.platform.unix.freebsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@EnabledOnOs(OS.FREEBSD)
class BsdSysctlUtilFFMTest {

    @Test
    void testSysctlStringFailureReturnsDefault() {
        assertThat(BsdSysctlUtilFFM.sysctl("invalid.bogus.name", "mydefault"), is("mydefault"));
    }

    @Test
    void testSysctlIntFailureReturnsDefault() {
        assertThat(BsdSysctlUtilFFM.sysctl("invalid.bogus.name", -99), is(-99));
    }

    @Test
    void testSysctlLongFailureReturnsDefault() {
        assertThat(BsdSysctlUtilFFM.sysctl("invalid.bogus.name", -123L), is(-123L));
    }
}
