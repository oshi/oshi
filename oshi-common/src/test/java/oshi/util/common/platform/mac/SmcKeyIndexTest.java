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

import org.junit.jupiter.api.Test;

/**
 * Tests SMC key index logic without Mac hardware. This is why the logic lives here rather than in {@code SmcUtil},
 * whose static {@code IOKit.INSTANCE} field makes any of its members unloadable off a Mac.
 */
public class SmcKeyIndexTest {

    /** A realistic slice of a sorted SMC key index, with a Tg block in the middle. */
    private static final String[] SORTED = { "#KEY", "ALI0", "F0Ac", "TB0T", "TC0P", "TCMb", "TCMz", "Tf00", "Tf11",
            "Tg0W", "Tg0X", "Tg0e", "Tg0f", "Tg1g", "Tg1h", "Th00", "Tp01", "Tp09", "VP0C", "zSPc" };

    private static IntFunction<String> lookup(String[] keys) {
        return i -> i >= 0 && i < keys.length ? keys[i] : null;
    }

    private static List<String> findTg(String[] keys) {
        return SmcKeyIndex.findKeys(keys.length, lookup(keys), "Tg", SmcKeyIndex::isGpuTemperatureKey);
    }

    // -- binary search --

    @Test
    public void testFindsBlockInTheMiddle() {
        assertThat(findTg(SORTED), contains("Tg0W", "Tg0X", "Tg0e", "Tg0f", "Tg1g", "Tg1h"));
    }

    @Test
    public void testFindsBlockAtStart() {
        String[] keys = { "Tg0W", "Tg0X", "Th00", "Tp01" };
        assertThat(findTg(keys), contains("Tg0W", "Tg0X"));
    }

    @Test
    public void testFindsBlockAtEnd() {
        String[] keys = { "TB0T", "TCMb", "Tg0W", "Tg0X" };
        assertThat(findTg(keys), contains("Tg0W", "Tg0X"));
    }

    @Test
    public void testAbsentBlockYieldsEmptyNotNull() {
        // Empty means "this machine has no such keys" and is a cacheable answer; null means "could not read".
        String[] keys = { "TB0T", "TCMb", "Th00", "Tp01" };
        List<String> found = findTg(keys);
        assertThat("Absent block is a completed run", found, is(notNullValue()));
        assertThat(found, is(empty()));
    }

    @Test
    public void testEmptyAndSingleElementIndex() {
        assertThat("Zero count is not a readable index", findTg(new String[0]), is(nullValue()));
        assertThat(findTg(new String[] { "Tg0f" }), contains("Tg0f"));
        assertThat(findTg(new String[] { "TB0T" }), is(empty()));
    }

    @Test
    public void testImplausibleKeyCountIsRejected() {
        assertThat(SmcKeyIndex.findKeys(0, lookup(SORTED), "Tg", SmcKeyIndex::isGpuTemperatureKey), is(nullValue()));
        assertThat(SmcKeyIndex.findKeys(-1, lookup(SORTED), "Tg", SmcKeyIndex::isGpuTemperatureKey), is(nullValue()));
        assertThat("Guards against a garbage #KEY read",
                SmcKeyIndex.findKeys(Integer.MAX_VALUE, lookup(SORTED), "Tg", SmcKeyIndex::isGpuTemperatureKey),
                is(nullValue()));
    }

    @Test
    public void testKeyCountLargerThanIndexDegradesSafely() {
        // A count that overruns the readable range must degrade to "could not read", not throw and not report empty.
        assertThat(SmcKeyIndex.findKeys(SORTED.length + 50, lookup(SORTED), "Tg", SmcKeyIndex::isGpuTemperatureKey),
                is(nullValue()));
    }

    // -- read failures --

    @Test
    public void testAllReadsFailingYieldsNull() {
        assertThat("Nothing readable must not be cached as empty",
                SmcKeyIndex.findKeys(20, i -> null, "Tg", SmcKeyIndex::isGpuTemperatureKey), is(nullValue()));
    }

    @Test
    public void testTransientFailureIsRecovered() {
        // Fails the first read of one index, then succeeds: the retry must recover the full block.
        Set<Integer> failedOnce = new HashSet<>();
        IntFunction<String> flaky = i -> i == 12 && failedOnce.add(i) ? null : SORTED[i];
        assertThat(SmcKeyIndex.findKeys(SORTED.length, flaky, "Tg", SmcKeyIndex::isGpuTemperatureKey),
                contains("Tg0W", "Tg0X", "Tg0e", "Tg0f", "Tg1g", "Tg1h"));
    }

    @Test
    public void testPermanentFailureMidScanSkipsOnlyThatKey() {
        // Index 12 is Tg0f. Skipping rather than stopping preserves Tg1g/Tg1h, which a break would have discarded.
        IntFunction<String> flaky = i -> i == 12 ? null : SORTED[i];
        assertThat(SmcKeyIndex.findKeys(SORTED.length, flaky, "Tg", SmcKeyIndex::isGpuTemperatureKey),
                contains("Tg0W", "Tg0X", "Tg0e", "Tg1g", "Tg1h"));
    }

    @Test
    public void testFailedProbeDuringSearchIsRetriedAtNeighbour() {
        // A permanently unreadable index during the binary search descends via a neighbour instead of aborting.
        IntFunction<String> flaky = i -> i == SORTED.length / 2 ? null : SORTED[i];
        assertThat(SmcKeyIndex.findKeys(SORTED.length, flaky, "Tg", SmcKeyIndex::isGpuTemperatureKey),
                is(notNullValue()));
    }

    // -- robustness --

    @Test
    public void testUnsortedIndexTerminatesAndIsBounded() {
        String[] shuffled = SORTED.clone();
        Arrays.sort(shuffled, (a, b) -> b.compareTo(a)); // reverse order breaks the search's assumption
        List<String> found = SmcKeyIndex.findKeys(shuffled.length, lookup(shuffled), "Tg",
                SmcKeyIndex::isGpuTemperatureKey);
        assertThat("Must terminate rather than hang or overrun", found == null || found.size() <= shuffled.length,
                is(true));
    }

    @Test
    public void testDuplicateKeysAreDeduped() {
        String[] keys = { "TB0T", "Tg0f", "Tg0f", "Tg1h", "Th00" };
        assertThat(findTg(keys), contains("Tg0f", "Tg1h"));
    }

    // -- mask --

    @Test
    public void testMaskAcceptsRealGpuKeys() {
        // Every shape seen across M1-M5 and A18: uppercase, lowercase and digit fourth characters.
        for (String key : new String[] { "Tg05", "Tg0D", "Tg0f", "Tg0j", "Tg0W", "Tg1A", "Tg1k", "Tg99", "Tg0P" }) {
            assertThat(key + " must match", SmcKeyIndex.isGpuTemperatureKey(key), is(true));
        }
    }

    @Test
    public void testMaskRejectsNonGpuKeys() {
        // Tp/TG are CPU and Intel-era GPU keys; the rest are malformed.
        for (String key : new String[] { "Tp09", "TG05", "TCMb", "Tg0", "Tg005", "TgA5", "Tg0_", "tg05", "",
                " Tg0f" }) {
            assertThat("'" + key + "' must not match", SmcKeyIndex.isGpuTemperatureKey(key), is(false));
        }
        assertThat(SmcKeyIndex.isGpuTemperatureKey(null), is(false));
    }

    // -- configured keys --

    @Test
    public void testParseConfiguredKeys() {
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
        switch (key) {
            case "Tg0W":
                return 6.7d; // idle-gated sentinel
            case "Tg0X":
                return 40.7d;
            case "Tg0f":
                return 63.4d;
            case "Tg1h":
                return -4.0d; // negative sentinel, seen on M4 Max
            default:
                return 0d;
        }
    }

    @Test
    public void testMaxPlausibleIgnoresSentinels() {
        List<String> keys = Arrays.asList("Tg0W", "Tg0X", "Tg0f", "Tg1h");
        assertThat("Returns the hottest genuine reading, not the global max",
                SmcKeyIndex.maxPlausible(keys, SmcKeyIndexTest::read, v -> v >= FLOOR), is(63.4d));
    }

    @Test
    public void testMaxPlausibleWithNothingUsable() {
        assertThat("All-sentinel reads must report unavailable",
                SmcKeyIndex.maxPlausible(Arrays.asList("Tg0W", "Tg1h"), SmcKeyIndexTest::read, v -> v >= FLOOR),
                is(0d));
        assertThat(SmcKeyIndex.maxPlausible(Arrays.<String>asList(), SmcKeyIndexTest::read, v -> v >= FLOOR), is(0d));
    }

    @Test
    public void testMaxPlausibleNeverReturnsBelowTheFloor() {
        double result = SmcKeyIndex.maxPlausible(Arrays.asList("Tg0W", "Tg0X"), SmcKeyIndexTest::read, v -> v >= FLOOR);
        assertThat("A result is either 0 or at least the floor, never in between", result == 0d || result >= FLOOR,
                is(true));
        assertThat(6.7d, is(lessThanOrEqualTo(result)));
    }
}
