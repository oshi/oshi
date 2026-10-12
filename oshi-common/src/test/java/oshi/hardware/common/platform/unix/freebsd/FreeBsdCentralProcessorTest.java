/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.freebsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import oshi.hardware.CentralProcessor.LogicalProcessor;
import oshi.hardware.CentralProcessor.ProcessorCache;
import oshi.hardware.CentralProcessor.TickType;
import oshi.hardware.common.platform.unix.freebsd.FreeBsdCentralProcessor.DmesgProcessorId;
import oshi.util.tuples.Pair;

class FreeBsdCentralProcessorTest {

    // Representative dmesg.boot header (indented Origin/Features lines, as FreeBSD writes them)
    private static final List<String> DMESG_ID = """
            CPU: Intel(R) Xeon(R) CPU E5-2683 v3 @ 2.00GHz (2000.05-MHz K8-class CPU)
              Origin="GenuineIntel"  Id=0x306f2  Family=0x6  Model=0x3f  Stepping=2
              Features=0xbfebfbff<FPU,VME,DE,PSE,TSC,MSR,PAE,MCE,CX8,APIC>
              Features2=0x7ffefbff<SSE3,PCLMULQDQ,DTES64>
            """.lines().toList();

    @Test
    void testParseProcessorIdFromDmesg() {
        DmesgProcessorId id = FreeBsdCentralProcessor.parseProcessorIdFromDmesg(DMESG_ID, "");
        assertThat(id.vendor(), is("GenuineIntel"));
        assertThat(id.name(), is("Intel(R) Xeon(R) CPU E5-2683 v3 @ 2.00GHz (2000.05-MHz K8-class CPU)"));
        assertThat(id.family(), is("6"));
        assertThat(id.model(), is("63"));
        assertThat(id.stepping(), is("2"));
        // low 32 bits from Id, high 32 bits from the first Features field
        assertThat(id.processorIdBits(), is(0xbfebfbff000306f2L));
    }

    @Test
    void testParseProcessorIdFromDmesgPrefersSysctlName() {
        // A non-empty initial name (from sysctl hw.model) is kept over the CPU: line
        DmesgProcessorId id = FreeBsdCentralProcessor.parseProcessorIdFromDmesg(DMESG_ID, "Preset Model");
        assertThat(id.name(), is("Preset Model"));
        assertThat(id.vendor(), is("GenuineIntel"));
    }

    @Test
    void testParseProcessorIdFromDmesgEmpty() {
        DmesgProcessorId id = FreeBsdCentralProcessor.parseProcessorIdFromDmesg(Collections.emptyList(), "");
        assertThat(id.vendor(), is(""));
        assertThat(id.name(), is(""));
        assertThat(id.processorIdBits(), is(0L));
    }

    @Test
    void testParseProcessorIdFromDmesgDragonFly() {
        // DragonFly BSD spaces out each = and omits Family and Model, which come from the Id
        List<String> dmesg = """
                CPU: Intel Xeon Processor (Cascadelake) (2693.67-MHz K8-class CPU)
                  Origin = "GenuineIntel"  Id = 0x50655  Stepping = 5
                  Features=0xf83fbff<FPU,VME,DE,PSE,TSC,MSR,PAE,MCE,CX8,APIC,SEP,MTRR,PGE,MCA,CMOV>
                  Features2=0xf7fa3223<SSE3,PCLMULQDQ,VMX,SSSE3,FMA,CX16>
                """.lines().toList();
        DmesgProcessorId id = FreeBsdCentralProcessor.parseProcessorIdFromDmesg(dmesg, "");
        assertThat(id.vendor(), is("GenuineIntel"));
        assertThat(id.name(), is("Intel Xeon Processor (Cascadelake) (2693.67-MHz K8-class CPU)"));
        assertThat(id.family(), is("6"));
        assertThat(id.model(), is("85"));
        assertThat(id.stepping(), is("5"));
        assertThat(id.processorIdBits(), is(0x0f83fbff00050655L));

        Pair<Map<Integer, String>, List<String>> r = FreeBsdCentralProcessor.parseProcModelsAndFlags(dmesg);
        assertThat(r.getB(),
                contains("Features=0xf83fbff<FPU,VME,DE,PSE,TSC,MSR,PAE,MCE,CX8,APIC,SEP,MTRR,PGE,MCA,CMOV>",
                        "Features2=0xf7fa3223<SSE3,PCLMULQDQ,VMX,SSSE3,FMA,CX16>"));
    }

    @Test
    void testBuildTopologyFromIdsOneCorePerChip() {
        // DragonFly BSD on an 8-vCPU OpenStack guest: eight one-core packages, and no core_id
        int[] none = { -1, -1, -1, -1, -1, -1, -1, -1 };
        List<LogicalProcessor> lps = FreeBsdCentralProcessor.buildTopologyFromIds(new int[] { 0, 1, 2, 3, 4, 5, 6, 7 },
                none);
        assertThat(lps.size(), is(8));
        assertThat(lps.stream().map(LogicalProcessor::getPhysicalPackageNumber).distinct().count(), is(8L));
        assertThat(lps.stream().map(LogicalProcessor::getPhysicalProcessorNumber).distinct().count(), is(8L));
    }

    @Test
    void testBuildTopologyFromIdsSmt() {
        // Two packages of two cores with two threads each; core IDs repeat in each package
        List<LogicalProcessor> lps = FreeBsdCentralProcessor.buildTopologyFromIds(new int[] { 0, 0, 0, 0, 1, 1, 1, 1 },
                new int[] { 0, 0, 1, 1, 0, 0, 1, 1 });
        assertThat(lps.size(), is(8));
        assertThat(lps.stream().map(LogicalProcessor::getPhysicalPackageNumber).distinct().count(), is(2L));
        assertThat(lps.stream().map(LogicalProcessor::getPhysicalProcessorNumber).distinct().count(), is(4L));
        assertThat(lps.get(1).getPhysicalProcessorNumber(), is(lps.get(0).getPhysicalProcessorNumber()));
        assertThat(lps.get(4).getPhysicalProcessorNumber(), is(2));
        assertThat(lps.get(7).getProcessorNumber(), is(7));
        assertThat(lps.get(7).getPhysicalPackageNumber(), is(1));
    }

    @Test
    void testBuildTopologyFromIdsEmpty() {
        assertThat(FreeBsdCentralProcessor.buildTopologyFromIds(new int[0], new int[0]).isEmpty(), is(true));
    }

    @Test
    void testParseProcModelsAndFlags() {
        List<String> dmesg = """
                cpu0: <ACPI CPU> on acpi0
                cpu1: <ACPI CPU> on acpi0
                  Origin="GenuineIntel"  Id=0x306f2
                  Features=0xbfebfbff<FPU,VME>
                  Features2=0x7ffefbff<SSE3>
                real memory  = 8589934592
                """.lines().toList();
        Pair<Map<Integer, String>, List<String>> r = FreeBsdCentralProcessor.parseProcModelsAndFlags(dmesg);
        assertThat(r.getA().get(0), is("<ACPI CPU>"));
        assertThat(r.getA().get(1), is("<ACPI CPU>"));
        assertThat(r.getB(), contains("Features=0xbfebfbff<FPU,VME>", "Features2=0x7ffefbff<SSE3>"));
    }

    @Test
    void testParseProcModelsAndFlagsArmHybrid() {
        List<String> dmesg = List.of("CPU 0: ARM Cortex-A53 r0p4 affinity: 0 0",
                "CPU 1: ARM Cortex-A72 r0p3 affinity: 0 1");
        Pair<Map<Integer, String>, List<String>> r = FreeBsdCentralProcessor.parseProcModelsAndFlags(dmesg);
        assertThat(r.getA().get(0), is("ARM Cortex-A53 r0p4"));
        assertThat(r.getA().get(1), is("ARM Cortex-A72 r0p3"));
        assertThat(r.getB(), is(empty()));
    }

    @Test
    void testParseCachesFromLscpu() {
        List<String> lscpu = """
                L1d cache:                       32K
                L1i cache:                       32K
                L2 cache:                        256K
                L3 cache:                        8M
                """.lines().toList();
        List<ProcessorCache> caches = FreeBsdCentralProcessor.parseCachesFromLscpu(lscpu);
        assertThat(caches, hasSize(4));
        assertThat(caches.stream().anyMatch(c -> c.getLevel() == 3), is(true));
        assertThat(caches.stream().anyMatch(c -> c.getLevel() == 1 && c.getType() == ProcessorCache.Type.DATA),
                is(true));
    }

    @Test
    void testParseCachesFromLscpuEmpty() {
        assertThat(FreeBsdCentralProcessor.parseCachesFromLscpu(Collections.emptyList()), is(empty()));
    }

    @Test
    void testParseProcessorTicksFreeBsdLayout() {
        // kern.cp_times: CPUSTATES values per CPU, sized for more CPUs than are present
        long[] cpTimes = { 1, 2, 3, 4, 5, 11, 12, 13, 14, 15, 0, 0, 0, 0, 0 };
        long[][] ticks = FreeBsdCentralProcessor.parseProcessorTicks(cpTimes, 2, 5);
        assertThat(ticks.length, is(2));
        assertThat(ticks[0][TickType.USER.getIndex()], is(1L));
        assertThat(ticks[0][TickType.NICE.getIndex()], is(2L));
        assertThat(ticks[0][TickType.SYSTEM.getIndex()], is(3L));
        assertThat(ticks[0][TickType.IRQ.getIndex()], is(4L));
        assertThat(ticks[0][TickType.IDLE.getIndex()], is(5L));
        assertThat(ticks[1][TickType.USER.getIndex()], is(11L));
        assertThat(ticks[1][TickType.IDLE.getIndex()], is(15L));
    }

    @Test
    void testParseProcessorTicksKinfoCputimeLayout() {
        // kern.cputime: one 13-value struct kinfo_cputime per CPU; only the first five are CPU states
        long[] cpTimes = new long[26];
        for (int i = 0; i < cpTimes.length; i++) {
            cpTimes[i] = 100L + i;
        }
        long[][] ticks = FreeBsdCentralProcessor.parseProcessorTicks(cpTimes, 2, cpTimes.length / 2);
        assertThat(ticks[0][TickType.USER.getIndex()], is(100L));
        assertThat(ticks[0][TickType.IDLE.getIndex()], is(104L));
        assertThat(ticks[1][TickType.USER.getIndex()], is(113L));
        assertThat(ticks[1][TickType.IDLE.getIndex()], is(117L));
    }

    @Test
    void testParseProcessorTicksShortOrMissing() {
        long[] zeros = new long[TickType.values().length];
        // Fewer records than CPUs: the missing CPUs stay zero
        long[][] ticks = FreeBsdCentralProcessor.parseProcessorTicks(new long[] { 1, 2, 3, 4, 5 }, 2, 5);
        assertThat(ticks[0][TickType.IDLE.getIndex()], is(5L));
        assertThat(ticks[1], is(zeros));
        // A record too short to hold the CPU states is ignored rather than read past
        assertThat(FreeBsdCentralProcessor.parseProcessorTicks(new long[] { 1, 2, 3, 4 }, 1, 4)[0], is(zeros));
        assertThat(FreeBsdCentralProcessor.parseProcessorTicks(null, 2, 5), is(new long[2][zeros.length]));
    }
}
