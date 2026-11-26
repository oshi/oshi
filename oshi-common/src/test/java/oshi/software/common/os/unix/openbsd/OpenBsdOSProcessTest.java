/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.openbsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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

    private static byte[] kinfoProc(int pid, long uvalid, int uSec, int uUsec, int sSec, int sUsec) {
        // Offsets of struct kinfo_proc in <sys/sysctl.h>
        ByteBuffer entry = ByteBuffer.allocate(436).order(ByteOrder.nativeOrder());
        entry.putInt(108, pid);
        entry.putLong(400, uvalid);
        entry.putInt(420, uSec);
        entry.putInt(424, uUsec);
        entry.putInt(428, sSec);
        entry.putInt(432, sUsec);
        return entry.array();
    }

    @Test
    void testParseKernelUserTime() {
        assertThat(OpenBsdOSProcess.parseKernelUserTime(kinfoProc(42, 1L, 3, 977_795, 1, 250_000), 42),
                is(new long[] { 1250L, 3977L }));
        // u_int32_t seconds past Integer.MAX_VALUE stay positive
        assertThat(OpenBsdOSProcess.parseKernelUserTime(kinfoProc(42, 1L, -1, 0, 0, 0), 42)[1], is(4_294_967_295_000L));
        // An exiting process, an entry for another process, or a short or empty read
        assertThat(OpenBsdOSProcess.parseKernelUserTime(kinfoProc(42, 0L, 3, 0, 1, 0), 42).length, is(0));
        assertThat(OpenBsdOSProcess.parseKernelUserTime(kinfoProc(43, 1L, 3, 0, 1, 0), 42).length, is(0));
        assertThat(OpenBsdOSProcess.parseKernelUserTime(new byte[400], 42).length, is(0));
        assertThat(OpenBsdOSProcess.parseKernelUserTime(new byte[0], 42).length, is(0));
    }
}
