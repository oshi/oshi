/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.common.platform.mac;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * Tests SMC key index logic without Mac hardware. This is why the logic lives here rather than in {@code SmcUtil},
 * whose static {@code IOKit.INSTANCE} field makes any of its members unloadable off a Mac.
 */
class SmcKeyIndexTest {

    /** A realistic slice of a sorted SMC key index, with a Tg block in the middle. */
    private static final String[] SORTED = { "#KEY", "ALI0", "F0Ac", "TB0T", "TC0P", "TCMb", "TCMz", "Tf00", "Tf11",
            "Tg0W", "Tg0X", "Tg0e", "Tg0f", "Tg1g", "Tg1h", "Th00", "Tp01", "Tp09", "VP0C", "zSPc" };

    private static IntFunction<@Nullable String> lookup(String[] keys) {
        return i -> i >= 0 && i < keys.length ? keys[i] : null;
    }

    private static @Nullable List<String> findTg(String[] keys) {
        return SmcKeyIndex.findKeys(keys.length, lookup(keys), "Tg", SmcKeyIndex::isGpuTemperatureKey);
    }

    // -- binary search --

    @Test
    void testFindsBlockInTheMiddle() {
        assertThat(findTg(SORTED), contains("Tg0W", "Tg0X", "Tg0e", "Tg0f", "Tg1g", "Tg1h"));
    }

    @Test
    void testFindsBlockAtStart() {
        String[] keys = { "Tg0W", "Tg0X", "Th00", "Tp01" };
        assertThat(findTg(keys), contains("Tg0W", "Tg0X"));
    }

    @Test
    void testFindsBlockAtEnd() {
        String[] keys = { "TB0T", "TCMb", "Tg0W", "Tg0X" };
        assertThat(findTg(keys), contains("Tg0W", "Tg0X"));
    }

    @Test
    void testAbsentBlockYieldsEmptyNotNull() {
        // Empty means "this machine has no such keys" and is a cacheable answer; null means "could not read".
        String[] keys = { "TB0T", "TCMb", "Th00", "Tp01" };
        List<String> found = findTg(keys);
        assertThat("Absent block is a completed run", found, is(notNullValue()));
        assertThat(found, is(empty()));
    }

    @Test
    void testEmptyAndSingleElementIndex() {
        assertThat("Zero count is not a readable index", findTg(new String[0]), is(nullValue()));
        assertThat(findTg(new String[] { "Tg0f" }), contains("Tg0f"));
        assertThat(findTg(new String[] { "TB0T" }), is(empty()));
    }

    @Test
    void testImplausibleKeyCountIsRejected() {
        assertThat(SmcKeyIndex.findKeys(0, lookup(SORTED), "Tg", SmcKeyIndex::isGpuTemperatureKey), is(nullValue()));
        assertThat(SmcKeyIndex.findKeys(-1, lookup(SORTED), "Tg", SmcKeyIndex::isGpuTemperatureKey), is(nullValue()));
        assertThat("Guards against a garbage #KEY read",
                SmcKeyIndex.findKeys(Integer.MAX_VALUE, lookup(SORTED), "Tg", SmcKeyIndex::isGpuTemperatureKey),
                is(nullValue()));
    }

    @Test
    void testKeyCountLargerThanIndexDegradesSafely() {
        // A count that overruns the readable range must degrade to "could not read", not throw and not report empty.
        assertThat(SmcKeyIndex.findKeys(SORTED.length + 50, lookup(SORTED), "Tg", SmcKeyIndex::isGpuTemperatureKey),
                is(nullValue()));
    }

    // -- read failures --

    @Test
    void testAllReadsFailingYieldsNull() {
        assertThat("Nothing readable must not be cached as empty",
                SmcKeyIndex.findKeys(20, i -> null, "Tg", SmcKeyIndex::isGpuTemperatureKey), is(nullValue()));
    }

    @Test
    void testTransientFailureIsRecovered() {
        // Fails the first read of one index, then succeeds: the retry must recover the full block.
        Set<Integer> failedOnce = new HashSet<>();
        IntFunction<@Nullable String> flaky = i -> i == 12 && failedOnce.add(i) ? null : SORTED[i];
        assertThat(SmcKeyIndex.findKeys(SORTED.length, flaky, "Tg", SmcKeyIndex::isGpuTemperatureKey),
                contains("Tg0W", "Tg0X", "Tg0e", "Tg0f", "Tg1g", "Tg1h"));
    }

    @Test
    void testPermanentFailureMidScanSkipsOnlyThatKey() {
        // Index 12 is Tg0f. Skipping rather than stopping preserves Tg1g/Tg1h, which a break would have discarded.
        IntFunction<@Nullable String> flaky = i -> i == 12 ? null : SORTED[i];
        assertThat(SmcKeyIndex.findKeys(SORTED.length, flaky, "Tg", SmcKeyIndex::isGpuTemperatureKey),
                contains("Tg0W", "Tg0X", "Tg0e", "Tg1g", "Tg1h"));
    }

    @Test
    void testFailedSoleMatchIsNotReportedAsAbsent() {
        // The only Tg key is unreadable and the next key ends the block. Reporting empty would be cached as "this
        // machine has no GPU sensors" and would disable the sensor for the JVM lifetime.
        String[] keys = { "TB0T", "Tg0f", "Th00", "Tp01" };
        IntFunction<@Nullable String> flaky = i -> i == 1 ? null : keys[i];
        assertThat("An unreadable sole match must not be cached as a confirmed absence",
                SmcKeyIndex.findKeys(keys.length, flaky, "Tg", SmcKeyIndex::isGpuTemperatureKey), is(nullValue()));
    }

    @Test
    void testFailedSearchProbeThatSkipsTheBlockIsNotReportedAsAbsent() {
        // Same shape, but the failure is consumed by the binary search rather than the scan: substituting a neighbour
        // for the unreadable probe moves the landing point past Tg0f, so the scan never attempts it at all. Failing
        // only the first read is what makes null the proof of that -- had the scan reached index 2, its retry would
        // have recovered Tg0f and returned a non-empty list. A permanently failing index cannot tell the two apart.
        String[] keys = { "TB0T", "TCMb", "Tg0f", "Th00" };
        Set<Integer> failedOnce = new HashSet<>();
        IntFunction<@Nullable String> flaky = i -> i == 2 && failedOnce.add(i) ? null : keys[i];
        assertThat("A search that skipped the block on a failed read must not report a confirmed absence",
                SmcKeyIndex.findKeys(keys.length, flaky, "Tg", SmcKeyIndex::isGpuTemperatureKey), is(nullValue()));
    }

    @Test
    void testConfirmedAbsenceStillReportsEmpty() {
        // The counterpart guard: with every read succeeding, "no Tg keys" is a real answer and must stay cacheable,
        // so the fix above cannot degrade into "never return empty".
        String[] keys = { "TB0T", "TCMb", "Th00", "Tp01" };
        assertThat(findTg(keys), is(empty()));
    }

    @Test
    void testFailedProbeDuringSearchIsRetriedAtNeighbour() {
        // A permanently unreadable index during the binary search descends via a neighbour instead of aborting.
        IntFunction<@Nullable String> flaky = i -> i == SORTED.length / 2 ? null : SORTED[i];
        assertThat(SmcKeyIndex.findKeys(SORTED.length, flaky, "Tg", SmcKeyIndex::isGpuTemperatureKey),
                is(notNullValue()));
    }

    // -- robustness --

    @Test
    void testUnsortedIndexTerminatesAndIsBounded() {
        String[] shuffled = SORTED.clone();
        Arrays.sort(shuffled, (a, b) -> b.compareTo(a)); // reverse order breaks the search's assumption
        List<String> found = SmcKeyIndex.findKeys(shuffled.length, lookup(shuffled), "Tg",
                SmcKeyIndex::isGpuTemperatureKey);
        assertThat("Must terminate rather than hang or overrun", found == null || found.size() <= shuffled.length,
                is(true));
    }

    @Test
    void testDuplicateKeysAreDeduped() {
        String[] keys = { "TB0T", "Tg0f", "Tg0f", "Tg1h", "Th00" };
        assertThat(findTg(keys), contains("Tg0f", "Tg1h"));
    }

    // -- mask --

    @Test
    void testMaskAcceptsRealGpuKeys() {
        // Every shape seen across M1-M5 and A18: uppercase, lowercase and digit fourth characters.
        for (String key : new String[] { "Tg05", "Tg0D", "Tg0f", "Tg0j", "Tg0W", "Tg1A", "Tg1k", "Tg99", "Tg0P" }) {
            assertThat(key + " must match", SmcKeyIndex.isGpuTemperatureKey(key), is(true));
        }
    }

    @Test
    void testMaskRejectsNonGpuKeys() {
        // Tp/TG are CPU and Intel-era GPU keys; the rest are malformed.
        for (String key : new String[] { "Tp09", "TG05", "TCMb", "Tg0", "Tg005", "TgA5", "Tg0_", "tg05", "",
                " Tg0f" }) {
            assertThat("'" + key + "' must not match", SmcKeyIndex.isGpuTemperatureKey(key), is(false));
        }
        assertThat(SmcKeyIndex.isGpuTemperatureKey(null), is(false));
    }

    // -- fan keys --

    @Test
    void testFindsFanSpeedKeysExcludingOtherFanKeys() {
        // The SORTED fixture has a single fan key; discovery on it must return exactly that one.
        assertThat(SmcKeyIndex.findKeys(SORTED.length, lookup(SORTED), "F", SmcKeyIndex::isFanSpeedKey),
                contains("F0Ac"));
        // A realistic F block: the mask must reject FNum, F0Mn, F0Mx, FBAC and Ftst, which all sort inside it.
        String[] keys = { "F0Ac", "F0Mn", "F0Mx", "F1Ac", "FBAC", "FNum", "FOff", "Ftst" };
        assertThat(SmcKeyIndex.findKeys(keys.length, lookup(keys), "F", SmcKeyIndex::isFanSpeedKey),
                contains("F0Ac", "F1Ac"));
    }

    @Test
    void testFanSpeedMaskAcceptsRealFanKeys() {
        for (int i = 0; i <= 9; i++) {
            String key = "F" + i + "Ac";
            assertThat(key + " must match", SmcKeyIndex.isFanSpeedKey(key), is(true));
        }
    }

    @Test
    void testFanSpeedMaskRejectsNonFanKeys() {
        // FNum/F0Mn/F0Mx/F0Tg/FBAC/Ftst are other keys in the F block; the rest are malformed or wrong case.
        for (String key : new String[] { "FNum", "F0Mn", "F0Mx", "F0Tg", "FBAC", "Ftst", "f0ac", "F0AC", "F10Ac", "FAc",
                "" }) {
            assertThat("'" + key + "' must not match", SmcKeyIndex.isFanSpeedKey(key), is(false));
        }
        assertThat(SmcKeyIndex.isFanSpeedKey(null), is(false));
    }

    @Test
    void testFanSpeedKeysSynthesizedFromCount() {
        assertThat(SmcKeyIndex.fanSpeedKeys(0), is(empty()));
        assertThat(SmcKeyIndex.fanSpeedKeys(2), contains("F0Ac", "F1Ac"));
        assertThat("A negative count clamps to empty", SmcKeyIndex.fanSpeedKeys(-1), is(empty()));
    }

    @Test
    void testFanSpeedKeysAreAlwaysFourCharacters() {
        // Pins the latent defect: F%dAc with a two-digit index formats a five-character key that reads a different key.
        // Clamping to MAX_FANS keeps every synthesized key exactly four characters.
        for (long count : new long[] { 99L, Long.MAX_VALUE }) {
            List<String> keys = SmcKeyIndex.fanSpeedKeys(count);
            assertThat(keys.size(), is(SmcKeyIndex.MAX_FANS));
            for (String key : keys) {
                assertThat("'" + key + "' must be four characters", key.length(), is(4));
            }
        }
    }

    @Test
    void testReconcileFanKeysPrefersDiscovered() {
        List<String> discovered = Arrays.asList("F0Ac", "F1Ac");
        // Non-empty discovery is authoritative regardless of FNum, including when the two disagree.
        assertThat(SmcKeyIndex.reconcileFanKeys(discovered, 2), contains("F0Ac", "F1Ac"));
        assertThat(SmcKeyIndex.reconcileFanKeys(discovered, 5), contains("F0Ac", "F1Ac"));
        assertThat(SmcKeyIndex.reconcileFanKeys(discovered, 0), contains("F0Ac", "F1Ac"));
    }

    @Test
    void testReconcileFanKeysSynthesizesFromFnumWhenDiscoveryEmpty() {
        // No-regression: an index that yielded no fan keys still reports the fans FNum implies, as older OSHI did.
        assertThat(SmcKeyIndex.reconcileFanKeys(null, 2), contains("F0Ac", "F1Ac"));
        assertThat(SmcKeyIndex.reconcileFanKeys(Arrays.<String>asList(), 2), contains("F0Ac", "F1Ac"));
    }

    @Test
    void testReconcileFanKeysCannotTellReturnsNull() {
        // Discovery failed and FNum read zero: "no fans" is indistinguishable from "could not read", so defer.
        assertThat("Null must not be cached as a confirmed absence", SmcKeyIndex.reconcileFanKeys(null, 0),
                is(nullValue()));
    }

    @Test
    void testReconcileFanKeysConfirmedNoFansReturnsEmpty() {
        // Discovery completed with no fan keys and FNum agrees: a genuine fanless machine, a cacheable empty answer.
        List<String> result = SmcKeyIndex.reconcileFanKeys(Arrays.<String>asList(), 0);
        assertThat(result, is(notNullValue()));
        assertThat(result, is(empty()));
    }

    // -- configured keys --

    @Test
    void testParseConfiguredKeys() {
        assertThat(SmcKeyIndex.parseConfiguredKeys(null), is(empty()));
        assertThat(SmcKeyIndex.parseConfiguredKeys(""), is(empty()));
        assertThat(SmcKeyIndex.parseConfiguredKeys("   "), is(empty()));
        assertThat(SmcKeyIndex.parseConfiguredKeys(",,,"), is(empty()));
        assertThat(SmcKeyIndex.parseConfiguredKeys("Tg05,Tg0D"), contains("Tg05", "Tg0D"));
        assertThat(SmcKeyIndex.parseConfiguredKeys(" Tg05 , ,Tg0D "), contains("Tg05", "Tg0D"));
        assertThat("Wrong-length tokens are dropped, not passed to the SMC",
                SmcKeyIndex.parseConfiguredKeys("TOOLONG,Tg05,ab"), contains("Tg05"));
    }

    // -- aggregation --

    /** Sentinels and genuine readings captured from hardware; see MacSensorsPlausibilityTest. */
    private static final double FLOOR = 15d;

    private static double read(String key) {
        return switch (key) {
            case "Tg0W" -> 6.7d; // idle-gated sentinel
            case "Tg0X" -> 40.7d;
            case "Tg0f" -> 63.4d;
            case "Tg1h" -> -4.0d; // negative sentinel, seen on M4 Max
            default -> 0d;
        };
    }

    @Test
    void testMaxPlausibleIgnoresSentinels() {
        List<String> keys = Arrays.asList("Tg0W", "Tg0X", "Tg0f", "Tg1h");
        assertThat("Returns the hottest genuine reading, not the global max",
                SmcKeyIndex.maxPlausible(keys, SmcKeyIndexTest::read, v -> v >= FLOOR), is(63.4d));
    }

    @Test
    void testMaxPlausibleWithNothingUsable() {
        assertThat("All-sentinel reads must report unavailable",
                SmcKeyIndex.maxPlausible(Arrays.asList("Tg0W", "Tg1h"), SmcKeyIndexTest::read, v -> v >= FLOOR),
                is(0d));
        assertThat(SmcKeyIndex.maxPlausible(Arrays.<String>asList(), SmcKeyIndexTest::read, v -> v >= FLOOR), is(0d));
    }

    @Test
    void testMaxPlausibleNeverReturnsBelowTheFloor() {
        double result = SmcKeyIndex.maxPlausible(Arrays.asList("Tg0W", "Tg0X"), SmcKeyIndexTest::read, v -> v >= FLOOR);
        assertThat("A result is either 0 or at least the floor, never in between", result == 0d || result >= FLOOR,
                is(true));
        assertThat(6.7d, is(lessThanOrEqualTo(result)));
    }

    // -- first plausible --

    @Test
    void testFirstPlausibleReturnsTheFirstNotTheBest() {
        // Order is preference order, so an earlier plausible reading wins even though a later one is higher. This is
        // what distinguishes it from maxPlausible.
        List<String> keys = Arrays.asList("Tg0W", "Tg0X", "Tg0f");
        assertThat(SmcKeyIndex.firstPlausible(keys, SmcKeyIndexTest::read, v -> v >= FLOOR, "temperature"), is(40.7d));
    }

    @Test
    void testFirstPlausibleSkipsImplausibleAndSentinelReads() {
        assertThat("A leading sentinel must not stop the scan",
                SmcKeyIndex.firstPlausible(Arrays.asList("Tg1h", "Tg0W", "Tg0f"), SmcKeyIndexTest::read,
                        v -> v >= FLOOR, "temperature"),
                is(63.4d));
    }

    @Test
    void testFirstPlausibleWithNothingUsable() {
        assertThat(SmcKeyIndex.firstPlausible(Arrays.asList("Tg0W", "Tg1h"), SmcKeyIndexTest::read, v -> v >= FLOOR,
                "temperature"), is(0d));
        assertThat(SmcKeyIndex.firstPlausible(Arrays.<String>asList(), SmcKeyIndexTest::read, v -> v >= FLOOR,
                "temperature"), is(0d));
    }

    @Test
    void testFirstPlausibleAndMaxPlausibleAgreeOnASingleKey() {
        List<String> one = Arrays.asList("Tg0f");
        assertThat(SmcKeyIndex.firstPlausible(one, SmcKeyIndexTest::read, v -> v >= FLOOR, "temperature"),
                is(SmcKeyIndex.maxPlausible(one, SmcKeyIndexTest::read, v -> v >= FLOOR)));
    }
}
