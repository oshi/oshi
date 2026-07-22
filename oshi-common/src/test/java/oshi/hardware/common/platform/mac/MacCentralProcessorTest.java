/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;

import oshi.hardware.CentralProcessor;
import oshi.hardware.CentralProcessor.TickType;

class MacCentralProcessorTest {

    @Test
    void testParseX86FeatureFlagsWithFeatures() {
        Map<String, String> strings = new HashMap<>();
        strings.put("machdep.cpu.brand_string", "Test CPU");
        strings.put("machdep.cpu.features", "FPU VME SSE SSE2 SSE3");
        strings.put("machdep.cpu.extfeatures", "LAHF EM64T");
        StubMacCentralProcessor cpu = new StubMacCentralProcessor(strings);

        List<String> flags = cpu.parseX86FeatureFlags();
        assertThat(flags, hasSize(2));
        assertThat(flags.get(0), is("machdep.cpu.features: FPU VME SSE SSE2 SSE3"));
        assertThat(flags.get(1), is("machdep.cpu.extfeatures: LAHF EM64T"));
    }

    @Test
    void testParseX86FeatureFlagsEmpty() {
        StubMacCentralProcessor cpu = new StubMacCentralProcessor(new HashMap<>());
        List<String> flags = cpu.parseX86FeatureFlags();
        assertThat(flags, is(empty()));
    }

    @Test
    void testQueryProcessorIdX86() {
        // A non-Apple brand_string routes queryProcessorId into the x86 (Intel) identity branch, which reads
        // machdep.cpu.* sysctls directly. Representative Intel values (Core 2 Duo, Penryn family 6 model 23).
        Map<String, String> strings = new HashMap<>();
        strings.put("machdep.cpu.brand_string", "Intel(R) Core(TM)2 Duo CPU L9400 @ 1.86GHz");
        strings.put("machdep.cpu.vendor", "GenuineIntel");
        Map<String, Integer> ints = new HashMap<>();
        ints.put("machdep.cpu.family", 6);
        ints.put("machdep.cpu.model", 23);
        ints.put("machdep.cpu.stepping", 10);
        ints.put("machdep.cpu.signature", 0x000106A5);
        Map<String, Long> longs = new HashMap<>();
        longs.put("machdep.cpu.feature_bits", 0xBFEBFBFFL);
        StubMacCentralProcessor cpu = new StubMacCentralProcessor(strings, ints, longs);

        CentralProcessor.ProcessorIdentifier id = cpu.queryProcessorId();
        assertThat(id.getVendor(), is("GenuineIntel"));
        assertThat(id.getName(), is("Intel(R) Core(TM)2 Duo CPU L9400 @ 1.86GHz"));
        assertThat(id.getFamily(), is("6"));
        assertThat(id.getModel(), is("23"));
        assertThat(id.getStepping(), is("10"));
        assertThat(id.isCpu64bit(), is(true));
        // processorID packs the signature in the low word and the feature bits in the high word
        assertThat(id.getProcessorID(), is(String.format(Locale.ROOT, "%016x", 0x000106A5L | (0xBFEBFBFFL << 32))));
    }

    @Test
    void testQuerySystemCpuLoadTicks() {
        // Mach cpu_ticks order: user, system, idle, nice
        StubMacCentralProcessor cpu = new StubMacCentralProcessor(new HashMap<>(), new int[] { 10, 30, 50, 20 },
                new int[0], new double[0], 0);
        long[] ticks = cpu.querySystemCpuLoadTicks();
        assertThat(ticks[TickType.USER.getIndex()], is(10L));
        assertThat(ticks[TickType.NICE.getIndex()], is(20L));
        assertThat(ticks[TickType.SYSTEM.getIndex()], is(30L));
        assertThat(ticks[TickType.IDLE.getIndex()], is(50L));
    }

    @Test
    void testQuerySystemCpuLoadTicksShortArrayLeavesZeros() {
        // Fewer than CPU_STATE_MAX values: mapping is skipped, ticks stay zero
        StubMacCentralProcessor cpu = new StubMacCentralProcessor(new HashMap<>(), new int[] { 10, 30, 50 }, new int[0],
                new double[0], 0);
        long[] ticks = cpu.querySystemCpuLoadTicks();
        for (long tick : ticks) {
            assertThat(tick, is(0L));
        }
    }

    @Test
    void testQueryProcessorCpuLoadTicks() {
        // One processor (the stub reports a single logical processor): user, system, idle, nice
        StubMacCentralProcessor cpu = new StubMacCentralProcessor(new HashMap<>(), new int[0],
                new int[] { 10, 30, 50, 20 }, new double[0], 0);
        long[][] ticks = cpu.queryProcessorCpuLoadTicks();
        assertThat(ticks.length, is(1));
        assertThat(ticks[0][TickType.USER.getIndex()], is(10L));
        assertThat(ticks[0][TickType.SYSTEM.getIndex()], is(30L));
        assertThat(ticks[0][TickType.IDLE.getIndex()], is(50L));
        assertThat(ticks[0][TickType.NICE.getIndex()], is(20L));
    }

    @Test
    void testQueryProcessorCpuLoadTicksCapsExtraProcessors() {
        // Native reports two processors but only one is expected: mapping caps to the expected count without error
        StubMacCentralProcessor cpu = new StubMacCentralProcessor(new HashMap<>(), new int[0],
                new int[] { 10, 30, 50, 20, 11, 31, 51, 21 }, new double[0], 0);
        long[][] ticks = cpu.queryProcessorCpuLoadTicks();
        assertThat(ticks.length, is(1));
        assertThat(ticks[0][TickType.USER.getIndex()], is(10L));
        assertThat(ticks[0][TickType.IDLE.getIndex()], is(50L));
    }

    @Test
    void testLoadAverageRejectsBelowOne() {
        StubMacCentralProcessor cpu = new StubMacCentralProcessor(new HashMap<>());
        assertThrows(IllegalArgumentException.class, () -> cpu.getSystemLoadAverage(0));
    }

    @Test
    void testLoadAverageRejectsAboveThree() {
        StubMacCentralProcessor cpu = new StubMacCentralProcessor(new HashMap<>());
        assertThrows(IllegalArgumentException.class, () -> cpu.getSystemLoadAverage(4));
    }

    @Test
    void testLoadAveragePreservesFullResult() {
        StubMacCentralProcessor cpu = new StubMacCentralProcessor(new HashMap<>(), new int[0], new int[0],
                new double[] { 1.0, 5.0, 15.0 }, 3);
        double[] avg = cpu.getSystemLoadAverage(3);
        assertThat(avg[0], is(1.0));
        assertThat(avg[1], is(5.0));
        assertThat(avg[2], is(15.0));
    }

    @Test
    void testLoadAveragePartialResultFillsNegative() {
        // Native call returns fewer samples than requested: all-or-nothing, whole array becomes -1
        StubMacCentralProcessor cpu = new StubMacCentralProcessor(new HashMap<>(), new int[0], new int[0],
                new double[] { 1.0, 5.0, 15.0 }, 2);
        double[] avg = cpu.getSystemLoadAverage(3);
        assertThat(avg[0], is(-1.0));
        assertThat(avg[1], is(-1.0));
        assertThat(avg[2], is(-1.0));
    }

    /**
     * Minimal stub providing sysctl values and raw Mach CPU-tick / load-average values without native calls. The
     * abstract superclass eagerly derives processor counts during construction, so the stub reports a single logical
     * processor; per-processor tests are written against that.
     */
    static class StubMacCentralProcessor extends MacCentralProcessor {

        private final Map<String, Integer> sysctlInts = new HashMap<>();
        private final Map<String, Long> sysctlLongs = new HashMap<>();
        private final Map<String, String> sysctlStrings;
        private final int[] hostCpuTicks;
        private final int[] processorCpuTicks;
        private final double[] loadavgValues;
        private final int loadavgRetval;

        StubMacCentralProcessor(Map<String, String> extraStrings) {
            this(extraStrings, new int[0], new int[0], new double[0], 0);
        }

        StubMacCentralProcessor(Map<String, String> extraStrings, Map<String, Integer> extraInts,
                Map<String, Long> extraLongs) {
            this(extraStrings, new int[0], new int[0], new double[0], 0);
            sysctlInts.putAll(extraInts);
            sysctlLongs.putAll(extraLongs);
        }

        StubMacCentralProcessor(Map<String, String> extraStrings, int[] hostCpuTicks, int[] processorCpuTicks,
                double[] loadavgValues, int loadavgRetval) {
            Map<String, String> defaults = new HashMap<>();
            defaults.put("machdep.cpu.brand_string", "Test CPU");
            defaults.putAll(extraStrings);
            this.sysctlStrings = defaults;
            sysctlInts.put("hw.logicalcpu", 1);
            sysctlInts.put("hw.physicalcpu", 1);
            sysctlInts.put("hw.packages", 1);
            sysctlInts.put("hw.cpu64bit_capable", 1);
            this.hostCpuTicks = hostCpuTicks;
            this.processorCpuTicks = processorCpuTicks;
            this.loadavgValues = loadavgValues;
            this.loadavgRetval = loadavgRetval;
        }

        @Override
        protected int sysctlInt(String name, int def) {
            return sysctlInts == null ? def : sysctlInts.getOrDefault(name, def);
        }

        @Override
        protected int sysctlIntNoWarn(String name, int def) {
            return sysctlInts == null ? def : sysctlInts.getOrDefault(name, def);
        }

        @Override
        protected long sysctlLong(String name, long def) {
            return sysctlLongs == null ? def : sysctlLongs.getOrDefault(name, def);
        }

        @Override
        protected String sysctlString(String name, String def) {
            return sysctlStrings == null ? def : sysctlStrings.getOrDefault(name, def);
        }

        @Override
        protected String sysctlStringNoWarn(String name, String def) {
            return sysctlStrings == null ? def : sysctlStrings.getOrDefault(name, def);
        }

        @Override
        protected SysctlProvider sysctlProvider() {
            return null; // Not used; sysctl methods are overridden directly
        }

        @Override
        protected IOKitProvider ioKitProvider() {
            return null; // Not used; platformExpert/queryCompatibleStrings/calculateNominalFrequencies are overridden
        }

        @Override
        protected String platformExpert() {
            return "Apple";
        }

        @Override
        protected Map<Integer, String> queryCompatibleStrings() {
            return new HashMap<>();
        }

        @Override
        protected void calculateNominalFrequencies() {
        }

        @Override
        protected int[] queryHostCpuLoadTicks() {
            return hostCpuTicks;
        }

        @Override
        protected int[] queryProcessorCpuTicks() {
            return processorCpuTicks;
        }

        @Override
        protected int getloadavgNative(double[] loadavg, int nelem) {
            for (int i = 0; i < nelem && i < loadavgValues.length; i++) {
                loadavg[i] = loadavgValues[i];
            }
            return loadavgRetval;
        }

        @Override
        public long queryMaxFreq() {
            return 0L;
        }

        @Override
        public long[] queryCurrentFreq() {
            return new long[] { 0L };
        }

        @Override
        public long queryContextSwitches() {
            return 0L;
        }

        @Override
        public long queryInterrupts() {
            return 0L;
        }
    }
}
