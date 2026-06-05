/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.unix.solaris;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@EnabledOnOs(OS.SOLARIS)
class SolarisLibcFunctionsTest {

    @Test
    void testGetpid() throws Throwable {
        int pid = SolarisLibcFunctions.getpid();
        assertThat("Process ID should be positive", pid, greaterThan(0));
    }

    @Test
    void testThrSelf() throws Throwable {
        int tid = SolarisLibcFunctions.thr_self();
        assertThat("Thread ID should be positive", tid, greaterThan(0));
    }

    @Test
    void testGetrlimit() throws Throwable {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment rlim = arena.allocate(SolarisLibcFunctions.RLIMIT_LAYOUT);
            int ret = SolarisLibcFunctions.getrlimit(SolarisLibcFunctions.RLIMIT_NOFILE, rlim);
            assertThat("getrlimit should succeed", ret, is(0));
            long cur = SolarisLibcFunctions.rlimitCur(rlim);
            long max = SolarisLibcFunctions.rlimitMax(rlim);
            assertThat("soft limit should be positive", cur, greaterThan(0L));
            assertThat("hard limit should be >= soft limit", max >= cur, is(true));
        }
    }
}
