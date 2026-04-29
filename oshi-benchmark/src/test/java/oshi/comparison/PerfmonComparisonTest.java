/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.comparison;

import static org.assertj.core.api.Assertions.assertThat;
import static oshi.comparison.ComparisonAssertions.assertWithinRatio;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;

import oshi.driver.common.windows.perfmon.GpuInformation.GpuEngineProperty;
import oshi.driver.common.windows.perfmon.MemoryInformation.PageSwapProperty;
import oshi.driver.common.windows.perfmon.PagingFile.PagingPercentProperty;
import oshi.driver.common.windows.perfmon.PdhCounterProperty;
import oshi.driver.common.windows.perfmon.PdhCounterWildcardProperty;
import oshi.driver.common.windows.perfmon.PhysicalDisk.PhysicalDiskProperty;
import oshi.driver.common.windows.perfmon.ProcessInformation.HandleCountProperty;
import oshi.driver.common.windows.perfmon.ProcessInformation.IdleProcessorTimeProperty;
import oshi.driver.common.windows.perfmon.ProcessInformation.ProcessPerformanceProperty;
import oshi.driver.common.windows.perfmon.ProcessorInformation.InterruptsProperty;
import oshi.driver.common.windows.perfmon.ProcessorInformation.ProcessorFrequencyProperty;
import oshi.driver.common.windows.perfmon.ProcessorInformation.ProcessorTickCountProperty;
import oshi.driver.common.windows.perfmon.ProcessorInformation.ProcessorUtilityTickCountProperty;
import oshi.driver.common.windows.perfmon.ProcessorInformation.SystemTickCountProperty;
import oshi.driver.common.windows.perfmon.SystemInformation.ContextSwitchProperty;
import oshi.driver.common.windows.perfmon.SystemInformation.ProcessorQueueLengthProperty;
import oshi.driver.common.windows.perfmon.ThreadInformation.ThreadPerformanceProperty;
import oshi.driver.windows.perfmon.GpuInformationFFM;
import oshi.driver.windows.perfmon.GpuInformationJNA;
import oshi.driver.windows.perfmon.MemoryInformationFFM;
import oshi.driver.windows.perfmon.MemoryInformationJNA;
import oshi.driver.windows.perfmon.PagingFileFFM;
import oshi.driver.windows.perfmon.PagingFileJNA;
import oshi.driver.windows.perfmon.PhysicalDiskFFM;
import oshi.driver.windows.perfmon.PhysicalDiskJNA;
import oshi.driver.windows.perfmon.ProcessInformationFFM;
import oshi.driver.windows.perfmon.ProcessInformationJNA;
import oshi.driver.windows.perfmon.ProcessorInformationFFM;
import oshi.driver.windows.perfmon.ProcessorInformationJNA;
import oshi.driver.windows.perfmon.SystemInformationFFM;
import oshi.driver.windows.perfmon.SystemInformationJNA;
import oshi.driver.windows.perfmon.ThreadInformationFFM;
import oshi.driver.windows.perfmon.ThreadInformationJNA;
import oshi.util.PlatformEnum;
import oshi.util.tuples.Pair;

/**
 * Compares JNA and FFM perfmon driver implementations to verify they return equivalent results. Also validates PDH vs
 * WMI consistency for both implementations.
 */
@DisabledIf("isNotWindows")
class PerfmonComparisonTest {

    // Cumulative counters grow between queries; 50% tolerance for timing differences
    private static final double CUMULATIVE_RATIO = 0.5;
    // Instantaneous gauges should be close
    private static final double GAUGE_RATIO = 0.25;

    @Test
    void testPageSwaps() {
        Map<PageSwapProperty, Long> jna = MemoryInformationJNA.queryPageSwaps();
        Map<PageSwapProperty, Long> ffm = MemoryInformationFFM.queryPageSwaps();
        assertNonWildcardKeysMatch(jna, ffm, "PageSwap");
        assertNonWildcardValuesClose(jna, ffm, CUMULATIVE_RATIO, "PageSwap");
    }

    @Test
    void testSwapUsed() {
        Map<PagingPercentProperty, Long> jna = PagingFileJNA.querySwapUsed();
        Map<PagingPercentProperty, Long> ffm = PagingFileFFM.querySwapUsed();
        assertNonWildcardKeysMatch(jna, ffm, "SwapUsed");
        assertNonWildcardValuesClose(jna, ffm, GAUGE_RATIO, "SwapUsed");
    }

    @Test
    void testDiskCounters() {
        Pair<List<String>, Map<PhysicalDiskProperty, List<Long>>> jna = PhysicalDiskJNA.queryDiskCounters();
        Pair<List<String>, Map<PhysicalDiskProperty, List<Long>>> ffm = PhysicalDiskFFM.queryDiskCounters();
        assertWildcardStructureMatch(jna, ffm, "Disk");
    }

    @Test
    void testProcessCounters() {
        Pair<List<String>, Map<ProcessPerformanceProperty, List<Long>>> jna = ProcessInformationJNA
                .queryProcessCounters();
        Pair<List<String>, Map<ProcessPerformanceProperty, List<Long>>> ffm = ProcessInformationFFM
                .queryProcessCounters();
        assertThat(ffm.getB().keySet()).as("Process counter keys").isEqualTo(jna.getB().keySet());
    }

    @Test
    void testHandles() {
        Map<HandleCountProperty, Long> jna = ProcessInformationJNA.queryHandles();
        Map<HandleCountProperty, Long> ffm = ProcessInformationFFM.queryHandles();
        assertNonWildcardKeysMatch(jna, ffm, "Handles");
        assertNonWildcardValuesClose(jna, ffm, GAUGE_RATIO, "Handles");
    }

    @Test
    void testIdleProcessCounters() {
        Pair<List<String>, Map<IdleProcessorTimeProperty, List<Long>>> jna = ProcessInformationJNA
                .queryIdleProcessCounters();
        Pair<List<String>, Map<IdleProcessorTimeProperty, List<Long>>> ffm = ProcessInformationFFM
                .queryIdleProcessCounters();
        assertWildcardStructureMatch(jna, ffm, "IdleProcess");
    }

    @Test
    void testProcessorCounters() {
        Pair<List<String>, Map<ProcessorTickCountProperty, List<Long>>> jna = ProcessorInformationJNA
                .queryProcessorCounters();
        Pair<List<String>, Map<ProcessorTickCountProperty, List<Long>>> ffm = ProcessorInformationFFM
                .queryProcessorCounters();
        assertWildcardStructureMatch(jna, ffm, "Processor");
    }

    @Test
    void testSystemCounters() {
        Map<SystemTickCountProperty, Long> jna = ProcessorInformationJNA.querySystemCounters();
        Map<SystemTickCountProperty, Long> ffm = ProcessorInformationFFM.querySystemCounters();
        assertNonWildcardKeysMatch(jna, ffm, "System");
        assertNonWildcardValuesClose(jna, ffm, CUMULATIVE_RATIO, "System");
    }

    @Test
    void testProcessorCapacityCounters() {
        Pair<List<String>, Map<ProcessorUtilityTickCountProperty, List<Long>>> jna = ProcessorInformationJNA
                .queryProcessorCapacityCounters();
        Pair<List<String>, Map<ProcessorUtilityTickCountProperty, List<Long>>> ffm = ProcessorInformationFFM
                .queryProcessorCapacityCounters();
        assertWildcardStructureMatch(jna, ffm, "ProcessorCapacity");
    }

    @Test
    void testInterruptCounters() {
        Map<InterruptsProperty, Long> jna = ProcessorInformationJNA.queryInterruptCounters();
        Map<InterruptsProperty, Long> ffm = ProcessorInformationFFM.queryInterruptCounters();
        assertNonWildcardKeysMatch(jna, ffm, "Interrupts");
        assertNonWildcardValuesClose(jna, ffm, CUMULATIVE_RATIO, "Interrupts");
    }

    @Test
    void testFrequencyCounters() {
        Pair<List<String>, Map<ProcessorFrequencyProperty, List<Long>>> jna = ProcessorInformationJNA
                .queryFrequencyCounters();
        Pair<List<String>, Map<ProcessorFrequencyProperty, List<Long>>> ffm = ProcessorInformationFFM
                .queryFrequencyCounters();
        assertWildcardStructureMatch(jna, ffm, "Frequency");
    }

    @Test
    void testContextSwitchCounters() {
        Map<ContextSwitchProperty, Long> jna = SystemInformationJNA.queryContextSwitchCounters();
        Map<ContextSwitchProperty, Long> ffm = SystemInformationFFM.queryContextSwitchCounters();
        assertNonWildcardKeysMatch(jna, ffm, "ContextSwitch");
        assertNonWildcardValuesClose(jna, ffm, CUMULATIVE_RATIO, "ContextSwitch");
    }

    @Test
    void testProcessorQueueLength() {
        Map<ProcessorQueueLengthProperty, Long> jna = SystemInformationJNA.queryProcessorQueueLength();
        Map<ProcessorQueueLengthProperty, Long> ffm = SystemInformationFFM.queryProcessorQueueLength();
        assertNonWildcardKeysMatch(jna, ffm, "QueueLength");
    }

    @Test
    void testThreadCounters() {
        Pair<List<String>, Map<ThreadPerformanceProperty, List<Long>>> jna = ThreadInformationJNA.queryThreadCounters();
        Pair<List<String>, Map<ThreadPerformanceProperty, List<Long>>> ffm = ThreadInformationFFM.queryThreadCounters();
        assertThat(ffm.getB().keySet()).as("Thread counter keys").isEqualTo(jna.getB().keySet());
    }

    @Test
    void testThreadCountersFiltered() {
        // Exercise the filtered overload with a known process name
        Pair<List<String>, Map<ThreadPerformanceProperty, List<Long>>> jna = ThreadInformationJNA
                .queryThreadCounters("java", -1);
        Pair<List<String>, Map<ThreadPerformanceProperty, List<Long>>> ffm = ThreadInformationFFM
                .queryThreadCounters("java", -1);
        assertThat(ffm.getB().keySet()).as("Filtered thread counter keys").isEqualTo(jna.getB().keySet());
        assertThat(ffm.getA().isEmpty()).as("Filtered thread instances empty").isEqualTo(jna.getA().isEmpty());
    }

    @Test
    void testGpuEngineCounters() {
        // GPU counters may not be available on all systems; just verify both return same structure
        Pair<List<String>, Map<GpuEngineProperty, List<Long>>> jna = GpuInformationJNA.queryGpuEngineCounters();
        Pair<List<String>, Map<GpuEngineProperty, List<Long>>> ffm = GpuInformationFFM.queryGpuEngineCounters();
        assertThat(ffm.getA().isEmpty()).as("GPU engine instances empty").isEqualTo(jna.getA().isEmpty());
    }

    // --- Helper methods ---

    private static <T extends Enum<T> & PdhCounterProperty> void assertNonWildcardKeysMatch(Map<T, Long> jna,
            Map<T, Long> ffm, String label) {
        assertThat(ffm.keySet()).as(label + " keys").isEqualTo(jna.keySet());
    }

    private static <T extends Enum<T> & PdhCounterProperty> void assertNonWildcardValuesClose(Map<T, Long> jna,
            Map<T, Long> ffm, double ratio, String label) {
        for (T key : jna.keySet()) {
            if (ffm.containsKey(key)) {
                assertWithinRatio(ffm.get(key), jna.get(key), ratio, label + "." + key.name());
            }
        }
    }

    private static <T extends Enum<T> & PdhCounterWildcardProperty> void assertWildcardStructureMatch(
            Pair<List<String>, Map<T, List<Long>>> jna, Pair<List<String>, Map<T, List<Long>>> ffm, String label) {
        assertThat(ffm.getA()).as(label + " instances").containsExactlyInAnyOrderElementsOf(jna.getA());
        assertThat(ffm.getB().keySet()).as(label + " counter keys").isEqualTo(jna.getB().keySet());
        for (T key : jna.getB().keySet()) {
            if (ffm.getB().containsKey(key)) {
                assertThat(ffm.getB().get(key)).as(label + "." + key.name() + " size")
                        .hasSameSizeAs(jna.getB().get(key));
            }
        }
    }

    static boolean isNotWindows() {
        return PlatformEnum.getCurrentPlatform() != PlatformEnum.WINDOWS;
    }
}
