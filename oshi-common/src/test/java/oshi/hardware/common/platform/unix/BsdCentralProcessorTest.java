/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import oshi.hardware.CentralProcessor.ProcessorCache;
import oshi.hardware.CentralProcessor.ProcessorCache.Type;
import oshi.hardware.common.platform.unix.BsdCentralProcessor.DmesgStrings;
import oshi.util.tuples.Pair;

class BsdCentralProcessorTest {

    // Representative OpenBSD/NetBSD dmesg topology lines
    private static final List<String> DMESG_TOPOLOGY = Arrays.asList(//
            "cpu0: smt 0, core 0, package 0", //
            "cpu1: smt 0, core 1, package 0", //
            "cpu2: smt 0, core 0, package 1", //
            "cpu3: smt 0, core 1, package 1");

    @Test
    void testParseTopology() {
        Pair<Map<Integer, Integer>, Map<Integer, Integer>> result = BsdCentralProcessor.parseTopology(DMESG_TOPOLOGY);
        Map<Integer, Integer> coreMap = result.getA();
        Map<Integer, Integer> packageMap = result.getB();
        assertThat(coreMap.size(), is(4));
        assertThat(coreMap.get(0), is(0));
        assertThat(coreMap.get(1), is(1));
        assertThat(coreMap.get(2), is(0));
        assertThat(coreMap.get(3), is(1));
        assertThat(packageMap.get(0), is(0));
        assertThat(packageMap.get(2), is(1));
    }

    @Test
    void testParseTopologyEmpty() {
        Pair<Map<Integer, Integer>, Map<Integer, Integer>> result = BsdCentralProcessor
                .parseTopology(Collections.emptyList());
        assertThat(result.getA().isEmpty(), is(true));
        assertThat(result.getB().isEmpty(), is(true));
    }

    @Test
    void testParseDmesgModelsAndCachesIntel() {
        // Representative x86 dmesg — cache entries are split across separate lines but each line ends with "cache"
        List<String> dmesg = Arrays.asList(//
                "cpu0: Intel(R) Celeron(R) N4000 CPU @ 1.10GHz, 2491.67 MHz, 06-7a-01", //
                "cpu0: 32KB 64b/line 8-way D-cache, 32KB 64b/line 8-way I-cache", //
                "cpu0: 4MB 64b/line 16-way L2 cache", //
                "cpu0: FPU,VME,DE,PSE,TSC,MSR,PAE,MCE");
        DmesgStrings result = BsdCentralProcessor.parseDmesgModelsAndCaches(dmesg);
        assertThat(result.getCpuMap().get(0), is("Intel(R) Celeron(R) N4000 CPU @ 1.10GHz, 2491.67 MHz, 06-7a-01"));
        assertThat(result.getCaches(), is(not(empty())));
        // D-cache, I-cache (from first cache line), L2 cache (from second)
        assertThat(result.getCaches().size(), is(3));
    }

    @Test
    void testParseDmesgModelsAndCachesArm() {
        // Representative ARM big.LITTLE dmesg
        List<String> dmesg = Arrays.asList(//
                "cpu0 at mainbus0 mpidr 0: ARM Cortex-A53 r0p4", //
                "cpu0: 32KB 64b/line 2-way L1 VIPT I-cache, 32KB 64b/line 4-way L1 D-cache", //
                "cpu0: 512KB 64b/line 16-way L2 cache", //
                "cpu4 at mainbus0 mpidr 100: ARM Cortex-A72 r0p2", //
                "cpu4: 48KB 64b/line 3-way L1 PIPT I-cache, 32KB 64b/line 2-way L1 D-cache", //
                "cpu4: 1024KB 64b/line 16-way L2 cache");
        DmesgStrings result = BsdCentralProcessor.parseDmesgModelsAndCaches(dmesg);
        assertThat(result.getCpuMap().get(0), is("ARM Cortex-A53 r0p4"));
        assertThat(result.getCpuMap().get(4), is("ARM Cortex-A72 r0p2"));
        // Should have L1 I-cache, L1 D-cache, L2 from each CPU type (unique by cache params)
        assertThat(result.getCaches().size(), is(6));
    }

    @Test
    void testParseDmesgModelsAndCachesFeatureFlags() {
        // Feature flags are lines starting with "cpu" having ": " with 4+ comma-delimited items
        List<String> dmesg = Arrays.asList(//
                "cpu0: FPU,VME,DE,PSE,TSC,MSR,PAE,MCE", //
                "cpu0: SSE3,PCLMULQDQ,DTES64,MONITOR");
        DmesgStrings result = BsdCentralProcessor.parseDmesgModelsAndCaches(dmesg);
        assertThat(result.getFeatureFlags().size(), is(2));
    }

    @Test
    void testParseDmesgModelsAndCachesEmpty() {
        DmesgStrings result = BsdCentralProcessor.parseDmesgModelsAndCaches(Collections.emptyList());
        assertThat(result.getCpuMap().isEmpty(), is(true));
        assertThat(result.getCaches(), is(empty()));
        assertThat(result.getFeatureFlags(), is(empty()));
    }

    @Test
    void testParseCacheStrDCache() {
        ProcessorCache cache = BsdCentralProcessor.parseCacheStr("32KB 64b/line 8-way D-cache");
        assertThat(cache, is(not(nullValue())));
        assertThat(cache.getType(), is(Type.DATA));
        assertThat(cache.getLevel(), is((byte) 1));
        assertThat(cache.getAssociativity(), is((byte) 8));
        assertThat(cache.getLineSize(), is((short) 64));
        assertThat(cache.getCacheSize(), is(32768));
    }

    @Test
    void testParseCacheStrICache() {
        ProcessorCache cache = BsdCentralProcessor.parseCacheStr("48KB 64b/line 3-way L1 PIPT I-cache");
        assertThat(cache, is(not(nullValue())));
        assertThat(cache.getType(), is(Type.INSTRUCTION));
        assertThat(cache.getLevel(), is((byte) 1));
        assertThat(cache.getAssociativity(), is((byte) 3));
    }

    @Test
    void testParseCacheStrL2Unified() {
        ProcessorCache cache = BsdCentralProcessor.parseCacheStr("4MB 64b/line 16-way L2 cache");
        assertThat(cache, is(not(nullValue())));
        assertThat(cache.getType(), is(Type.UNIFIED));
        assertThat(cache.getLevel(), is((byte) 2));
        assertThat(cache.getAssociativity(), is((byte) 16));
        assertThat(cache.getLineSize(), is((short) 64));
        assertThat(cache.getCacheSize(), is(4 * 1024 * 1024));
    }

    @Test
    void testParseCacheStrL3Unified() {
        ProcessorCache cache = BsdCentralProcessor.parseCacheStr("24MB 64b/line 12-way L3 cache");
        assertThat(cache, is(not(nullValue())));
        assertThat(cache.getType(), is(Type.UNIFIED));
        assertThat(cache.getLevel(), is((byte) 3));
        assertThat(cache.getAssociativity(), is((byte) 12));
    }

    @Test
    void testParseCacheStrTooShort() {
        ProcessorCache cache = BsdCentralProcessor.parseCacheStr("disabled");
        assertThat(cache, is(nullValue()));
    }
}
