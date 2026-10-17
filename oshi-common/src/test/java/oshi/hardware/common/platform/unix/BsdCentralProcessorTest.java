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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import oshi.hardware.CentralProcessor.ProcessorCache;
import oshi.hardware.common.platform.unix.BsdCentralProcessor.DmesgStrings;
import oshi.util.tuples.Pair;

class BsdCentralProcessorTest {

    // Representative OpenBSD/NetBSD dmesg topology lines
    private static final List<String> DMESG_TOPOLOGY = """
            cpu0: smt 0, core 0, package 0
            cpu1: smt 0, core 1, package 0
            cpu2: smt 0, core 0, package 1
            cpu3: smt 0, core 1, package 1
            """.lines().toList();

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
    void testParseTopologyNetBsd() {
        // NetBSD's order, captured from NetBSD 10.1: aarch64 (cfarm428), then amd64 with a NUMA node in front
        List<String> dmesg = """
                cpu0: package 0, core 0, smt 0
                cpu15: package 0, core 15, smt 0
                cpu2: node 0, package 1, core 2, smt 0
                """.lines().toList();
        Pair<Map<Integer, Integer>, Map<Integer, Integer>> result = BsdCentralProcessor.parseTopology(dmesg);
        assertThat(result.getA(), is(Map.of(0, 0, 15, 15, 2, 2)));
        assertThat(result.getB(), is(Map.of(0, 0, 15, 0, 2, 1)));
    }

    @Test
    void testStripDmesgTimestamps() {
        List<String> dmesg = List.of("[     1.000004] cpu0: node 0, package 0, core 0, smt 0", "[ 123.456789] cpu1",
                "cpu2: smt 0, core 0, package 0");
        assertThat(BsdCentralProcessor.stripDmesgTimestamps(dmesg),
                is(List.of("cpu0: node 0, package 0, core 0, smt 0", "cpu1", "cpu2: smt 0, core 0, package 0")));
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
        List<String> dmesg = """
                cpu0: Intel(R) Celeron(R) N4000 CPU @ 1.10GHz, 2491.67 MHz, 06-7a-01
                cpu0: 32KB 64b/line 8-way D-cache, 32KB 64b/line 8-way I-cache
                cpu0: 4MB 64b/line 16-way L2 cache
                cpu0: FPU,VME,DE,PSE,TSC,MSR,PAE,MCE
                """.lines().toList();
        DmesgStrings result = BsdCentralProcessor.parseDmesgModelsAndCaches(dmesg);
        assertThat(result.getCpuMap().get(0), is("Intel(R) Celeron(R) N4000 CPU @ 1.10GHz, 2491.67 MHz, 06-7a-01"));
        assertThat(result.getCaches(), is(not(empty())));
        // D-cache, I-cache (from first cache line), L2 cache (from second)
        assertThat(result.getCaches().size(), is(3));
    }

    @Test
    void testParseDmesgModelsAndCachesArm() {
        // Representative ARM big.LITTLE dmesg
        List<String> dmesg = """
                cpu0 at mainbus0 mpidr 0: ARM Cortex-A53 r0p4
                cpu0: 32KB 64b/line 2-way L1 VIPT I-cache, 32KB 64b/line 4-way L1 D-cache
                cpu0: 512KB 64b/line 16-way L2 cache
                cpu4 at mainbus0 mpidr 100: ARM Cortex-A72 r0p2
                cpu4: 48KB 64b/line 3-way L1 PIPT I-cache, 32KB 64b/line 2-way L1 D-cache
                cpu4: 1024KB 64b/line 16-way L2 cache
                """.lines().toList();
        DmesgStrings result = BsdCentralProcessor.parseDmesgModelsAndCaches(dmesg);
        assertThat(result.getCpuMap().get(0), is("ARM Cortex-A53 r0p4"));
        assertThat(result.getCpuMap().get(4), is("ARM Cortex-A72 r0p2"));
        // Should have L1 I-cache, L1 D-cache, L2 from each CPU type (unique by cache params)
        assertThat(result.getCaches().size(), is(6));
    }

    @Test
    void testParseDmesgModelsAndCachesNetBsdArm() {
        // NetBSD 10.1 on a KVM Neoverse N1 (cfarm428). QEMU reports one-set caches, so the sizes round down to 0KB
        List<String> dmesg = """
                cpu0 at acpi0: Arm Neoverse N1 r3p1 (v8.2-A+), id 0x0
                cpu0: L1 0KB/64B 1-way (1 set) PIPT Instruction cache
                cpu0: L2 0KB/64B 1-way (1 set) PIPT Unified cache
                cpu0: package 0, core 0, smt 0
                cpu1 at acpi0: Arm Neoverse N1 r3p1 (v8.2-A+), id 0x1
                cpu1: L1 64KB/64B 4-way (256 set) VIPT Data cache
                cpu1: L2 1024KB/64B 8-way (2048 set) PIPT Unified cache
                """.lines().toList();
        DmesgStrings result = BsdCentralProcessor.parseDmesgModelsAndCaches(dmesg);
        // The affinity register differs on every processor, so it must not reach the name, or each core is a type
        assertThat(result.getCpuMap(),
                is(Map.of(0, "Arm Neoverse N1 r3p1 (v8.2-A+)", 1, "Arm Neoverse N1 r3p1 (v8.2-A+)")));
        assertThat(result.getCaches().size(), is(4));
        assertThat(result.getCaches().contains(new ProcessorCache(1, 4, 64, 65_536L, ProcessorCache.Type.DATA)),
                is(true));
        assertThat(result.getCaches().contains(new ProcessorCache(2, 8, 64, 1_048_576L, ProcessorCache.Type.UNIFIED)),
                is(true));
        assertThat(result.getCaches().contains(new ProcessorCache(1, 1, 64, 0L, ProcessorCache.Type.INSTRUCTION)),
                is(true));
        assertThat(result.getFeatureFlags(), is(empty()));
    }

    @Test
    void testParseDmesgModelsAndCachesNetBsdX86() {
        // NetBSD 10.1 amd64 on the GitHub Actions VM
        List<String> dmesg = """
                cpu0 at mainbus0 apid 0
                cpu0: AMD EPYC 7763 64-Core Processor                , id 0xa00f11
                cpu0: node 0, package 0, core 0, smt 0
                """.lines().toList();
        DmesgStrings result = BsdCentralProcessor.parseDmesgModelsAndCaches(dmesg);
        assertThat(result.getCpuMap(), is(Map.of(0, "AMD EPYC 7763 64-Core Processor")));
        assertThat(BsdCentralProcessor.parseDmesgCpuName(dmesg), is("AMD EPYC 7763 64-Core Processor"));
        // Four comma-separated topology fields are not a feature list
        assertThat(result.getFeatureFlags(), is(empty()));
    }

    @Test
    void testParseDmesgModelsAndCachesFeatureFlags() {
        // Feature flags are lines starting with "cpu" having ": " with 4+ comma-delimited items
        List<String> dmesg = List.of(//
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
        assertThat(cache.getType(), is(ProcessorCache.Type.DATA));
        assertThat(cache.getLevel(), is((byte) 1));
        assertThat(cache.getAssociativity(), is((byte) 8));
        assertThat(cache.getLineSize(), is((short) 64));
        assertThat(cache.getCacheSize(), is(32768));
    }

    @Test
    void testParseCacheStrICache() {
        ProcessorCache cache = BsdCentralProcessor.parseCacheStr("48KB 64b/line 3-way L1 PIPT I-cache");
        assertThat(cache, is(not(nullValue())));
        assertThat(cache.getType(), is(ProcessorCache.Type.INSTRUCTION));
        assertThat(cache.getLevel(), is((byte) 1));
        assertThat(cache.getAssociativity(), is((byte) 3));
    }

    @Test
    void testParseCacheStrL2Unified() {
        ProcessorCache cache = BsdCentralProcessor.parseCacheStr("4MB 64b/line 16-way L2 cache");
        assertThat(cache, is(not(nullValue())));
        assertThat(cache.getType(), is(ProcessorCache.Type.UNIFIED));
        assertThat(cache.getLevel(), is((byte) 2));
        assertThat(cache.getAssociativity(), is((byte) 16));
        assertThat(cache.getLineSize(), is((short) 64));
        assertThat(cache.getCacheSize(), is(4 * 1024 * 1024));
    }

    @Test
    void testParseCacheStrL3Unified() {
        ProcessorCache cache = BsdCentralProcessor.parseCacheStr("24MB 64b/line 12-way L3 cache");
        assertThat(cache, is(not(nullValue())));
        assertThat(cache.getType(), is(ProcessorCache.Type.UNIFIED));
        assertThat(cache.getLevel(), is((byte) 3));
        assertThat(cache.getAssociativity(), is((byte) 12));
    }

    @Test
    void testParseCacheStrTooShort() {
        ProcessorCache cache = BsdCentralProcessor.parseCacheStr("disabled");
        assertThat(cache, is(nullValue()));
    }
}
