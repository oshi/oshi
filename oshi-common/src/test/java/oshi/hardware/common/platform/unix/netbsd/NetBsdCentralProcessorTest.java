/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.netbsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.hardware.CentralProcessor.ProcessorIdentifier;
import oshi.hardware.CentralProcessor.TickType;
import oshi.util.tuples.Pair;

class NetBsdCentralProcessorTest {

    @Test
    void testParseVmStats() {
        List<String> vmstat = """
                       42983 CPU context switches
                        8301 device interrupts
                           0 software interrupts
                       12345 some other stat
                """.lines().toList();
        Pair<Long, Long> result = NetBsdCentralProcessor.parseVmStats(vmstat);
        assertThat(result.getA(), is(42983L));
        assertThat(result.getB(), is(8301L));
    }

    @Test
    void testParseVmStatsEmpty() {
        Pair<Long, Long> result = NetBsdCentralProcessor.parseVmStats(Collections.emptyList());
        assertThat(result.getA(), is(0L));
        assertThat(result.getB(), is(0L));
    }

    @Test
    void testParseFamilyModelStepping() {
        // NetBSD 10.1 amd64 on the GitHub Actions VM; cpuctl identify reads it as family 0x19 model 0x1 stepping 0x1
        List<String> dmesg = """
                cpu0 at mainbus0 apid 0
                cpu0: AMD EPYC 7763 64-Core Processor                , id 0xa00f11
                cpu0: node 0, package 0, core 0, smt 0
                """.lines().toList();
        assertThat(NetBsdCentralProcessor.parseFamilyModelStepping(dmesg), is(arrayContaining("25", "1", "1")));
    }

    @Test
    void testParseFamilyModelSteppingEmpty() {
        String[] fms = NetBsdCentralProcessor.parseFamilyModelStepping(Collections.emptyList());
        assertThat(fms, is(arrayContaining("", "", "")));
    }

    @Test
    void testParseFamilyModelSteppingArm() {
        // The ARM model line carries the affinity register, not a signature
        List<String> dmesg = List.of("cpu0 at acpi0: Arm Neoverse N1 r3p1 (v8.2-A+), id 0x0");
        String[] fms = NetBsdCentralProcessor.parseFamilyModelStepping(dmesg);
        assertThat(fms, is(arrayContaining("", "", "")));
    }

    @Test
    void testParseMidr() {
        // sysctl -nx machdep.cpu0.cpu_id on NetBSD 10.1 aarch64 (cfarm428): MIDR, then REVIDR 0x1c7
        String cpuId = "c1d03f4100000000c701000000000000...(120 more bytes)";
        assertThat(NetBsdCentralProcessor.parseMidr(cpuId, false), is(0x413fd0c1L));
        assertThat(NetBsdCentralProcessor.parseMidr("00000000413fd0c1", true), is(0x413fd0c1L));
        assertThat(NetBsdCentralProcessor.parseMidr("", false), is(-1L));
        assertThat(NetBsdCentralProcessor.parseMidr("sysctl: second level name 'cpu0' is invalid", false), is(-1L));
    }

    @Test
    void testCreateArmIdentifier() {
        ProcessorIdentifier id = NetBsdCentralProcessor.createArmIdentifier(0x413fd0c1L,
                "Arm Neoverse N1 r3p1 (v8.2-A+)", 2_000_000_000L);
        assertThat(id.getVendor(), is("ARM"));
        assertThat(id.getName(), is("Arm Neoverse N1 r3p1 (v8.2-A+)"));
        assertThat(id.getFamily(), is("8"));
        assertThat(id.getModel(), is("0xd0c"));
        assertThat(id.getStepping(), is("r3p1"));
        assertThat(id.getProcessorID(), is("413FD0C100000000"));
        assertThat(id.getMicroarchitecture(), is("Neoverse-N1"));
        assertThat(id.isCpu64bit(), is(true));
        assertThat(id.getVendorFreq(), is(2_000_000_000L));
    }

    @Test
    void testParseFreq() {
        assertThat(NetBsdCentralProcessor.parseFreq("Intel(R) Celeron(R) N4000 CPU @ 1.10GHz", "2000 MHz"),
                is(1_100_000_000L));
        assertThat(NetBsdCentralProcessor.parseFreq("netbsd,generic-acpi", "2000 MHz"), is(2_000_000_000L));
        assertThat(NetBsdCentralProcessor.parseFreq("netbsd,generic-acpi", ""), is(-1L));
    }

    @Test
    void testParseCpTime() {
        long[] ticks = NetBsdCentralProcessor
                .parseCpTime("kern.cp_time: user = 2930, nice = 42, sys = 1334, intr = 1877, idle = 46354");
        assertThat(ticks[0], is(2930L));
        assertThat(ticks[1], is(42L));
        assertThat(ticks[2], is(1334L));
        assertThat(ticks[3], is(1877L));
        assertThat(ticks[4], is(46354L));
    }

    @Test
    void testParseCpTimeEmpty() {
        long[] ticks = NetBsdCentralProcessor.parseCpTime("");
        assertThat(ticks.length, is(5));
        for (long tick : ticks) {
            assertThat(tick, is(0L));
        }
    }

    @Test
    void testParseCpTimeNull() {
        long[] ticks = NetBsdCentralProcessor.parseCpTime(null);
        assertThat(ticks.length, is(5));
        for (long tick : ticks) {
            assertThat(tick, is(0L));
        }
    }

    @Test
    void testParseCpTimePerCpu() {
        long[] ticks = NetBsdCentralProcessor
                .parseCpTime("kern.cp_time.0: user = 100, nice = 0, sys = 50, intr = 10, idle = 840");
        assertThat(ticks[0], is(100L));
        assertThat(ticks[1], is(0L));
        assertThat(ticks[2], is(50L));
        assertThat(ticks[3], is(10L));
        assertThat(ticks[4], is(840L));
    }

    @Test
    void testCpTimeToTicks() {
        long[] ticks = NetBsdCentralProcessor.cpTimeToTicks(new long[] { 2930, 42, 1334, 1877, 46354 });
        assertThat(ticks[TickType.USER.getIndex()], is(2930L));
        assertThat(ticks[TickType.NICE.getIndex()], is(42L));
        assertThat(ticks[TickType.SYSTEM.getIndex()], is(1334L));
        assertThat(ticks[TickType.IRQ.getIndex()], is(1877L));
        assertThat(ticks[TickType.IDLE.getIndex()], is(46354L));
        assertThat(ticks[TickType.IOWAIT.getIndex()], is(0L));
        assertThat(ticks[TickType.SOFTIRQ.getIndex()], is(0L));
        assertThat(ticks[TickType.STEAL.getIndex()], is(0L));
    }

    @Test
    void testCpTimeToTicksShort() {
        long[] ticks = NetBsdCentralProcessor.cpTimeToTicks(new long[] { 1, 2, 3 });
        assertThat(ticks.length, is(TickType.values().length));
        for (long tick : ticks) {
            assertThat(tick, is(0L));
        }
    }
}
