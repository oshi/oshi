/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.solaris;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class SolarisCentralProcessorTest {

    @Test
    void testParseDmesgCpuInfo() {
        List<String> dmesg = Arrays.asList(//
                "Jan  9 14:04:28 solaris unix: [ID 950921 kern.info] cpu0: Intel(r) Celeron(r) CPU J3455 @ 1.50GHz", //
                "Jan  9 14:04:28 solaris unix: [ID 950921 kern.info] cpu0: x86 (chipid 0x0 GenuineIntel 506C9 family 6 model 92 step 9 clock 1500 MHz)", //
                "Jan  9 14:04:28 solaris unix: [ID 950921 kern.info] cpu1: Intel(r) Celeron(r) CPU J3455 @ 1.50GHz");
        Map<Integer, String> result = SolarisCentralProcessor.parseDmesgCpuInfo(dmesg);
        // Only lines matching ".* cpu(\d+): ((ARM|AMD|Intel).+)" are captured
        assertThat(result.size(), is(2));
        assertThat(result, hasEntry(0, "Intel(r) Celeron(r) CPU J3455 @ 1.50GHz"));
        assertThat(result, hasEntry(1, "Intel(r) Celeron(r) CPU J3455 @ 1.50GHz"));
    }

    @Test
    void testParseDmesgCpuInfoEmpty() {
        Map<Integer, String> result = SolarisCentralProcessor.parseDmesgCpuInfo(Collections.emptyList());
        assertThat(result.isEmpty(), is(true));
    }

    @Test
    void testParseNumaNodes() {
        List<String> lgrpinfo = Arrays.asList(//
                "lgroup 0 (root):", //
                "CPUs: 0-3", //
                "lgroup 1 (leaf):", //
                "CPUs: 4 5 6 7");
        Map<Integer, Integer> result = SolarisCentralProcessor.parseNumaNodes(lgrpinfo);
        assertThat(result.size(), is(8));
        assertThat(result, hasEntry(0, 0));
        assertThat(result, hasEntry(1, 0));
        assertThat(result, hasEntry(2, 0));
        assertThat(result, hasEntry(3, 0));
        assertThat(result, hasEntry(4, 1));
        assertThat(result, hasEntry(5, 1));
        assertThat(result, hasEntry(6, 1));
        assertThat(result, hasEntry(7, 1));
    }

    @Test
    void testParseNumaNodesEmpty() {
        Map<Integer, Integer> result = SolarisCentralProcessor.parseNumaNodes(Collections.emptyList());
        assertThat(result.isEmpty(), is(true));
    }

    @Test
    void testParseIsainfoFlags() {
        // isainfo -v output: first a 64-bit header, then indented flags, then 32-bit header
        List<String> isainfo = Arrays.asList(//
                "64-bit amd64 applications", //
                "        avx512f avx512cd sha sse4.2 popcnt", //
                "32-bit i386 applications", //
                "        sse2 sse mmx");
        String[] flags = SolarisCentralProcessor.parseIsainfoFlags(isainfo);
        // Should only contain flags from the 64-bit section, lowercased, split on whitespace.
        // The first element is empty because the flags StringBuilder starts with a space.
        assertThat(flags, arrayContaining("", "avx512f", "avx512cd", "sha", "sse4.2", "popcnt"));
    }

    @Test
    void testParseIsainfoFlagsEmpty() {
        String[] flags = SolarisCentralProcessor.parseIsainfoFlags(Collections.emptyList());
        // Splitting an empty string produces a single-element array with ""
        assertThat(flags.length, is(1));
        assertThat(flags[0], is(""));
    }

    @Test
    void testSumKstatLong() {
        List<String> kstat = Arrays.asList(//
                "cpu_stat:0:cpu_stat0:pswitch\t1000", //
                "cpu_stat:1:cpu_stat1:pswitch\t2000", //
                "cpu_stat:0:cpu_stat0:inv_swtch\t500");
        assertThat(SolarisCentralProcessor.sumKstatLong(kstat), is(3500L));
    }

    @Test
    void testSumKstatLongEmpty() {
        assertThat(SolarisCentralProcessor.sumKstatLong(Collections.emptyList()), is(0L));
    }
}
