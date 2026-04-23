/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import oshi.hardware.CentralProcessor.LogicalProcessor;
import oshi.hardware.CentralProcessor.PhysicalProcessor;
import oshi.hardware.CentralProcessor.ProcessorCache;
import oshi.hardware.CentralProcessor.ProcessorCache.Type;
import oshi.hardware.CentralProcessor.ProcessorIdentifier;
import oshi.hardware.CentralProcessor.TickType;
import oshi.util.tuples.Quartet;

class AbstractCentralProcessorTest {

    private static AbstractCentralProcessor createProcessor() {
        return new AbstractCentralProcessor() {
            @Override
            protected Quartet<List<LogicalProcessor>, List<PhysicalProcessor>, List<ProcessorCache>, List<String>> initProcessorCounts() {
                return new Quartet<>(Collections.singletonList(new LogicalProcessor(0, 0, 0)), null, null,
                        Collections.emptyList());
            }

            @Override
            protected ProcessorIdentifier queryProcessorId() {
                return new ProcessorIdentifier("Vendor", "TestCPU", "6", "42", "1", "0000000000000000", true);
            }

            @Override
            protected long[] queryCurrentFreq() {
                return new long[] { 3_600_000_000L };
            }

            @Override
            protected long queryContextSwitches() {
                return 100L;
            }

            @Override
            protected long queryInterrupts() {
                return 200L;
            }

            @Override
            protected long[] querySystemCpuLoadTicks() {
                return new long[TickType.values().length];
            }

            @Override
            protected long[][] queryProcessorCpuLoadTicks() {
                return new long[][] { new long[TickType.values().length] };
            }

            @Override
            public double[] getSystemLoadAverage(int nelem) {
                return new double[] { 1.0 };
            }
        };
    }

    @Test
    void testSystemCpuLoadBetweenTicksFullLoad() {
        long[] oldTicks = new long[TickType.values().length];
        long[] newTicks = new long[TickType.values().length];
        newTicks[TickType.USER.getIndex()] = 500;
        newTicks[TickType.SYSTEM.getIndex()] = 500;

        double load = createProcessor().getSystemCpuLoadBetweenTicks(oldTicks, newTicks);
        assertThat(load, is(closeTo(1.0, 0.001)));
    }

    @Test
    void testSystemCpuLoadBetweenTicksHalfIdle() {
        long[] oldTicks = new long[TickType.values().length];
        long[] newTicks = new long[TickType.values().length];
        newTicks[TickType.USER.getIndex()] = 250;
        newTicks[TickType.SYSTEM.getIndex()] = 250;
        newTicks[TickType.IDLE.getIndex()] = 500;

        double load = createProcessor().getSystemCpuLoadBetweenTicks(oldTicks, newTicks);
        assertThat(load, is(closeTo(0.5, 0.001)));
    }

    @Test
    void testSystemCpuLoadBetweenTicksWrongLength() {
        assertThrows(IllegalArgumentException.class, () -> createProcessor().getSystemCpuLoadBetweenTicks(new long[2]));
    }

    @Test
    void testProcessorCpuLoadBetweenTicks() {
        long[] oldTicks = new long[TickType.values().length];
        long[] newTicks = new long[TickType.values().length];
        newTicks[TickType.USER.getIndex()] = 750;
        newTicks[TickType.IDLE.getIndex()] = 250;

        double[] loads = createProcessor().getProcessorCpuLoadBetweenTicks(new long[][] { oldTicks },
                new long[][] { newTicks });
        assertThat(loads.length, is(1));
        assertThat(loads[0], is(closeTo(0.75, 0.001)));
    }

    @Test
    void testCreateProcessorIDNoHwcap() {
        String id = AbstractCentralProcessor.createProcessorID("1", "2", "3", new String[] { "fpu", "sse" });
        assertThat(id, is(notNullValue()));
        assertThat(id.length(), is(16));
        assertDoesNotThrow(() -> Long.parseUnsignedLong(id, 16));
    }

    @Test
    void testCreateProcessorIDWithHwcap() {
        String id = AbstractCentralProcessor.createProcessorID("1", "2", "3", new String[0], 0xFFL);
        assertThat(id.length(), is(16));
        long parsed = Long.parseUnsignedLong(id, 16);
        assertThat(parsed >>> 32, is(0xFFL));
    }

    @Test
    void testCreateProcessorIDWithHighBitHwcap() {
        long hwcap = 0x80000000L;
        String id = AbstractCentralProcessor.createProcessorID("1", "2", "3", new String[0], hwcap);
        assertThat(id.length(), is(16));
        long parsed = Long.parseUnsignedLong(id, 16);
        assertThat(parsed >>> 32, is(hwcap));
    }

    @Test
    void testOrderedProcCaches() {
        // Model real Apple Silicon-style caches where P-core and E-core variants
        // of the same level+type have different sizes but the same highestOneBit,
        // producing sort key ties (e.g., 131072 and 196608 both -> highestOneBit 131072)
        Set<ProcessorCache> caches = new HashSet<>();
        ProcessorCache l2p = new ProcessorCache(2, 0, 128, 16777216, Type.UNIFIED);
        ProcessorCache l2e = new ProcessorCache(2, 0, 128, 25165824, Type.UNIFIED);
        ProcessorCache l1ip = new ProcessorCache(1, 0, 128, 131072, Type.INSTRUCTION);
        ProcessorCache l1ie = new ProcessorCache(1, 0, 128, 196608, Type.INSTRUCTION);
        ProcessorCache l1dp = new ProcessorCache(1, 0, 128, 131072, Type.DATA);
        ProcessorCache l1de = new ProcessorCache(1, 0, 128, 196608, Type.DATA);
        caches.add(l2p);
        caches.add(l2e);
        caches.add(l1ip);
        caches.add(l1ie);
        caches.add(l1dp);
        caches.add(l1de);

        List<ProcessorCache> ordered = AbstractCentralProcessor.orderedProcCaches(caches);
        assertThat(ordered, hasSize(6));
        // L2 sorts before L1; within same level+type+highestOneBit, order is not guaranteed
        assertThat(ordered.subList(0, 2), containsInAnyOrder(l2p, l2e));
        assertThat(ordered.subList(2, 4), containsInAnyOrder(l1ip, l1ie));
        assertThat(ordered.subList(4, 6), containsInAnyOrder(l1dp, l1de));
    }

    @Test
    void testProcessorCounts() {
        AbstractCentralProcessor cpu = createProcessor();
        assertThat(cpu.getLogicalProcessorCount(), is(1));
        assertThat(cpu.getPhysicalProcessorCount(), is(1));
        assertThat(cpu.getPhysicalPackageCount(), is(1));
    }

    @Test
    void testToString() {
        assertThat(createProcessor().toString(), containsString("physical CPU package"));
        assertThat(createProcessor().toString(), containsString("logical CPU"));
    }

    private static AbstractCentralProcessor createHybridProcessor() {
        return new AbstractCentralProcessor() {
            @Override
            protected Quartet<List<LogicalProcessor>, List<PhysicalProcessor>, List<ProcessorCache>, List<String>> initProcessorCounts() {
                List<LogicalProcessor> logProcs = Arrays.asList(new LogicalProcessor(0, 0, 0),
                        new LogicalProcessor(1, 1, 0), new LogicalProcessor(2, 2, 0), new LogicalProcessor(3, 3, 0));
                List<PhysicalProcessor> physProcs = Arrays.asList(new PhysicalProcessor(0, 0, 1, "P-core"),
                        new PhysicalProcessor(0, 1, 1, "P-core"), new PhysicalProcessor(0, 2, 0, "E-core"),
                        new PhysicalProcessor(0, 3, 0, "E-core"));
                List<ProcessorCache> caches = Collections
                        .singletonList(new ProcessorCache(2, 0, 64, 4 * 1024 * 1024, Type.UNIFIED));
                return new Quartet<>(logProcs, physProcs, caches, Arrays.asList("sse", "avx"));
            }

            @Override
            protected ProcessorIdentifier queryProcessorId() {
                return new ProcessorIdentifier("Vendor", "HybridCPU", "6", "42", "1", "0000000000000000", true);
            }

            @Override
            protected long[] queryCurrentFreq() {
                return new long[] { 3_000_000_000L };
            }

            @Override
            protected long queryContextSwitches() {
                return 0L;
            }

            @Override
            protected long queryInterrupts() {
                return 0L;
            }

            @Override
            protected long[] querySystemCpuLoadTicks() {
                return new long[TickType.values().length];
            }

            @Override
            protected long[][] queryProcessorCpuLoadTicks() {
                return new long[4][TickType.values().length];
            }

            @Override
            public double[] getSystemLoadAverage(int nelem) {
                return new double[] { 1.0 };
            }
        };
    }

    @Test
    void testExplicitPhysicalProcessorsAndCaches() {
        AbstractCentralProcessor cpu = createHybridProcessor();
        assertThat(cpu.getPhysicalProcessorCount(), is(4));
        assertThat(cpu.getLogicalProcessorCount(), is(4));
        assertThat(cpu.getPhysicalPackageCount(), is(1));
        assertThat(cpu.getProcessorCaches().size(), is(1));
        assertThat(cpu.getFeatureFlags().size(), is(2));
    }

    @Test
    void testToStringHybridCores() {
        String s = createHybridProcessor().toString();
        assertThat(s, containsString("2 performance + 2 efficiency"));
    }

    @Test
    void testGetCurrentFreqPadding() {
        // createHybridProcessor returns 1 freq for 4 logical processors
        // getCurrentFreq should pad to match logical processor count
        AbstractCentralProcessor cpu = createHybridProcessor();
        long[] freqs = cpu.getCurrentFreq();
        assertThat(freqs.length, is(4));
        for (long f : freqs) {
            assertThat(f, is(3_000_000_000L));
        }
    }

    @Test
    void testCreateProcListFromDmesg() {
        AbstractCentralProcessor cpu = createProcessor();
        List<LogicalProcessor> logProcs = Arrays.asList(new LogicalProcessor(0, 0, 0), new LogicalProcessor(1, 1, 0));
        Map<Integer, String> dmesg = new HashMap<>();
        dmesg.put(0, "ARM Cortex-A73");
        dmesg.put(1, "ARM Cortex-A53");

        List<PhysicalProcessor> result = cpu.createProcListFromDmesg(logProcs, dmesg);
        assertThat(result.size(), is(2));
        // A73 (>=70) should be efficiency 1 (big), A53 (<70) should be 0 (LITTLE)
        assertThat(result.get(0).getEfficiency(), is(1));
        assertThat(result.get(1).getEfficiency(), is(0));
    }

    @Test
    void testCreateProcListFromDmesgAppleSilicon() {
        AbstractCentralProcessor cpu = createProcessor();
        List<LogicalProcessor> logProcs = Arrays.asList(new LogicalProcessor(0, 0, 0), new LogicalProcessor(1, 1, 0));
        Map<Integer, String> dmesg = new HashMap<>();
        dmesg.put(0, "Apple Firestorm");
        dmesg.put(1, "Apple Icestorm");

        List<PhysicalProcessor> result = cpu.createProcListFromDmesg(logProcs, dmesg);
        assertThat(result.size(), is(2));
        assertThat(result.get(0).getEfficiency(), is(1)); // Firestorm = performance
        assertThat(result.get(1).getEfficiency(), is(0)); // Icestorm = efficiency
    }

    @Test
    void testCreateProcListFromDmesgHomogeneous() {
        AbstractCentralProcessor cpu = createProcessor();
        List<LogicalProcessor> logProcs = Arrays.asList(new LogicalProcessor(0, 0, 0), new LogicalProcessor(1, 1, 0));
        Map<Integer, String> dmesg = new HashMap<>();
        dmesg.put(0, "ARM Cortex-A73");
        dmesg.put(1, "ARM Cortex-A73");

        List<PhysicalProcessor> result = cpu.createProcListFromDmesg(logProcs, dmesg);
        assertThat(result, hasSize(2));
        // All same type = not hybrid, all efficiency 0
        for (PhysicalProcessor pp : result) {
            assertThat(pp.getEfficiency(), is(0));
        }
    }

    @Test
    void testCreateProcessorIDAllFlags() {
        String[] allFlags = { "fpu", "vme", "de", "pse", "tsc", "msr", "pae", "mce", "cx8", "apic", "sep", "mtrr",
                "pge", "mca", "cmov", "pat", "pse-36", "psn", "clfsh", "ds", "acpi", "mmx", "fxsr", "sse", "sse2", "ss",
                "htt", "tm", "ia64", "pbe" };
        String id = AbstractCentralProcessor.createProcessorID("0", "0", "0", allFlags);
        assertThat(id, is("FFEFFBFF00000000"));
    }

    @Test
    void testProcessorCpuLoadBetweenTicksWrongLength() {
        // Outer array length mismatch
        assertThrows(IllegalArgumentException.class,
                () -> createProcessor().getProcessorCpuLoadBetweenTicks(new long[2][2], new long[1][1]));
        // Matching outer length but wrong inner subarray length
        assertThrows(IllegalArgumentException.class,
                () -> createProcessor().getProcessorCpuLoadBetweenTicks(new long[1][2], new long[1][2]));
    }

    @Test
    void testSystemCpuLoadBetweenTicksZeroTotal() {
        long[] ticks = new long[TickType.values().length];
        double load = createProcessor().getSystemCpuLoadBetweenTicks(ticks, ticks);
        assertThat(load, is(0d));
    }
}
