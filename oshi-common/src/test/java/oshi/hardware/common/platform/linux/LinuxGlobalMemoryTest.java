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

import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;

class LinuxGlobalMemoryTest {

    @Test
    void testParseMemInfoUsesMemAvailable() {
        List<String> meminfo = Arrays.asList("MemTotal:       16000000 kB", "MemFree:         1000000 kB",
                "MemAvailable:    8000000 kB", "Active(file):    2000000 kB", "Inactive(file):  1000000 kB",
                "SReclaimable:     500000 kB");
        Pair<Long, Long> p = LinuxGlobalMemory.parseMemInfo(meminfo);
        assertThat(p.getA(), is(ParseUtil.parseDecimalMemorySizeToBinary("8000000 kB")));
        assertThat(p.getB(), is(ParseUtil.parseDecimalMemorySizeToBinary("16000000 kB")));
    }

    @Test
    void testParseMemInfoFallbackSumWhenNoMemAvailable() {
        // Older kernels lack MemAvailable: available is estimated as MemFree + Active(file) + Inactive(file) +
        // SReclaimable
        List<String> meminfo = Arrays.asList("MemTotal:       16000000 kB", "MemFree:         1000000 kB",
                "Active(file):    2000000 kB", "Inactive(file):  1000000 kB", "SReclaimable:     500000 kB");
        Pair<Long, Long> p = LinuxGlobalMemory.parseMemInfo(meminfo);
        long expected = ParseUtil.parseDecimalMemorySizeToBinary("1000000 kB")
                + ParseUtil.parseDecimalMemorySizeToBinary("2000000 kB")
                + ParseUtil.parseDecimalMemorySizeToBinary("1000000 kB")
                + ParseUtil.parseDecimalMemorySizeToBinary("500000 kB");
        assertThat(p.getA(), is(expected));
        assertThat(p.getB(), is(ParseUtil.parseDecimalMemorySizeToBinary("16000000 kB")));
    }

    @Test
    void testParseMemInfoEmpty() {
        Pair<Long, Long> p = LinuxGlobalMemory.parseMemInfo(Collections.emptyList());
        assertThat(p.getA(), is(0L));
        assertThat(p.getB(), is(0L));
    }
}
