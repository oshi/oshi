/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.openbsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

class OpenBsdOSProcessTest {

    // A kern.cpustats record: six cs_time values followed by cs_flags
    private static long[] cpuStats(boolean online) {
        return new long[] { 1L, 2L, 3L, 4L, 5L, 6L, online ? 0x1L : 0x0L };
    }

    @Test
    void testOnlineCpuMask() {
        // Every processor online
        assertThat(OpenBsdOSProcess.onlineCpuMask(4, cpu -> cpuStats(true)), is(0b1111L));
        // hw.smt=0 takes the odd-numbered SMT siblings offline
        assertThat(OpenBsdOSProcess.onlineCpuMask(4, cpu -> cpuStats(cpu % 2 == 0)), is(0b0101L));
        // An unreadable record leaves its processor out; none readable is 0
        assertThat(OpenBsdOSProcess.onlineCpuMask(3, cpu -> cpu == 1 ? new long[0] : cpuStats(true)), is(0b101L));
        assertThat(OpenBsdOSProcess.onlineCpuMask(4, cpu -> new long[0]), is(0L));
        assertThat(OpenBsdOSProcess.onlineCpuMask(0, cpu -> cpuStats(true)), is(0L));
        // More processors than the mask holds
        assertThat(OpenBsdOSProcess.onlineCpuMask(80, cpu -> cpuStats(true)), is(-1L));
    }
}
