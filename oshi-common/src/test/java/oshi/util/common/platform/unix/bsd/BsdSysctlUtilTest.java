/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.common.platform.unix.bsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import oshi.util.PlatformEnum;

/**
 * Tests the command-line {@code sysctl -n} implementation shared across all BSD-family operating systems. Enabled on
 * every platform that ships a {@code sysctl} command so both the success and failure paths of all three overloads are
 * exercised.
 */
class BsdSysctlUtilTest {

    /** Platforms that provide a {@code sysctl} command whose {@code -n} flag prints a bare value. */
    private static final Set<PlatformEnum> SYSCTL_PLATFORMS = EnumSet.of(PlatformEnum.MACOS, PlatformEnum.FREEBSD,
            PlatformEnum.OPENBSD, PlatformEnum.NETBSD, PlatformEnum.DRAGONFLYBSD);

    /**
     * Condition method for {@link EnabledIf}: true when the current platform ships {@code sysctl}.
     *
     * @return whether the sysctl command-line tests should run
     */
    static boolean hasSysctlCommand() {
        return SYSCTL_PLATFORMS.contains(PlatformEnum.getCurrentPlatform());
    }

    @Test
    @EnabledIf("hasSysctlCommand")
    void testSysctlStringSuccess() {
        // kern.ostype is present on macOS and every BSD; its value is the kernel name.
        assertThat("kern.ostype should not be empty", BsdSysctlUtil.sysctl("kern.ostype", ""), is(not("")));
    }

    @Test
    @EnabledIf("hasSysctlCommand")
    void testSysctlStringFailureReturnsDefault() {
        assertThat(BsdSysctlUtil.sysctl("invalid.bogus.name", "mydefault"), is("mydefault"));
    }

    @Test
    @EnabledIf("hasSysctlCommand")
    void testSysctlIntSuccess() {
        // hw.pagesize is a positive int on macOS and every BSD.
        assertThat("hw.pagesize should be positive", BsdSysctlUtil.sysctl("hw.pagesize", 0), greaterThan(0));
    }

    @Test
    @EnabledIf("hasSysctlCommand")
    void testSysctlIntFailureReturnsDefault() {
        assertThat(BsdSysctlUtil.sysctl("invalid.bogus.name", -99), is(-99));
    }

    @Test
    @EnabledIf("hasSysctlCommand")
    void testSysctlLongSuccess() {
        // hw.pagesize also parses as a positive long.
        assertThat("hw.pagesize should be positive", BsdSysctlUtil.sysctl("hw.pagesize", 0L), greaterThan(0L));
    }

    @Test
    @EnabledIf("hasSysctlCommand")
    void testSysctlLongFailureReturnsDefault() {
        assertThat(BsdSysctlUtil.sysctl("invalid.bogus.name", -123L), is(-123L));
    }
}
