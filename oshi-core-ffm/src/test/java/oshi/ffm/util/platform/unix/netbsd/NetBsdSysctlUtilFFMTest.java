/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.util.platform.unix.netbsd;

import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.foreign.MemorySegment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import oshi.ffm.platform.unix.netbsd.NetBsdLibcFunctions;

// JUnit's OS enum has no NetBSD constant
@EnabledIfSystemProperty(named = "os.name", matches = "(?i)netbsd")
class NetBsdSysctlUtilFFMTest {

    private static final int CTL_HW = 6;
    private static final int KERN_OSTYPE = 1;
    private static final int HW_NCPU = 3;
    private static final int HW_PAGESIZE = 7;
    private static final int CPUSTATES = 5;

    @Test
    void testFfmAvailable() {
        assertTrue(NetBsdSysctlUtilFFM.FFM_AVAILABLE, "FFM native access should work on a HotSpot JDK");
    }

    @Test
    void testSysctlReturnsDefault() {
        int[] bogus = { 99, 99, 99 };
        assertThat(NetBsdSysctlUtilFFM.sysctl(bogus, -42), is(-42));
        assertThat(NetBsdSysctlUtilFFM.sysctl(bogus, -123L), is(-123L));
        assertThat(NetBsdSysctlUtilFFM.sysctl(bogus, "mydefault"), is("mydefault"));
    }

    @Test
    void testSysctlInt() {
        assertThat(NetBsdSysctlUtilFFM.sysctl(new int[] { CTL_HW, HW_NCPU }, 0), greaterThan(0));
        assertThat(NetBsdSysctlUtilFFM.sysctl(new int[] { CTL_HW, HW_PAGESIZE }, 0), greaterThan(0));
        assertThat(NetBsdSysctlUtilFFM.sysctl(
                new int[] { NetBsdLibcFunctions.CTL_KERN, NetBsdLibcFunctions.KERN_ARGMAX }, 0), greaterThan(0));
    }

    @Test
    void testSysctlString() {
        assertThat(NetBsdSysctlUtilFFM.sysctl(new int[] { NetBsdLibcFunctions.CTL_KERN, KERN_OSTYPE }, ""),
                is("NetBSD"));
    }

    @Test
    void testSysctlCpTime() {
        MemorySegment cpTime = NetBsdSysctlUtilFFM
                .sysctl(new int[] { NetBsdLibcFunctions.CTL_KERN, NetBsdLibcFunctions.KERN_CP_TIME });
        assertNotNull(cpTime);
        assertThat(cpTime.byteSize(), is(CPUSTATES * JAVA_LONG.byteSize()));
    }
}
