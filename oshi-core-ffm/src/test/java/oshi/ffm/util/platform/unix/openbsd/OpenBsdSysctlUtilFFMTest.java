/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.util.platform.unix.openbsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions;

@EnabledOnOs(OS.OPENBSD)
class OpenBsdSysctlUtilFFMTest {

    private static final int CTL_KERN = 1;
    private static final int CTL_HW = 6;
    private static final int KERN_OSTYPE = 1;
    private static final int KERN_OSRELEASE = 2;
    private static final int HW_MACHINE = 1;
    private static final int HW_PAGESIZE = 7;
    private static final int HW_NCPUFOUND = 21;

    @Test
    void testSysctlIntReturnsDefault() {
        int[] bogus = { 99, 99, 99 };
        assertThat(OpenBsdSysctlUtilFFM.sysctl(bogus, -42), is(-42));
    }

    @Test
    void testSysctlLongReturnsDefault() {
        int[] bogus = { 99, 99, 99 };
        assertThat(OpenBsdSysctlUtilFFM.sysctl(bogus, -123L), is(-123L));
    }

    @Test
    void testSysctlStringReturnsDefault() {
        int[] bogus = { 99, 99, 99 };
        assertThat(OpenBsdSysctlUtilFFM.sysctl(bogus, "mydefault"), is("mydefault"));
    }

    @Test
    void testSysctlIntHwPagesize() {
        int[] mib = { CTL_HW, HW_PAGESIZE };
        int pageSize = OpenBsdSysctlUtilFFM.sysctl(mib, 0);
        assertThat("Page size should be positive", pageSize, greaterThan(0));
    }

    @Test
    void testSysctlIntHwNcpufound() {
        int[] mib = { CTL_HW, HW_NCPUFOUND };
        int ncpu = OpenBsdSysctlUtilFFM.sysctl(mib, 0);
        assertThat("CPU count should be at least 1", ncpu, greaterThan(0));
    }

    @Test
    void testSysctlStringKernOstype() {
        int[] mib = { CTL_KERN, KERN_OSTYPE };
        String osType = OpenBsdSysctlUtilFFM.sysctl(mib, "");
        assertThat("kern.ostype should be OpenBSD", osType, is("OpenBSD"));
    }

    @Test
    void testSysctlStringKernOsrelease() {
        int[] mib = { CTL_KERN, KERN_OSRELEASE };
        String release = OpenBsdSysctlUtilFFM.sysctl(mib, "");
        assertThat("kern.osrelease should not be empty", release, is(not("")));
    }

    @Test
    void testSysctlStringHwMachine() {
        int[] mib = { CTL_HW, HW_MACHINE };
        String machine = OpenBsdSysctlUtilFFM.sysctl(mib, "");
        assertThat("hw.machine should not be empty", machine, is(not("")));
    }

    @Test
    void testSysctlBufferReturnsNullForBogus() {
        int[] bogus = { 99, 99, 99 };
        assertThat(OpenBsdSysctlUtilFFM.sysctl(bogus), is(nullValue()));
    }

    @Test
    void testSysctlCommandLineString() {
        String osType = OpenBsdSysctlUtilFFM.sysctl("kern.ostype", "");
        assertThat("Command-line kern.ostype should be OpenBSD", osType, is("OpenBSD"));
    }

    @Test
    void testSysctlCommandLineInt() {
        int pageSize = OpenBsdSysctlUtilFFM.sysctl("hw.pagesize", 0);
        assertThat("Command-line hw.pagesize should be positive", pageSize, greaterThan(0));
    }

    @Test
    void testSysctlCommandLineLong() {
        long physmem = OpenBsdSysctlUtilFFM.sysctl("hw.physmem", 0L);
        assertThat("Command-line hw.physmem should be positive", physmem, greaterThan(0L));
    }

    @Test
    void testGetrlimit() throws Throwable {
        java.lang.foreign.Arena arena = java.lang.foreign.Arena.ofConfined();
        java.lang.foreign.MemorySegment rlim = arena.allocate(OpenBsdLibcFunctions.RLIMIT_LAYOUT);
        int ret = OpenBsdLibcFunctions.getrlimit(OpenBsdLibcFunctions.RLIMIT_NOFILE, rlim);
        assertThat("getrlimit should succeed", ret, is(0));
        long cur = OpenBsdLibcFunctions.rlimitCur(rlim);
        long max = OpenBsdLibcFunctions.rlimitMax(rlim);
        assertThat("soft limit should be positive", cur, greaterThan(0L));
        assertThat("hard limit should be >= soft limit", max >= cur, is(true));
        arena.close();
    }

    @Test
    void testGetthrid() throws Throwable {
        int tid = OpenBsdLibcFunctions.getthrid();
        assertThat("Thread ID should be positive", tid, greaterThan(0));
    }
}
