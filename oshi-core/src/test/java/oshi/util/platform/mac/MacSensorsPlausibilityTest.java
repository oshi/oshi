/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.mac;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notANumber;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import com.sun.jna.platform.mac.IOKit;

import oshi.SystemInfo;
import oshi.util.common.platform.mac.SmcKeyIndex;

/**
 * Tests the plausibility guard that keeps an idle-gated SMC sensor's sentinel from being reported as a temperature. JNA
 * twin of {@code oshi.ffm.MacSensorsPlausibilityFFMTest}.
 */
@EnabledOnOs(OS.MAC)
class MacSensorsPlausibilityTest {

    /**
     * Sentinels actually captured from hardware: -4.0, 0.0, 2.5, 4.0 and 5.2 appear in the iSMC sample reports, and
     * 4.633, 6.033, 6.7, 7.425 and 8.425 in a sweep of all 288 temperature sensors on an M2 Max. 8.425 is the highest
     * ever observed, so it is the value that pins the floor.
     */
    private static final double[] OBSERVED_SENTINELS = { -4.0, 0.0, 2.5, 4.0, 4.633, 5.2, 6.033, 6.7, 7.425, 8.425 };

    /**
     * Genuine readings actually observed: the lowest seen anywhere in the iSMC reports (21.0 on an Intel T2), a typical
     * Apple Silicon idle, and readings under load.
     */
    private static final double[] GENUINE_READINGS = { 21.0, 28.0, 35.0, 52.5, 85.0, 100.0 };

    @Test
    void testObservedSentinelsAreRejected() {
        for (double sentinel : OBSERVED_SENTINELS) {
            assertThat("Sentinel " + sentinel + " from an idle-gated sensor must be rejected",
                    SmcUtil.isPlausibleTemperature(sentinel), is(false));
        }
    }

    @Test
    void testGenuineReadingsAreAccepted() {
        for (double reading : GENUINE_READINGS) {
            assertThat("Genuine temperature " + reading + " must be accepted", SmcUtil.isPlausibleTemperature(reading),
                    is(true));
        }
    }

    /**
     * No upper bound is applied. Every bad value observed was at the low end, and a ceiling near the ~100 C throttling
     * point would discard real readings exactly when they matter most. This is a deliberate contract, not an observed
     * reading, so it is asserted separately from {@link #GENUINE_READINGS}.
     */
    @Test
    void testNoUpperBoundIsApplied() {
        assertThat("A reading above the throttling point must still be accepted", SmcUtil.isPlausibleTemperature(110d),
                is(true));
    }

    /**
     * The guard sits in an empty band: across M1 through M5, A18 and Intel T2 machines, nothing was observed between
     * the highest sentinel and the lowest genuine reading. Failing this means the floor has been moved onto one of the
     * two populations.
     */
    @Test
    void testFloorSitsBetweenTheTwoObservedPopulations() {
        // Strictly greater: the predicate accepts values equal to the floor, so a floor of exactly 8.425 would
        // accept the highest observed sentinel.
        assertThat("Floor must clear the highest observed sentinel", SmcUtil.MIN_PLAUSIBLE_TEMPERATURE,
                is(greaterThan(8.425)));
        assertThat("Floor must stay below the lowest observed genuine reading", SmcUtil.MIN_PLAUSIBLE_TEMPERATURE,
                is(lessThanOrEqualTo(21.0)));
    }

    /**
     * End to end against the real SMC: a reading is either unavailable or plausible, never a sentinel in between.
     */
    @Test
    void testCpuTemperatureIsPlausibleOrUnavailable() {
        double temp = new SystemInfo().getHardware().getSensors().getCpuTemperature();
        assertThat("CPU temperature must be unavailable or plausible, never a sentinel", temp,
                either(notANumber()).or(is(0d)).or(greaterThanOrEqualTo(SmcUtil.MIN_PLAUSIBLE_TEMPERATURE)));
    }

    /**
     * The chip-independent CPU-die aggregate keys are tried before the per-core keys, and {@code TCMb} (the die
     * average) is preferred over {@code TCMz} (the die max) so the reported value stays close to what OSHI historically
     * returned from a single per-core sensor.
     */
    @Test
    void testCpuTempAggregateKeysArePreferredAverageFirst() {
        assertThat("TCMb (average) must be tried before TCMz (max)", SmcUtil.SMC_KEYS_CPU_TEMP_AGGREGATE_AS,
                is(contains("TCMb", "TCMz")));
        assertThat("Aggregate keys must be distinct from the per-core keys",
                SmcUtil.SMC_KEYS_CPU_TEMP_AGGREGATE_AS.stream().noneMatch(SmcUtil.SMC_KEYS_CPU_TEMP_AS::contains),
                is(true));
    }

    /**
     * GPU keys are discovered from the SMC rather than hardcoded, because they are chip-specific: on this hardware only
     * two of the four keys OSHI originally shipped exist, and six sensors appear in no published key table.
     */
    @Test
    void testGpuTemperatureKeysAreDiscovered() {
        List<String> keys = SmcUtil.getGpuTemperatureKeys();
        assertThat("Discovery must never return null", keys, is(notNullValue()));
        for (String key : keys) {
            assertThat(key + " must match the GPU key convention", SmcKeyIndex.isGpuTemperatureKey(key), is(true));
        }
        List<String> second = SmcUtil.getGpuTemperatureKeys();
        assertThat("Repeated calls must agree", second, is(equalTo(keys)));
        // A completed discovery is cached; a failed one deliberately is not, and returns the shared fallback constant
        // instead. Asserting identity unconditionally would pass vacuously in that case, since both calls would return
        // the same constant without anything having been cached.
        if (!keys.equals(SmcUtil.SMC_KEYS_GPU_TEMP_AS)) {
            assertThat("A completed discovery must be cached, not repeated", second, is(sameInstance(keys)));
        }
    }

    /**
     * Regression guard against a discovery bug silently narrowing the sensor set: any of the legacy hardcoded keys that
     * this machine can actually read must also have been discovered. Passes on any Apple Silicon Mac.
     */
    @Test
    void testDiscoveryIsASupersetOfReadableLegacyKeys() {
        IOKit.IOConnect conn = SmcUtil.smcOpen();
        assumeTrue(conn != null, "SMC unavailable");
        try {
            List<String> discovered = SmcUtil.getGpuTemperatureKeys();
            for (String legacy : SmcUtil.SMC_KEYS_GPU_TEMP_AS) {
                if (SmcUtil.isPlausibleTemperature(SmcUtil.smcGetFloat(conn, legacy))) {
                    assertThat(legacy + " reads a real value but was not discovered", discovered, hasItem(legacy));
                }
            }
        } finally {
            SmcUtil.smcClose(conn);
        }
    }

    /**
     * The reported value is the hottest cluster, so it can never be below the first-match value the previous
     * implementation would have returned from the same key set.
     */
    @Test
    void testMaxIsNotLessThanFirstMatch() {
        IOKit.IOConnect conn = SmcUtil.smcOpen();
        assumeTrue(conn != null, "SMC unavailable");
        try {
            List<String> keys = SmcUtil.getGpuTemperatureKeys();
            double max = SmcUtil.smcGetMaxTemperature(conn, keys);
            double first = SmcUtil.smcGetFirstTemperature(conn, keys);
            assertThat("max must be at least the first plausible reading", max, is(greaterThanOrEqualTo(first)));
            assertThat("A GPU temperature is unavailable or plausible, never a sentinel in between",
                    max == 0d || SmcUtil.isPlausibleTemperature(max), is(true));
        } finally {
            SmcUtil.smcClose(conn);
        }
    }

    /**
     * The fallback list must remain a superset of the four keys OSHI read before discovery existed, so a machine that
     * falls back can never report less than the previous implementation did. Widening the list by frequency across
     * published sensor dumps once dropped {@code Tg0f}, which is the only one of the four present on some M2 hardware.
     */
    @Test
    void testFallbackKeysIncludeTheHistoricalFour() {
        assertThat(SmcUtil.SMC_KEYS_GPU_TEMP_AS, hasItems("Tg05", "Tg0D", "Tg0f", "Tg0j"));
    }

    // -- fans --

    @Test
    void testFanSpeedKeysAreDiscoveredAndWellFormed() {
        List<String> keys = SmcUtil.getFanSpeedKeys();
        assertThat("Discovery must never return null", keys, is(notNullValue()));
        for (String key : keys) {
            assertThat(key + " must match the fan speed key convention", SmcKeyIndex.isFanSpeedKey(key), is(true));
        }
    }

    @Test
    void testFanSpeedKeyDiscoveryIsCached() {
        List<String> keys = SmcUtil.getFanSpeedKeys();
        List<String> second = SmcUtil.getFanSpeedKeys();
        assertThat("Repeated calls must agree", second, is(equalTo(keys)));
        // A completed discovery is cached; the "cannot tell" outcome deliberately is not, and returns an empty list
        // without caching, so asserting identity on an empty result could pass vacuously.
        if (!keys.isEmpty()) {
            assertThat("A completed discovery must be cached, not repeated", second, is(sameInstance(keys)));
        }
    }

    @Test
    void testFanCountAgreesWithFanNum() {
        IOKit.IOConnect conn = SmcUtil.smcOpen();
        assumeTrue(conn != null, "SMC unavailable");
        try {
            long fanNum = SmcUtil.smcGetLong(conn, SmcUtil.SMC_KEY_FAN_NUM);
            assumeTrue(fanNum > 0, "FNum unavailable");
            assertThat("Discovered fan count must agree with FNum", (long) SmcUtil.getFanSpeedKeys().size(),
                    is(equalTo(fanNum)));
        } finally {
            SmcUtil.smcClose(conn);
        }
    }

    @Test
    void testFanSpeedsAreWithinTheHardwareLimits() {
        IOKit.IOConnect conn = SmcUtil.smcOpen();
        assumeTrue(conn != null, "SMC unavailable");
        try {
            int[] speeds = new SystemInfo().getHardware().getSensors().getFanSpeeds();
            List<String> keys = SmcUtil.getFanSpeedKeys();
            assertThat("One speed per discovered key", speeds.length, is(keys.size()));
            for (int i = 0; i < speeds.length; i++) {
                assertThat("Fan speed must not be negative", speeds[i], is(greaterThanOrEqualTo(0)));
                // F%dMx is the fan's hardware maximum. Derive it from the discovered key rather than the loop index:
                // discovery returns keys in index order, so a machine numbering its fans non-contiguously would
                // otherwise be compared against a different fan's maximum. Only assert where the key reads a value.
                double max = SmcUtil.smcGetFloat(conn, keys.get(i).substring(0, 2) + "Mx");
                if (max > 0) {
                    assertThat("Fan speed must not exceed the hardware maximum", (double) speeds[i],
                            is(lessThanOrEqualTo(max * 1.5)));
                }
            }
        } finally {
            SmcUtil.smcClose(conn);
        }
    }

    @Test
    void testStoppedFanIsReportedAsZeroNotDropped() {
        // An empty array means "no fans detected"; a zero entry means "a fan reading zero". A stopped fan must produce
        // the latter, so the array length tracks the key count regardless of whether any fan is spinning.
        int[] speeds = new SystemInfo().getHardware().getSensors().getFanSpeeds();
        assertThat("One speed per discovered key, even when all are stopped", speeds.length,
                is(SmcUtil.getFanSpeedKeys().size()));
    }

    // -- voltage --

    @Test
    void testCpuVoltageIsPlausibleOrUnavailable() {
        double volts = new SystemInfo().getHardware().getSensors().getCpuVoltage();
        assertThat("CPU voltage must be unavailable or plausible, never a mis-scaled fraction", volts,
                either(is(0d)).or(greaterThanOrEqualTo(SmcUtil.MIN_PLAUSIBLE_VOLTAGE)));
    }

    @Test
    void testAppleSiliconVoltageKeyReadsPlausibly() {
        IOKit.IOConnect conn = SmcUtil.smcOpen();
        assumeTrue(conn != null, "SMC unavailable");
        try {
            // VP0C is chip-specific, not simply Apple Silicon versus Intel: it reads 0.75 V on an M3 Pro but is
            // absent on an M2 Max, whose 53 V keys contain no identified core voltage at all. So skip when the key
            // is absent, without inferring which chip that implies.
            assumeTrue(!SmcUtil.smcGetDataType(conn, SmcUtil.SMC_KEY_CPU_VOLTAGE_AS).isEmpty(),
                    "VP0C absent on this chip");
            double volts = SmcUtil.smcGetFirstVoltage(conn, SmcUtil.getCpuVoltageKeys());
            assertThat("Apple Silicon core voltage must read plausibly", volts,
                    is(greaterThanOrEqualTo(SmcUtil.MIN_PLAUSIBLE_VOLTAGE)));
        } finally {
            SmcUtil.smcClose(conn);
        }
    }

    @Test
    void testVoltageKeysAreNotDiscovered() {
        // Guards against someone "completing the pattern" and adding a V-prefix scan that could return a supply rail
        // (e.g. VD0R at 20 V) as the CPU voltage. The default keys are the two named keys, no discovery.
        assertThat("Voltage keys must remain the named defaults", SmcUtil.getCpuVoltageKeys(),
                is(equalTo(SmcUtil.SMC_KEYS_CPU_VOLTAGE)));
        assertThat(SmcUtil.SMC_KEYS_CPU_VOLTAGE, contains(SmcUtil.SMC_KEY_CPU_VOLTAGE_AS, SmcUtil.SMC_KEY_CPU_VOLTAGE));
    }
}
