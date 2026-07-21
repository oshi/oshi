/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.aix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.hardware.CentralProcessor.ProcessorCache;
import oshi.util.Constants;
import oshi.util.tuples.Quartet;

class AixCentralProcessorTest {

    @Test
    void testParseProcessorId() {
        // prtconf excerpt
        Quartet<String, String, String, Boolean> id = AixCentralProcessor.parseProcessorId(Arrays.asList(//
                "System Model: IBM,9114-275", //
                "Processor Type: PowerPC_POWER7", //
                "Processor Version: PV_7_Compat", //
                "Number Of Processors: 8", //
                "CPU Type: 64-bit"));
        assertThat(id.getA(), is("IBM"));
        assertThat(id.getB(), is("PowerPC_POWER7"));
        assertThat(id.getC(), is("PV_7_Compat"));
        assertThat(id.getD(), is(true));
    }

    @Test
    void testParseProcessorIdEmpty() {
        Quartet<String, String, String, Boolean> id = AixCentralProcessor.parseProcessorId(Collections.emptyList());
        assertThat(id.getA(), is(Constants.UNKNOWN));
        assertThat(id.getB(), is(""));
        assertThat(id.getC(), is(""));
        assertThat(id.getD(), is(false));
    }

    @Test
    void testParseCurrentFreq() {
        List<String> pmcycles = Arrays.asList("Cpu 0 runs at 3000 MHz", "Cpu 1 runs at 3000 MHz");
        // exactly two CPUs
        assertThat(AixCentralProcessor.parseCurrentFreq(pmcycles, 2),
                is(new long[] { 3_000_000_000L, 3_000_000_000L }));
        // more logical processors than output lines: trailing entries stay -1
        assertThat(AixCentralProcessor.parseCurrentFreq(pmcycles, 4),
                is(new long[] { 3_000_000_000L, 3_000_000_000L, -1L, -1L }));
        // no output: all -1
        assertThat(AixCentralProcessor.parseCurrentFreq(Collections.emptyList(), 2), is(new long[] { -1L, -1L }));
    }

    @Test
    void testParseSbits() {
        assertThat(AixCentralProcessor.parseSbits(Collections.singletonList("#define SBITS 16")), is(16));
        assertThat(AixCentralProcessor.parseSbits(Collections.singletonList("#define  SBITS  10")), is(10));
        // default when not present
        assertThat(AixCentralProcessor.parseSbits(Collections.singletonList("#define OTHER 5")), is(16));
        assertThat(AixCentralProcessor.parseSbits(Collections.emptyList()), is(16));
    }

    @Test
    void testCachesForPowerVersion() {
        assertThat(AixCentralProcessor.cachesForPowerVersion(7, 4), hasSize(4));
        assertThat(AixCentralProcessor.cachesForPowerVersion(8, 4), hasSize(5));
        // POWER9 sizes its shared L3 by core count: (cores * 10) MiB
        List<ProcessorCache> p9 = AixCentralProcessor.cachesForPowerVersion(9, 4);
        assertThat(p9, hasSize(4));
        ProcessorCache l3 = p9.stream().filter(c -> c.getLevel() == 3).findFirst().orElseThrow(AssertionError::new);
        assertThat(l3.getCacheSize(), is((4 * 10) << 20));
        // unknown version yields no caches
        assertThat(AixCentralProcessor.cachesForPowerVersion(99, 4), is(empty()));
    }
}
