/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import oshi.hardware.GpuTicks;

/**
 * Tests the shared macOS GpuStats logic without a Mac, by stubbing the two native hooks. This is why the logic lives
 * here rather than in the bindings, whose CoreFoundation and SMC calls cannot run in a unit test.
 */
class MacGpuStatsTest {

    private static final String CORE_UTIL = "GPU Core Utilization";
    private static final String DEVICE_UTIL = "Device Utilization %";
    private static final String VRAM_INTEL = "vramUsedBytes";
    private static final String VRAM_APPLE = "In use system memory";
    private static final String TEMPERATURE = "Temperature(C)";

    /** Records whether a sampler was requested, and hands out a fixed one. */
    private static final class RecordingFactory implements Supplier<IOReportSampler> {
        private final IOReportSampler sampler;
        private int calls;

        RecordingFactory(IOReportSampler sampler) {
            this.sampler = sampler;
        }

        @Override
        public IOReportSampler get() {
            calls++;
            return sampler;
        }
    }

    /** A sampler returning fixed values and recording that it was closed. */
    private static final class FakeSampler implements IOReportSampler {
        private final double utilization;
        private final double watts;
        private boolean closed;

        FakeSampler(double utilization, double watts) {
            this.utilization = utilization;
            this.watts = watts;
        }

        @Override
        public GpuTicks sampleGpuTicks() {
            return new GpuTicks(7L, 3L);
        }

        @Override
        public double sampleGpuUtilization() {
            return utilization;
        }

        @Override
        public double samplePowerWatts() {
            return watts;
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    /** Serves canned PerformanceStatistics, with null meaning the dictionary itself could not be read. */
    private static final class StubGpuStats extends MacGpuStats {
        private final Map<String, Long> stats;
        private final double smcTemp;
        private int perfStatQueries;

        StubGpuStats(boolean isAppleSilicon, String cardName, Supplier<IOReportSampler> samplerFactory,
                Map<String, Long> stats, double smcTemp) {
            super(isAppleSilicon, cardName, samplerFactory);
            this.stats = stats;
            this.smcTemp = smcTemp;
        }

        @Override
        protected Map<String, Long> queryPerfStats(String... keys) {
            perfStatQueries++;
            if (stats == null) {
                return null;
            }
            Map<String, Long> requested = new HashMap<>();
            for (String key : keys) {
                if (stats.containsKey(key)) {
                    requested.put(key, stats.get(key));
                }
            }
            return requested;
        }

        @Override
        protected double queryGpuTemperatureFromSmc() {
            return smcTemp;
        }
    }

    // The varargs are key/value pairs, so the loop is bounded on the value index rather than the key index: a caller
    // that passes a stray trailing key gets one fewer entry instead of running off the end.
    private static Map<String, Long> stats(Object... keyValues) {
        Map<String, Long> map = new HashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            map.put((String) keyValues[i], (Long) keyValues[i + 1]);
        }
        return map;
    }

    @Test
    void testIntelDoesNotSubscribeToIoReport() {
        // The IOReport GPU channels exist only on Apple Silicon, so no subscription is attempted on Intel.
        RecordingFactory factory = new RecordingFactory(new FakeSampler(50d, 12d));
        StubGpuStats intel = new StubGpuStats(false, "intel card", factory, stats(), -1d);
        assertThat("no sampler is created on Intel", factory.calls, is(0));
        // GpuTicks has no equals(), so compare the components.
        assertThat("active ticks fall back to zero without a sampler", intel.getGpuTicks().getActiveTicks(), is(0L));
        assertThat("idle ticks fall back to zero without a sampler", intel.getGpuTicks().getIdleTicks(), is(0L));
        assertThat("power is unavailable without a sampler", intel.getPowerDraw(), is(-1d));
    }

    @Test
    void testAppleSiliconSubscribesAndSamples() {
        RecordingFactory factory = new RecordingFactory(new FakeSampler(42d, 12.5d));
        StubGpuStats apple = new StubGpuStats(true, "apple card", factory, stats(), -1d);
        assertThat("a sampler is created on Apple Silicon", factory.calls, is(1));
        assertThat("active ticks come from the sampler", apple.getGpuTicks().getActiveTicks(), is(7L));
        assertThat("idle ticks come from the sampler", apple.getGpuTicks().getIdleTicks(), is(3L));
        assertThat("utilization comes from the sampler", apple.getGpuUtilization(), is(42d));
        assertThat("power comes from the sampler", apple.getPowerDraw(), is(12.5d));
    }

    @Test
    void testUtilizationFallsBackWhenTheSamplerReturnsNegative() {
        RecordingFactory factory = new RecordingFactory(new FakeSampler(-1d, 5d));
        StubGpuStats apple = new StubGpuStats(true, "fallback card", factory, stats(CORE_UTIL, 0xFFFFFFFFL / 2), -1d);
        assertThat("core utilization is scaled against the residency divisor", apple.getGpuUtilization(),
                is(closeTo(50d, 0.001d)));
    }

    @Test
    void testUtilizationPrefersCoreThenDeviceThenSentinel() {
        StubGpuStats withBoth = new StubGpuStats(false, "both card", MacGpuStatsTest::nullSampler,
                stats(CORE_UTIL, 0xFFFFFFFFL, DEVICE_UTIL, 17L), -1d);
        assertThat("core utilization wins", withBoth.getGpuUtilization(), is(closeTo(100d, 0.001d)));

        StubGpuStats deviceOnly = new StubGpuStats(false, "device card", MacGpuStatsTest::nullSampler,
                stats(DEVICE_UTIL, 17L), -1d);
        assertThat("device utilization is used unscaled", deviceOnly.getGpuUtilization(), is(17d));

        StubGpuStats neither = new StubGpuStats(false, "neither card", MacGpuStatsTest::nullSampler, stats(), -1d);
        assertThat("no key yields the sentinel", neither.getGpuUtilization(), is(-1d));

        StubGpuStats unreadable = new StubGpuStats(false, "unreadable card", MacGpuStatsTest::nullSampler, null, -1d);
        assertThat("an unreadable dictionary yields the sentinel", unreadable.getGpuUtilization(), is(-1d));
    }

    @Test
    void testVramKeySelectionDiffersByArchitecture() {
        // Apple Silicon reports GPU memory as a share of unified memory under a different key than discrete VRAM.
        Map<String, Long> both = stats(VRAM_INTEL, 100L, VRAM_APPLE, 200L);
        assertThat("Apple Silicon prefers the unified memory key",
                new StubGpuStats(true, "vram a", MacGpuStatsTest::nullSampler, both, -1d).getVramUsed(), is(200L));
        assertThat("Intel prefers the discrete VRAM key",
                new StubGpuStats(false, "vram b", MacGpuStatsTest::nullSampler, both, -1d).getVramUsed(), is(100L));

        // Each key is the other's fallback.
        assertThat("Apple Silicon falls back to the discrete key",
                new StubGpuStats(true, "vram c", MacGpuStatsTest::nullSampler, stats(VRAM_INTEL, 100L), -1d)
                        .getVramUsed(),
                is(100L));
        assertThat("Intel falls back to the unified key",
                new StubGpuStats(false, "vram d", MacGpuStatsTest::nullSampler, stats(VRAM_APPLE, 200L), -1d)
                        .getVramUsed(),
                is(200L));
        assertThat("neither key yields the sentinel",
                new StubGpuStats(false, "vram e", MacGpuStatsTest::nullSampler, stats(), -1d).getVramUsed(), is(-1L));
    }

    @Test
    void testTemperaturePrefersSmcOnAppleSilicon() {
        StubGpuStats apple = new StubGpuStats(true, "smc card", MacGpuStatsTest::nullSampler, stats(TEMPERATURE, 55L),
                61.5d);
        assertThat("a plausible SMC reading wins", apple.getTemperature(), is(61.5d));
        assertThat("the accelerator was not consulted", apple.perfStatQueries, is(0));

        StubGpuStats noSmc = new StubGpuStats(true, "no smc card", MacGpuStatsTest::nullSampler,
                stats(TEMPERATURE, 55L), 0d);
        assertThat("an unavailable SMC reading falls through", noSmc.getTemperature(), is(55d));
    }

    @Test
    void testTemperatureLatchesOnlyOnAMissingKey() {
        // A dictionary present but lacking the key means this card has no such sensor: latch it off.
        StubGpuStats missingKey = new StubGpuStats(false, "latch card", MacGpuStatsTest::nullSampler, stats(), -1d);
        assertThat("missing key yields the sentinel", missingKey.getTemperature(), is(-1d));
        assertThat("the first call queried", missingKey.perfStatQueries, is(1));
        assertThat("still the sentinel", missingKey.getTemperature(), is(-1d));
        assertThat("the lookup is not repeated once latched", missingKey.perfStatQueries, is(1));
    }

    @Test
    void testTemperatureDoesNotLatchOnAnUnreadableDictionary() {
        // A missing accelerator entry can be transient (driver reset, eGPU unplugged), so it must stay retryable.
        StubGpuStats unreadable = new StubGpuStats(false, "transient card", MacGpuStatsTest::nullSampler, null, -1d);
        assertThat("unreadable yields the sentinel", unreadable.getTemperature(), is(-1d));
        assertThat("unreadable still yields the sentinel", unreadable.getTemperature(), is(-1d));
        assertThat("the lookup is retried", unreadable.perfStatQueries, is(2));
    }

    @Test
    void testUnsupportedMetricsReturnSentinels() {
        StubGpuStats gpu = new StubGpuStats(true, "sentinel card", MacGpuStatsTest::nullSampler, stats(), -1d);
        assertThat("clock is unavailable on macOS", gpu.getCoreClockMhz(), is(-1L));
        assertThat("memory clock is unavailable on macOS", gpu.getMemoryClockMhz(), is(-1L));
        assertThat("fan speed is unavailable on macOS", gpu.getFanSpeedPercent(), is(-1d));
        assertThat("shared memory is unavailable on macOS", gpu.getSharedMemoryUsed(), is(-1L));
    }

    @Test
    void testCloseReleasesTheSamplerAndBlocksFurtherReads() {
        FakeSampler sampler = new FakeSampler(10d, 1d);
        StubGpuStats gpu = new StubGpuStats(true, "close card", new RecordingFactory(sampler), stats(), -1d);
        assertThat("open to begin with", gpu.isClosed(), is(false));
        gpu.close();
        assertThat("closed after close", gpu.isClosed(), is(true));
        assertThat("the sampler was released", sampler.closed, is(true));
        // Every metric guards on the closed flag.
        assertThrows(IllegalStateException.class, gpu::getGpuUtilization, "reading a closed session should throw");
    }

    @Test
    void testMatchesNameIgnoresCaseAndTrademarks() {
        StubGpuStats gpu = new StubGpuStats(false, "Radeon Pro 5500M", MacGpuStatsTest::nullSampler, stats(), -1d);
        assertThat("exact match", gpu.matchesName("radeon pro 5500m"), is(true));
        assertThat("case is ignored", gpu.matchesName("RADEON PRO 5500M"), is(true));
        assertThat("trademark symbols are stripped", gpu.matchesName("Radeon™ Pro 5500M"), is(true));
        assertThat("a containing name matches on a word boundary", gpu.matchesName("AMD Radeon Pro 5500M GPU"),
                is(true));
        assertThat("a different card does not match", gpu.matchesName("Radeon Pro 5300M"), is(false));
        assertThat("null does not match", gpu.matchesName(null), is(false));
        assertThat("empty does not match", gpu.matchesName(""), is(false));
    }

    private static IOReportSampler nullSampler() {
        return null;
    }
}
