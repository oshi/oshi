/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

class LinuxVirtualMemoryTest {

    @Test
    void testParseMemInfoSwap() {
        // kB values are multiplied by 1024; used swap = total - free
        List<String> meminfo = Arrays.asList("MemTotal:       16000000 kB", "SwapTotal:       2000000 kB",
                "SwapFree:         500000 kB", "CommitLimit:     8000000 kB");
        Triplet<Long, Long, Long> t = LinuxVirtualMemory.parseMemInfo(meminfo);
        assertThat(t.getA(), is(1_500_000L * 1024));
        assertThat(t.getB(), is(2_000_000L * 1024));
        assertThat(t.getC(), is(8_000_000L * 1024));
    }

    @Test
    void testParseMemInfoEmpty() {
        Triplet<Long, Long, Long> t = LinuxVirtualMemory.parseMemInfo(Collections.emptyList());
        assertThat(t.getA(), is(0L));
        assertThat(t.getB(), is(0L));
        assertThat(t.getC(), is(0L));
    }

    @Test
    void testParseVmStat() {
        List<String> vmstat = Arrays.asList("pgpgin 12345", "pswpin 100", "pswpout 200", "pgfault 999");
        Pair<Long, Long> p = LinuxVirtualMemory.parseVmStat(vmstat);
        assertThat(p.getA(), is(100L));
        assertThat(p.getB(), is(200L));
    }

    @Test
    void testParseVmStatEmpty() {
        Pair<Long, Long> p = LinuxVirtualMemory.parseVmStat(Collections.emptyList());
        assertThat(p.getA(), is(0L));
        assertThat(p.getB(), is(0L));
    }
}
