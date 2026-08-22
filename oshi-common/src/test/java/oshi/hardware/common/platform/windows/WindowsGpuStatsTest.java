/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import oshi.driver.common.windows.perfmon.GpuInformation.GpuAdapterMemoryProperty;
import oshi.driver.common.windows.perfmon.GpuInformation.GpuEngineProperty;
import oshi.driver.common.windows.wmi.LhmSensor.LhmSensorProperty;
import oshi.driver.common.windows.wmi.WmiConstants;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.hardware.GpuTicks;
import oshi.util.tuples.Pair;

/**
 * Tests the shared Windows GpuStats logic without Windows, by stubbing every native dispatch point. Each metric is
 * sourced from a chain of NVML, ADL, LibreHardwareMonitor and PDH counters, so the assertions pin the source
 * <em>priority</em> and not only that some source answers.
 */
class WindowsGpuStatsTest {

    private static final double EPS = 1e-6;

    private static final String LUID = "luid_0x00000000_0x0000c3f7_phys_0";
    private static final String LHM_PARENT = "/gpu-nvidia/0";
    private static final String PCI_BUS_ID = "0000:01:00.0";
    private static final String CARD_NAME = "NVIDIA GeForce RTX 4090";
    private static final int PCI_BUS_NUMBER = 1;

    private static final String NVML_BY_BUS = "nvml-device-0";
    private static final String NVML_BY_NAME = "nvml-by-name";
    private static final int ADL_INDEX = 3;

    /** The metrics a single vendor library can answer. Every one defaults to the unavailable sentinel. */
    private static final class VendorMetrics {
        private double utilization = -1d;
        private double temperature = -1d;
        private double powerDraw = -1d;
        private long coreClock = -1L;
        private long memoryClock = -1L;
        private double fanSpeed = -1d;

        VendorMetrics utilization(double value) {
            this.utilization = value;
            return this;
        }

        VendorMetrics temperature(double value) {
            this.temperature = value;
            return this;
        }

        VendorMetrics powerDraw(double value) {
            this.powerDraw = value;
            return this;
        }

        VendorMetrics coreClock(long value) {
            this.coreClock = value;
            return this;
        }

        VendorMetrics memoryClock(long value) {
            this.memoryClock = value;
            return this;
        }

        VendorMetrics fanSpeed(double value) {
            this.fanSpeed = value;
            return this;
        }

        /** Fills every metric with a distinct value so a test can tell which source answered. */
        VendorMetrics all(double base) {
            return utilization(base).temperature(base + 1).powerDraw(base + 2).coreClock((long) base + 3)
                    .memoryClock((long) base + 4).fanSpeed(base + 5);
        }
    }

    /** A {@link WmiResult} over an in-memory list of LHM sensor rows. */
    private static final class FakeSensorResult implements WmiResult<LhmSensorProperty> {
        private final List<String> names = new ArrayList<>();
        private final List<Float> values = new ArrayList<>();

        FakeSensorResult add(String name, double value) {
            names.add(name);
            values.add((float) value);
            return this;
        }

        @Override
        public int getResultCount() {
            return names.size();
        }

        @Override
        public @Nullable Object getValue(LhmSensorProperty property, int index) {
            return switch (property) {
                case NAME -> names.get(index);
                case VALUE -> values.get(index);
                default -> null;
            };
        }

        @Override
        public int getVtType(LhmSensorProperty property) {
            return property == LhmSensorProperty.VALUE ? WmiConstants.VT_R4 : WmiConstants.VT_BSTR;
        }

        @Override
        public int getCIMType(LhmSensorProperty property) {
            return property == LhmSensorProperty.VALUE ? WmiConstants.CIM_REAL32 : WmiConstants.CIM_STRING;
        }
    }

    /** Stubs all twenty native dispatch points, so a test can enable exactly the sources it wants to observe. */
    private static final class StubWindowsGpuStats extends WindowsGpuStats {

        private final VendorMetrics nvml = new VendorMetrics();
        private final VendorMetrics adl = new VendorMetrics();
        private final Map<String, FakeSensorResult> lhmSensors = new HashMap<>();

        private boolean nvmlAvailable;
        private boolean adlAvailable;
        private @Nullable String nvmlDeviceByBusId = NVML_BY_BUS;

        private int nvmlFindDeviceCalls;
        private int adlFindAdapterIndexCalls;

        private List<String> engineInstances = Collections.emptyList();
        private Map<GpuEngineProperty, List<Long>> engineValues = Collections.emptyMap();
        private List<String> memoryInstances = Collections.emptyList();
        private Map<GpuAdapterMemoryProperty, List<Long>> memoryValues = Collections.emptyMap();

        StubWindowsGpuStats(String luidPrefix, String lhmParent, int pciBusNumber, String pciBusId, String cardName) {
            super(luidPrefix, lhmParent, pciBusNumber, pciBusId, cardName);
        }

        static StubWindowsGpuStats nvidia() {
            return new StubWindowsGpuStats(LUID, LHM_PARENT, PCI_BUS_NUMBER, PCI_BUS_ID, CARD_NAME);
        }

        StubWindowsGpuStats withNvml() {
            this.nvmlAvailable = true;
            return this;
        }

        StubWindowsGpuStats withAdl() {
            this.adlAvailable = true;
            return this;
        }

        StubWindowsGpuStats withLhmSensor(String sensorType, String sensorName, double value) {
            FakeSensorResult result = lhmSensors.get(sensorType);
            if (result == null) {
                result = new FakeSensorResult();
                lhmSensors.put(sensorType, result);
            }
            result.add(sensorName, value);
            return this;
        }

        /** Publishes one engine counter instance belonging to this adapter, plus one belonging to another. */
        StubWindowsGpuStats withEngineTicks(long runningTime, long runningTimeBase) {
            this.engineInstances = List.of(LUID + "_engtype_3D", "luid_0x00000000_0x0000ffff_engtype_3D");
            Map<GpuEngineProperty, List<Long>> values = new HashMap<>();
            values.put(GpuEngineProperty.RUNNING_TIME, List.of(runningTime, 999_999L));
            values.put(GpuEngineProperty.RUNNING_TIME_BASE, List.of(runningTimeBase, 999_999L));
            this.engineValues = values;
            return this;
        }

        StubWindowsGpuStats withAdapterMemory(long dedicated, long shared) {
            this.memoryInstances = List.of("luid_0x00000000_0x0000ffff_phys_0", LUID);
            Map<GpuAdapterMemoryProperty, List<Long>> values = new HashMap<>();
            values.put(GpuAdapterMemoryProperty.DEDICATED_USAGE, List.of(-1L, dedicated));
            values.put(GpuAdapterMemoryProperty.SHARED_USAGE, List.of(-1L, shared));
            this.memoryValues = values;
            return this;
        }

        @Override
        protected Pair<List<String>, Map<GpuEngineProperty, List<Long>>> queryGpuEngineCounters() {
            return new Pair<>(engineInstances, engineValues);
        }

        @Override
        protected Pair<List<String>, Map<GpuAdapterMemoryProperty, List<Long>>> queryGpuAdapterMemoryCounters() {
            return new Pair<>(memoryInstances, memoryValues);
        }

        @Override
        protected WmiResult<LhmSensorProperty> queryLhmSensors(String parent, String sensorType) {
            FakeSensorResult result = LHM_PARENT.equals(parent) ? lhmSensors.get(sensorType) : null;
            return result == null ? new FakeSensorResult() : result;
        }

        @Override
        protected boolean isNvmlAvailable() {
            return nvmlAvailable;
        }

        @Override
        protected @Nullable String nvmlFindDevice(String pciBusId) {
            nvmlFindDeviceCalls++;
            return nvmlDeviceByBusId;
        }

        @Override
        protected @Nullable String nvmlFindDeviceByName(String gpuName) {
            return CARD_NAME.equals(gpuName) ? NVML_BY_NAME : null;
        }

        @Override
        protected double nvmlGetGpuUtilization(String device) {
            return nvml.utilization;
        }

        @Override
        protected double nvmlGetTemperature(String device) {
            return nvml.temperature;
        }

        @Override
        protected double nvmlGetPowerDraw(String device) {
            return nvml.powerDraw;
        }

        @Override
        protected long nvmlGetCoreClockMhz(String device) {
            return nvml.coreClock;
        }

        @Override
        protected long nvmlGetMemoryClockMhz(String device) {
            return nvml.memoryClock;
        }

        @Override
        protected double nvmlGetFanSpeedPercent(String device) {
            return nvml.fanSpeed;
        }

        @Override
        protected boolean isAdlAvailable() {
            return adlAvailable;
        }

        @Override
        protected int adlFindAdapterIndex(int pciBusNumber) {
            adlFindAdapterIndexCalls++;
            return PCI_BUS_NUMBER == pciBusNumber ? ADL_INDEX : -1;
        }

        @Override
        protected double adlGetGpuUtilization(int adapterIndex) {
            return adl.utilization;
        }

        @Override
        protected double adlGetTemperature(int adapterIndex) {
            return adl.temperature;
        }

        @Override
        protected double adlGetPowerDraw(int adapterIndex) {
            return adl.powerDraw;
        }

        @Override
        protected long adlGetCoreClockMhz(int adapterIndex) {
            return adl.coreClock;
        }

        @Override
        protected long adlGetMemoryClockMhz(int adapterIndex) {
            return adl.memoryClock;
        }

        @Override
        protected double adlGetFanSpeedPercent(int adapterIndex) {
            return adl.fanSpeed;
        }
    }

    /** Adds every LHM sensor the class knows how to read, at values distinct from the vendor stubs. */
    private static StubWindowsGpuStats withAllLhmSensors(StubWindowsGpuStats stats, double base) {
        return stats.withLhmSensor("Load", "GPU Core", base).withLhmSensor("Temperature", "GPU Core", base + 1)
                .withLhmSensor("Power", "GPU Package", base + 2).withLhmSensor("Clock", "GPU Core", base + 3)
                .withLhmSensor("Clock", "GPU Memory", base + 4).withLhmSensor("Control", "GPU Fan", base + 5);
    }

    private static void assertAllMetrics(StubWindowsGpuStats stats, double base) {
        assertThat(stats.getGpuUtilization(), closeTo(base, EPS));
        assertThat(stats.getTemperature(), closeTo(base + 1, EPS));
        assertThat(stats.getPowerDraw(), closeTo(base + 2, EPS));
        assertThat(stats.getCoreClockMhz(), is((long) base + 3));
        assertThat(stats.getMemoryClockMhz(), is((long) base + 4));
        assertThat(stats.getFanSpeedPercent(), closeTo(base + 5, EPS));
    }

    // -------------------------------------------------------------------------
    // Source priority — NVML outranks ADL outranks LHM, for every metric
    // -------------------------------------------------------------------------

    @Test
    void testAllMetricsPreferNvml() {
        try (StubWindowsGpuStats stats = StubWindowsGpuStats.nvidia().withNvml().withAdl()) {
            stats.nvml.all(10);
            stats.adl.all(50);
            withAllLhmSensors(stats, 80).withEngineTicks(700L, 1000L);
            assertAllMetrics(stats, 10);
        }
    }

    @Test
    void testAllMetricsFallBackToAdl() {
        // NVML resolves a device but answers nothing, as on an AMD card in a machine that also has the NVIDIA driver
        try (StubWindowsGpuStats stats = StubWindowsGpuStats.nvidia().withNvml().withAdl()) {
            stats.adl.all(50);
            withAllLhmSensors(stats, 80).withEngineTicks(700L, 1000L);
            assertAllMetrics(stats, 50);
        }
    }

    @Test
    void testAllMetricsFallBackToLhm() {
        try (StubWindowsGpuStats stats = StubWindowsGpuStats.nvidia()) {
            withAllLhmSensors(stats, 80).withEngineTicks(700L, 1000L);
            assertAllMetrics(stats, 80);
        }
    }

    @Test
    void testAllMetricsReturnSentinelWithNoSource() {
        try (StubWindowsGpuStats stats = StubWindowsGpuStats.nvidia()) {
            assertThat(stats.getGpuUtilization(), is(-1d));
            assertThat(stats.getTemperature(), is(-1d));
            assertThat(stats.getPowerDraw(), is(-1d));
            assertThat(stats.getCoreClockMhz(), is(-1L));
            assertThat(stats.getMemoryClockMhz(), is(-1L));
            assertThat(stats.getFanSpeedPercent(), is(-1d));
        }
    }

    @Test
    void testAlternateLhmSensorNames() {
        // Power and fan each have a second name LHM may use instead of the first
        try (StubWindowsGpuStats stats = StubWindowsGpuStats.nvidia()) {
            stats.withLhmSensor("Power", "GPU Power", 210).withLhmSensor("Control", "GPU Fan 1", 62);
            assertThat(stats.getPowerDraw(), closeTo(210, EPS));
            assertThat(stats.getFanSpeedPercent(), closeTo(62, EPS));
        }
    }

    // -------------------------------------------------------------------------
    // Utilization — the PDH engine tick delta is the last resort, not the first
    // -------------------------------------------------------------------------

    @Test
    void testUtilizationFallsBackToEngineTicks() {
        try (StubWindowsGpuStats stats = StubWindowsGpuStats.nvidia().withEngineTicks(1000L, 5000L)) {
            // The tick path needs two samples, so the first call only seeds the baseline
            assertThat(stats.getGpuUtilization(), is(-1d));
            stats.withEngineTicks(1300L, 5700L);
            // 300 more active ticks of a 700-tick window
            assertThat(stats.getGpuUtilization(), closeTo(300.0 * 100 / 700, EPS));
        }
    }

    @Test
    void testUtilizationIgnoresEngineTicksWhenVendorAnswers() {
        try (StubWindowsGpuStats stats = StubWindowsGpuStats.nvidia().withNvml().withEngineTicks(1000L, 5000L)) {
            stats.nvml.utilization(42);
            assertThat(stats.getGpuUtilization(), closeTo(42, EPS));
            stats.withEngineTicks(1300L, 5700L);
            assertThat(stats.getGpuUtilization(), closeTo(42, EPS));
        }
    }

    @Test
    void testUtilizationReturnsSentinelWhenTicksGoBackwards() {
        try (StubWindowsGpuStats stats = StubWindowsGpuStats.nvidia().withEngineTicks(1000L, 5000L)) {
            assertThat(stats.getGpuUtilization(), is(-1d));
            // A counter reset, as when the adapter's PDH instance is recreated
            stats.withEngineTicks(10L, 50L);
            assertThat(stats.getGpuUtilization(), is(-1d));
            // The reset sample becomes the new baseline, so the next window is measurable again
            stats.withEngineTicks(110L, 250L);
            assertThat(stats.getGpuUtilization(), closeTo(100.0 * 100 / 200, EPS));
        }
    }

    @Test
    void testUtilizationReturnsSentinelWhenNoTicksElapse() {
        try (StubWindowsGpuStats stats = StubWindowsGpuStats.nvidia().withEngineTicks(1000L, 5000L)) {
            assertThat(stats.getGpuUtilization(), is(-1d));
            assertThat(stats.getGpuUtilization(), is(-1d));
        }
    }

    // -------------------------------------------------------------------------
    // GPU ticks
    // -------------------------------------------------------------------------

    @Test
    void testGpuTicksSumsOnlyThisAdaptersInstances() {
        try (StubWindowsGpuStats stats = StubWindowsGpuStats.nvidia().withEngineTicks(1000L, 5000L)) {
            GpuTicks ticks = stats.getGpuTicks();
            assertThat(ticks.getActiveTicks(), is(1000L));
            assertThat(ticks.getIdleTicks(), is(4000L));
        }
    }

    @Test
    void testGpuTicksWithoutLuidReturnsZero() {
        try (StubWindowsGpuStats stats = new StubWindowsGpuStats("", LHM_PARENT, PCI_BUS_NUMBER, PCI_BUS_ID,
                CARD_NAME)) {
            stats.withEngineTicks(1000L, 5000L);
            GpuTicks ticks = stats.getGpuTicks();
            assertThat(ticks.getActiveTicks(), is(0L));
            assertThat(ticks.getIdleTicks(), is(0L));
        }
    }

    @Test
    void testGpuTicksClampsIdleAtZero() {
        // A base counter lagging the active counter must not produce negative idle time
        try (StubWindowsGpuStats stats = StubWindowsGpuStats.nvidia().withEngineTicks(5000L, 1000L)) {
            assertThat(stats.getGpuTicks().getIdleTicks(), is(0L));
        }
    }

    // -------------------------------------------------------------------------
    // Memory
    // -------------------------------------------------------------------------

    @Test
    void testVramUsedPrefersPdhCounter() {
        try (StubWindowsGpuStats stats = StubWindowsGpuStats.nvidia().withAdapterMemory(4_294_967_296L, 512L)) {
            stats.withLhmSensor("SmallData", "GPU Memory Used", 1024);
            assertThat(stats.getVramUsed(), is(4_294_967_296L));
        }
    }

    @Test
    void testVramUsedFallsBackToLhmInMegabytes() {
        try (StubWindowsGpuStats stats = StubWindowsGpuStats.nvidia()) {
            stats.withLhmSensor("SmallData", "GPU Memory Used", 2048);
            assertThat(stats.getVramUsed(), is(2048L * 1_048_576L));
        }
    }

    @Test
    void testVramUsedReturnsSentinelWithNoSource() {
        try (StubWindowsGpuStats stats = StubWindowsGpuStats.nvidia()) {
            assertThat(stats.getVramUsed(), is(-1L));
        }
    }

    @Test
    void testSharedMemoryUsed() {
        try (StubWindowsGpuStats stats = StubWindowsGpuStats.nvidia().withAdapterMemory(4096L, 8192L)) {
            assertThat(stats.getSharedMemoryUsed(), is(8192L));
        }
    }

    @Test
    void testSharedMemoryUsedWithoutLuidReturnsSentinel() {
        try (StubWindowsGpuStats stats = new StubWindowsGpuStats("", LHM_PARENT, PCI_BUS_NUMBER, PCI_BUS_ID,
                CARD_NAME)) {
            assertThat(stats.getSharedMemoryUsed(), is(-1L));
        }
    }

    // -------------------------------------------------------------------------
    // Device resolution is cached
    // -------------------------------------------------------------------------

    @Test
    void testNvmlDeviceResolvedOnceAndCached() {
        try (StubWindowsGpuStats stats = StubWindowsGpuStats.nvidia().withNvml()) {
            stats.nvml.temperature(65).powerDraw(120);
            assertThat(stats.getTemperature(), closeTo(65, EPS));
            assertThat(stats.getPowerDraw(), closeTo(120, EPS));
            assertThat(stats.nvmlFindDeviceCalls, is(1));
        }
    }

    @Test
    void testNvmlFallsBackToNameLookup() {
        try (StubWindowsGpuStats stats = StubWindowsGpuStats.nvidia().withNvml()) {
            stats.nvmlDeviceByBusId = null;
            stats.nvml.temperature(65);
            assertThat(stats.getTemperature(), closeTo(65, EPS));
        }
    }

    @Test
    void testNvmlUnavailableIsNotRetried() {
        try (StubWindowsGpuStats stats = StubWindowsGpuStats.nvidia()) {
            assertThat(stats.getTemperature(), is(-1d));
            assertThat(stats.getPowerDraw(), is(-1d));
            assertThat(stats.nvmlFindDeviceCalls, is(0));
        }
    }

    @Test
    void testAdlAdapterIndexResolvedOnceAndCached() {
        try (StubWindowsGpuStats stats = StubWindowsGpuStats.nvidia().withAdl()) {
            stats.adl.temperature(70).powerDraw(200);
            assertThat(stats.getTemperature(), closeTo(70, EPS));
            assertThat(stats.getPowerDraw(), closeTo(200, EPS));
            assertThat(stats.adlFindAdapterIndexCalls, is(1));
        }
    }

    @Test
    void testAdlSkippedForNegativeBusNumber() {
        try (StubWindowsGpuStats stats = new StubWindowsGpuStats(LUID, LHM_PARENT, -1, PCI_BUS_ID, CARD_NAME)) {
            stats.withAdl();
            stats.adl.temperature(70);
            assertThat(stats.getTemperature(), is(-1d));
            assertThat(stats.adlFindAdapterIndexCalls, is(0));
        }
    }

    // -------------------------------------------------------------------------
    // Session lifecycle
    // -------------------------------------------------------------------------

    @Test
    void testClosedSessionThrows() {
        StubWindowsGpuStats stats = StubWindowsGpuStats.nvidia();
        assertThat(stats.isClosed(), is(false));
        stats.close();
        assertThat(stats.isClosed(), is(true));
        assertThrows(IllegalStateException.class, stats::getGpuUtilization);
        assertThrows(IllegalStateException.class, stats::getGpuTicks);
        assertThrows(IllegalStateException.class, stats::getVramUsed);
        assertThrows(IllegalStateException.class, stats::getSharedMemoryUsed);
        assertThrows(IllegalStateException.class, stats::getTemperature);
        assertThrows(IllegalStateException.class, stats::getPowerDraw);
        assertThrows(IllegalStateException.class, stats::getCoreClockMhz);
        assertThrows(IllegalStateException.class, stats::getMemoryClockMhz);
        assertThrows(IllegalStateException.class, stats::getFanSpeedPercent);
    }
}
