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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import oshi.hardware.CentralProcessor.ProcessorCache;
import oshi.hardware.common.platform.unix.freebsd.FreeBsdCentralProcessor.DmesgProcessorId;
import oshi.util.tuples.Pair;

class FreeBsdCentralProcessorTest {

    // Representative dmesg.boot header (indented Origin/Features lines, as FreeBSD writes them)
    private static final List<String> DMESG_ID = Arrays.asList(
            "CPU: Intel(R) Xeon(R) CPU E5-2683 v3 @ 2.00GHz (2000.05-MHz K8-class CPU)",
            "  Origin=\"GenuineIntel\"  Id=0x306f2  Family=0x6  Model=0x3f  Stepping=2",
            "  Features=0xbfebfbff<FPU,VME,DE,PSE,TSC,MSR,PAE,MCE,CX8,APIC>",
            "  Features2=0x7ffefbff<SSE3,PCLMULQDQ,DTES64>");

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
    void testParseProcModelsAndFlags() {
        List<String> dmesg = Arrays.asList("cpu0: <ACPI CPU> on acpi0", "cpu1: <ACPI CPU> on acpi0",
                "  Origin=\"GenuineIntel\"  Id=0x306f2", "  Features=0xbfebfbff<FPU,VME>",
                "  Features2=0x7ffefbff<SSE3>", "real memory  = 8589934592");
        Pair<Map<Integer, String>, List<String>> r = FreeBsdCentralProcessor.parseProcModelsAndFlags(dmesg);
        assertThat(r.getA().get(0), is("<ACPI CPU>"));
        assertThat(r.getA().get(1), is("<ACPI CPU>"));
        assertThat(r.getB(), contains("Features=0xbfebfbff<FPU,VME>", "Features2=0x7ffefbff<SSE3>"));
    }

    @Test
    void testParseProcModelsAndFlagsArmHybrid() {
        List<String> dmesg = Arrays.asList("CPU 0: ARM Cortex-A53 r0p4 affinity: 0 0",
                "CPU 1: ARM Cortex-A72 r0p3 affinity: 0 1");
        Pair<Map<Integer, String>, List<String>> r = FreeBsdCentralProcessor.parseProcModelsAndFlags(dmesg);
        assertThat(r.getA().get(0), is("ARM Cortex-A53 r0p4"));
        assertThat(r.getA().get(1), is("ARM Cortex-A72 r0p3"));
        assertThat(r.getB(), is(empty()));
    }

    @Test
    void testParseCachesFromLscpu() {
        List<String> lscpu = Arrays.asList("L1d cache:                       32K",
                "L1i cache:                       32K", "L2 cache:                        256K",
                "L3 cache:                        8M");
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
}
