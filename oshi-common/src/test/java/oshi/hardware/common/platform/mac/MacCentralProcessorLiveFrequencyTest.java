/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;
import static oshi.hardware.common.platform.mac.MacCentralProcessorTest.M3_PRO_EFFICIENCY_TABLE;
import static oshi.hardware.common.platform.mac.MacCentralProcessorTest.M3_PRO_PERFORMANCE_TABLE;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;

import oshi.hardware.common.platform.mac.MacCentralProcessorTest.StubArmCentralProcessor;
import oshi.util.GlobalConfig;

/**
 * Tests the live Apple Silicon frequency path, which reads a configuration property. Run in one thread, because the
 * methods of a test class otherwise run concurrently and would race each other setting and clearing it.
 */
@Execution(SAME_THREAD)
class MacCentralProcessorLiveFrequencyTest {

    // Residency of a core that spent every active tick at one frequency, so the weighted average is that frequency.
    private static Map<String, Long> atFrequency(long[] table, int index) {
        Map<String, Long> states = new LinkedHashMap<>();
        states.put("IDLE", 1_000_000L);
        for (int i = 0; i < table.length; i++) {
            states.put("V" + i + "P" + (table.length - 1 - i), i == index ? 1_000L : 0L);
        }
        return states;
    }

    // Residency of a core over an interval so short that not one tick accumulated in any state.
    private static Map<String, Long> noTicks(long[] table) {
        Map<String, Long> states = atFrequency(table, -1);
        states.put("IDLE", 0L);
        return states;
    }

    // Residency of the eight cores of StubArmCentralProcessor, each parked at one index of its cluster's table.
    private static Map<String, Map<String, Long>> residencyAt(int efficiencyIndex, int performanceIndex) {
        Map<String, Map<String, Long>> residency = new LinkedHashMap<>();
        for (int i = 0; i < 4; i++) {
            residency.put("ECPU" + i, atFrequency(M3_PRO_EFFICIENCY_TABLE, efficiencyIndex));
            residency.put("PCPU" + i, atFrequency(M3_PRO_PERFORMANCE_TABLE, performanceIndex));
        }
        return residency;
    }

    // A sample holding only per-core residency, as a chip whose CPU complex channels could not be read reports.
    private static CpuResidencySample cores(Map<String, Map<String, Long>> coreStates) {
        return new CpuResidencySample(coreStates, Collections.<String, Map<String, Long>>emptyMap());
    }

    private static IOReportCpuSampler samplerAt(int efficiencyIndex, int performanceIndex) {
        return new StubCpuSampler(() -> cores(residencyAt(efficiencyIndex, performanceIndex)));
    }

    private static StubArmCentralProcessor liveCpu(@Nullable IOReportCpuSampler sampler) {
        GlobalConfig.set(GlobalConfig.OSHI_OS_MAC_CPU_FREQUENCY_IOREPORT, true);
        StubArmCentralProcessor cpu = new StubArmCentralProcessor();
        cpu.setSampler(sampler);
        return cpu;
    }

    @AfterEach
    void clearLiveFrequencyProperty() {
        GlobalConfig.remove(GlobalConfig.OSHI_OS_MAC_CPU_FREQUENCY_IOREPORT);
    }

    @Test
    void testLiveFrequencyReplacesTheNominalOnePerCore() {
        // The efficiency cores at their third frequency and the performance cores at their tenth, which are values
        // neither cluster's nominal maximum could produce
        StubArmCentralProcessor cpu = liveCpu(samplerAt(2, 9));
        long[] freqs = cpu.queryCurrentFreq();
        assertThat(freqs.length, is(8));
        for (int i = 0; i < 4; i++) {
            assertThat("efficiency core " + i, freqs[i], is(M3_PRO_EFFICIENCY_TABLE[2]));
        }
        for (int i = 4; i < 8; i++) {
            assertThat("performance core " + i, freqs[i], is(M3_PRO_PERFORMANCE_TABLE[9]));
        }
    }

    @Test
    void testEachClusterIsWeightedAgainstItsOwnTable() {
        // Both clusters parked at index 0. Reading one cluster against the other's table would report the other
        // cluster's lowest frequency, which is the mistake the per-core-type grouping exists to prevent.
        long[] freqs = liveCpu(samplerAt(0, 0)).queryCurrentFreq();
        assertThat("efficiency core", freqs[0], is(M3_PRO_EFFICIENCY_TABLE[0]));
        assertThat("performance core", freqs[4], is(M3_PRO_PERFORMANCE_TABLE[0]));
    }

    @Test
    void testNoSampleYetReportsTheNominalFrequencies() {
        // The first sample has nothing to subtract from, so there is no interval to weight over
        long[] freqs = liveCpu(new StubCpuSampler(() -> null)).queryCurrentFreq();
        assertArrayEquals(new StubArmCentralProcessor().queryCurrentFreq(), freqs, "no previous sample");
        long[] empty = liveCpu(new StubCpuSampler(() -> cores(Collections.<String, Map<String, Long>>emptyMap())))
                .queryCurrentFreq();
        assertArrayEquals(new StubArmCentralProcessor().queryCurrentFreq(), empty, "nothing sampled");
    }

    @Test
    void testAnIntervalTooShortToAccumulateTicksReportsTheNominalFrequencies() {
        // Two samples can arrive close enough together that the 24 MHz tick counters have not moved. That is not an
        // idle core, which would report its cluster's lowest frequency, but no measurement at all.
        long[] freqs = liveCpu(new StubCpuSampler(() -> {
            Map<String, Map<String, Long>> residency = new LinkedHashMap<>();
            for (int i = 0; i < 4; i++) {
                residency.put("ECPU" + i, noTicks(M3_PRO_EFFICIENCY_TABLE));
                residency.put("PCPU" + i, noTicks(M3_PRO_PERFORMANCE_TABLE));
            }
            return cores(residency);
        })).queryCurrentFreq();
        assertArrayEquals(new StubArmCentralProcessor().queryCurrentFreq(), freqs, "nothing accumulated");
    }

    @Test
    void testChannelsThatDoNotMatchTheCoresReportTheNominalFrequencies() {
        long[] nominal = new StubArmCentralProcessor().queryCurrentFreq();
        // One core type where the processor reports two
        assertArrayEquals(nominal, liveCpu(new StubCpuSampler(() -> {
            Map<String, Map<String, Long>> residency = new LinkedHashMap<>();
            for (int i = 0; i < 8; i++) {
                residency.put("PCPU" + i, atFrequency(M3_PRO_PERFORMANCE_TABLE, 9));
            }
            return cores(residency);
        })).queryCurrentFreq(), "one core type for two efficiency classes");
        // The right number of core types, but the wrong number of cores in each
        assertArrayEquals(nominal, liveCpu(new StubCpuSampler(() -> {
            Map<String, Map<String, Long>> residency = new LinkedHashMap<>();
            residency.put("ECPU0", atFrequency(M3_PRO_EFFICIENCY_TABLE, 2));
            residency.put("ECPU1", atFrequency(M3_PRO_EFFICIENCY_TABLE, 2));
            for (int i = 0; i < 6; i++) {
                residency.put("PCPU" + i, atFrequency(M3_PRO_PERFORMANCE_TABLE, 9));
            }
            return cores(residency);
        })).queryCurrentFreq(), "two efficiency and six performance cores");
    }

    @Test
    void testACoreWhoseResidencyCannotBePairedKeepsItsNominalFrequency() {
        // A performance core reporting the efficiency cluster's state count cannot be weighted, but its three siblings
        // still can be
        long[] freqs = liveCpu(new StubCpuSampler(() -> {
            Map<String, Map<String, Long>> residency = residencyAt(2, 9);
            residency.put("PCPU1", atFrequency(M3_PRO_EFFICIENCY_TABLE, 2));
            return cores(residency);
        })).queryCurrentFreq();
        assertThat("unpairable core", freqs[5], is(StubArmCentralProcessor.PERF_FREQ));
        for (int i : new int[] { 4, 6, 7 }) {
            assertThat("sibling " + i, freqs[i], is(M3_PRO_PERFORMANCE_TABLE[9]));
        }
    }

    @Test
    void testACoreTypeThisReleaseDoesNotKnowReportsTheNominalFrequencies() {
        // A future chip could name its core types with letters this release has never seen, and an unknown letter sorts
        // last however fast that core really is. Here the counts agree and each group's state count even pairs with the
        // table it is wrongly given, so nothing downstream would catch it: the efficiency cores would be charged the
        // unknown cores' frequency table and report a plausible but wrong number. The whole sample is refused instead.
        long[] freqs = liveCpu(new StubCpuSampler(() -> {
            Map<String, Map<String, Long>> residency = new LinkedHashMap<>();
            for (int i = 0; i < 4; i++) {
                residency.put("XCPU" + i, atFrequency(M3_PRO_PERFORMANCE_TABLE, 19));
                residency.put("PCPU" + i, atFrequency(M3_PRO_EFFICIENCY_TABLE, 0));
            }
            return cores(residency);
        })).queryCurrentFreq();
        assertArrayEquals(new StubArmCentralProcessor().queryCurrentFreq(), freqs, "unrecognized core type");
    }

    // The four channels an M3 Pro publishes for its two clusters, each parked at one index of its table. The pair
    // ending
    // CPU reports the state the cores asked for and the pair ending CPM the state the hardware ran at.
    private static Map<String, Map<String, Long>> complexes(int efficiencyRealized, int performanceRealized) {
        Map<String, Map<String, Long>> complexes = new LinkedHashMap<>();
        complexes.put("ECPU", atFrequency(M3_PRO_EFFICIENCY_TABLE, M3_PRO_EFFICIENCY_TABLE.length - 1));
        complexes.put("ECPM", atFrequency(M3_PRO_EFFICIENCY_TABLE, efficiencyRealized));
        complexes.put("PCPU", atFrequency(M3_PRO_PERFORMANCE_TABLE, M3_PRO_PERFORMANCE_TABLE.length - 1));
        complexes.put("PCPM", atFrequency(M3_PRO_PERFORMANCE_TABLE, performanceRealized));
        return complexes;
    }

    @Test
    void testTheFrequencyTheClusterRanAtReplacesTheStateTheCoresAskedFor() {
        // Every core asks for the top state, as a busy core does, while the hardware runs its cluster several states
        // below that. Weighting the cores' own residency would report each cluster's nominal maximum, which is the
        // value
        // this path exists to replace, so the cluster's own residency decides.
        long[] freqs = liveCpu(new StubCpuSampler(() -> new CpuResidencySample(
                residencyAt(M3_PRO_EFFICIENCY_TABLE.length - 1, M3_PRO_PERFORMANCE_TABLE.length - 1),
                complexes(4, 13)))).queryCurrentFreq();
        for (int i = 0; i < 4; i++) {
            assertThat("efficiency core " + i, freqs[i], is(M3_PRO_EFFICIENCY_TABLE[4]));
        }
        for (int i = 4; i < 8; i++) {
            assertThat("performance core " + i, freqs[i], is(M3_PRO_PERFORMANCE_TABLE[13]));
        }
    }

    @Test
    void testACoreThatDidNotRunIsNotCreditedWithItsSiblingsWork() {
        // Its cluster ran fast for the cores that had work, but this core sat in its idle state throughout, so it
        // reports the lowest frequency its cluster runs at rather than a frequency it never ran at
        long[] freqs = liveCpu(new StubCpuSampler(() -> {
            Map<String, Map<String, Long>> residency = residencyAt(M3_PRO_EFFICIENCY_TABLE.length - 1,
                    M3_PRO_PERFORMANCE_TABLE.length - 1);
            residency.put("PCPU1", atFrequency(M3_PRO_PERFORMANCE_TABLE, -1));
            return new CpuResidencySample(residency, complexes(4, 13));
        })).queryCurrentFreq();
        assertThat("idle core", freqs[5], is(M3_PRO_PERFORMANCE_TABLE[0]));
        assertThat("busy sibling", freqs[4], is(M3_PRO_PERFORMANCE_TABLE[13]));
    }

    @Test
    void testClusterStatesThatDoNotMatchTheCoreTypesAreIgnored() {
        // One cluster reported where the cores report two core types. Which table weights which cluster would be a
        // guess, so the cores' own residency is all there is, as on a chip that publishes no cluster channel at all.
        long[] freqs = liveCpu(new StubCpuSampler(() -> {
            Map<String, Map<String, Long>> complexes = new LinkedHashMap<>();
            complexes.put("ECPM", atFrequency(M3_PRO_EFFICIENCY_TABLE, 4));
            return new CpuResidencySample(residencyAt(2, 9), complexes);
        })).queryCurrentFreq();
        assertThat("efficiency core", freqs[0], is(M3_PRO_EFFICIENCY_TABLE[2]));
        assertThat("performance core", freqs[4], is(M3_PRO_PERFORMANCE_TABLE[9]));
    }

    @Test
    void testTheSubscriptionIsNotAttemptedUnlessAskedFor() {
        StubArmCentralProcessor cpu = new StubArmCentralProcessor();
        cpu.setSampler(samplerAt(2, 9));
        assertArrayEquals(
                new long[] { StubArmCentralProcessor.EFF_FREQ, StubArmCentralProcessor.EFF_FREQ,
                        StubArmCentralProcessor.EFF_FREQ, StubArmCentralProcessor.EFF_FREQ,
                        StubArmCentralProcessor.PERF_FREQ, StubArmCentralProcessor.PERF_FREQ,
                        StubArmCentralProcessor.PERF_FREQ, StubArmCentralProcessor.PERF_FREQ },
                cpu.queryCurrentFreq(), "property unset");
        assertThat("subscription attempts", cpu.samplerRequests(), is(0));
    }

    @Test
    void testTheSubscriptionIsAttemptedOnlyOnce() {
        StubArmCentralProcessor cpu = liveCpu(null);
        cpu.queryCurrentFreq();
        cpu.queryCurrentFreq();
        // Memoized indefinitely, so a null caches too and a chip without these channels is not asked twice
        assertThat("subscription attempts", cpu.samplerRequests(), is(1));
    }

    /** A sampler whose every sample comes from a supplied fixture. */
    static class StubCpuSampler implements IOReportCpuSampler {

        private final Supplier<@Nullable CpuResidencySample> fixture;

        StubCpuSampler(Supplier<@Nullable CpuResidencySample> fixture) {
            this.fixture = fixture;
        }

        @Override
        public @Nullable CpuResidencySample sampleResidencyDelta() {
            return fixture.get();
        }

        @Override
        public void close() {
            // Nothing to release
        }
    }
}
