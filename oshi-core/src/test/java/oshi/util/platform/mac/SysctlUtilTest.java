/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.mac;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@EnabledOnOs(OS.MAC)
class SysctlUtilTest {

    @Test
    void testSysctlStringFailureReturnsDefault() {
        assertThat(SysctlUtil.sysctl("invalid.bogus.name", "mydefault"), is("mydefault"));
    }

    @Test
    void testSysctlStringNoWarnFailureReturnsDefault() {
        assertThat(SysctlUtil.sysctl("invalid.bogus.name", "fallback", false), is("fallback"));
    }

    @Test
    void testSysctlIntFailureReturnsDefault() {
        assertThat(SysctlUtil.sysctl("invalid.bogus.name", -99), is(-99));
    }

    @Test
    void testSysctlLongFailureReturnsDefault() {
        assertThat(SysctlUtil.sysctl("invalid.bogus.name", -123L), is(-123L));
    }
}
