/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.mac;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

/**
 * Tests {@link CpuFrequencyResidency} against the performance state residency and voltage state tables of a real M3 Pro
 * (Mac15,7, macOS 26.6.1), and against the channel names other Apple Silicon chips are reported to publish.
 */
class CpuFrequencyResidencyTest {

    /** The eight frequencies in Hz of the M3 Pro efficiency cluster, from {@code pmgr}'s voltage-states1-sram. */
    private static final long[] E_TABLE = { 744_000_000L, 1_044_000_000L, 1_476_000_000L, 2_004_000_000L,
            2_268_000_000L, 2_448_000_000L, 2_640_000_000L, 2_748_000_000L };

    /** The twenty frequencies in Hz of the M3 Pro performance cluster, from {@code pmgr}'s voltage-states5-sram. */
    private static final long[] P_TABLE = { 696_000_000L, 1_092_000_000L, 1_356_000_000L, 1_596_000_000L,
            1_884_000_000L, 2_172_000_000L, 2_424_000_000L, 2_616_000_000L, 2_808_000_000L, 2_988_000_000L,
            3_144_000_000L, 3_288_000_000L, 3_420_000_000L, 3_576_000_000L, 3_624_000_000L, 3_708_000_000L,
            3_780_000_000L, 3_864_000_000L, 3_960_000_000L, 4_056_000_000L };

    /** How much of a frequency a rounding difference may account for. */
    private static final long TOLERANCE_HZ = 1_000_000L;

    // Compares two frequencies in Hz, allowing for the rounding of the expected value computed by hand.
    private static void assertFrequency(String message, long expectedHz, long actualHz) {
        assertThat(message + ", " + actualHz + " Hz", Math.abs(actualHz - expectedHz),
                is(lessThanOrEqualTo(TOLERANCE_HZ)));
    }

    // Builds a residency map in channel state order, the first name being the idle state.
    private static Map<String, Long> residency(String[] names, long... ticks) {
        Map<String, Long> states = new LinkedHashMap<>();
        for (int i = 0; i < names.length; i++) {
            states.put(names[i], ticks[i]);
        }
        return states;
    }

    private static final String[] E_STATES = { "IDLE", "V0P7", "V1P6", "V2P5", "V3P4", "V4P3", "V5P2", "V6P1", "V7P0" };

    private static final String[] P_STATES = { "IDLE", "V0P19", "V1P18", "V2P17", "V3P16", "V4P15", "V5P14", "V6P13",
            "V7P12", "V8P11", "V9P10", "V10P9", "V11P8", "V12P7", "V13P6", "V14P5", "V15P4", "V16P3", "V17P2", "V18P1",
            "V19P0" };

    // ECPU0 of the real dump: mostly parked at the lowest frequency, with a little time at the highest
    private static Map<String, Long> ecpu0() {
        return residency(E_STATES, 1_567_158L, 890_293L, 8_869L, 34_395L, 95_064L, 0L, 0L, 0L, 15_183L);
    }

    // PCPU0 of the real dump: idle 97% of the interval, and running near 3 GHz for the rest
    private static Map<String, Long> pcpu0() {
        return residency(P_STATES, 2_553_514L, 656L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 40_607L, 10_414L, 6_130L, 0L, 0L,
                0L, 0L, 0L, 0L, 0L, 229L);
    }

    // PCPU3 of the real dump: never left idle during the interval
    private static Map<String, Long> pcpu3() {
        return residency(P_STATES, 2_611_550L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
                0L, 0L);
    }

    @Test
    void testWeightsOnlyTheTimeTheCoreWasRunning() {
        // Weighted over the 1043804 active ticks; weighting the 1567158 idle ticks at zero would report 364 MHz, and
        // the nominal maximum this replaces reports 2748 MHz
        assertFrequency("efficiency core", 914_573_414L,
                CpuFrequencyResidency.activeWeightedFrequency(ecpu0(), E_TABLE));
        assertFrequency("performance core", 3_025_986_835L,
                CpuFrequencyResidency.activeWeightedFrequency(pcpu0(), P_TABLE));
    }

    @Test
    void testAnIdleCoreReportsItsLowestFrequency() {
        assertThat("never ran", CpuFrequencyResidency.activeWeightedFrequency(pcpu3(), P_TABLE), is(696_000_000L));
    }

    @Test
    void testAnIntervalWithNoTicksAtAllIsNotAFrequency() {
        // Two samples taken close enough together that no tick accumulated, which is not the same as an idle core: it
        // says nothing about the frequency, so the caller keeps the nominal one
        Map<String, Long> nothing = residency(P_STATES, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
                0L, 0L, 0L, 0L, 0L);
        assertThat("nothing observed", CpuFrequencyResidency.activeWeightedFrequency(nothing, P_TABLE), is(0L));
        assertThat("idle observed", CpuFrequencyResidency.activeWeightedFrequency(pcpu3(), P_TABLE), is(P_TABLE[0]));
    }

    @Test
    void testNoFrequencyFallsOutsideItsTable() {
        long efficiency = CpuFrequencyResidency.activeWeightedFrequency(ecpu0(), E_TABLE);
        assertThat("efficiency core at least the minimum", efficiency >= E_TABLE[0], is(true));
        assertThat("efficiency core at most the maximum", efficiency <= E_TABLE[E_TABLE.length - 1], is(true));
        long performance = CpuFrequencyResidency.activeWeightedFrequency(pcpu0(), P_TABLE);
        assertThat("performance core at least the minimum", performance >= P_TABLE[0], is(true));
        assertThat("performance core at most the maximum", performance <= P_TABLE[P_TABLE.length - 1], is(true));
    }

    @Test
    void testAPowerGatedStateAheadOfIdleIsAlsoExcluded() {
        // Some chips report DOWN or OFF for a power-gated core, ahead of IDLE, which adds a state the table cannot
        // explain. The frequencies still pair correctly from the end of the list.
        String[] names = new String[E_STATES.length + 1];
        names[0] = "DOWN";
        System.arraycopy(E_STATES, 0, names, 1, E_STATES.length);
        Map<String, Long> gated = residency(names, 42L, 1_567_158L, 890_293L, 8_869L, 34_395L, 95_064L, 0L, 0L, 0L,
                15_183L);
        assertThat("DOWN ahead of IDLE", CpuFrequencyResidency.activeWeightedFrequency(gated, E_TABLE),
                is(CpuFrequencyResidency.activeWeightedFrequency(ecpu0(), E_TABLE)));
    }

    @Test
    void testAMissingFrequencyStateIsNotPairedByShiftingTheTable() {
        // One frequency state short of the table. Pairing what is there against the end of the table would charge the
        // idle ticks as the lowest frequency and shift every other frequency onto its neighbour's ticks, reporting a
        // number that looks plausible and is wrong throughout.
        String[] names = Arrays.copyOf(E_STATES, E_STATES.length - 1);
        Map<String, Long> incomplete = residency(names, 1_567_158L, 890_293L, 8_869L, 34_395L, 95_064L, 0L, 0L, 0L);
        assertThat("one active state missing", CpuFrequencyResidency.activeWeightedFrequency(incomplete, E_TABLE),
                is(0L));
    }

    @Test
    void testAnUnrecognizedLeadingStateIsStillExcluded() {
        // A power-gated state named something this release does not know cannot be recognized as an idle state, so the
        // surplus is resolved from the end of the list instead. Doing so only drops leading states, never a frequency.
        String[] names = new String[E_STATES.length + 1];
        names[0] = "POWERGATED";
        System.arraycopy(E_STATES, 0, names, 1, E_STATES.length);
        Map<String, Long> gated = residency(names, 42L, 1_567_158L, 890_293L, 8_869L, 34_395L, 95_064L, 0L, 0L, 0L,
                15_183L);
        assertThat("unrecognized state ahead of IDLE", CpuFrequencyResidency.activeWeightedFrequency(gated, E_TABLE),
                is(CpuFrequencyResidency.activeWeightedFrequency(ecpu0(), E_TABLE)));
    }

    @Test
    void testResidencyTooShortForTheTableIsNotGuessedAt() {
        assertThat("fewer states than frequencies", CpuFrequencyResidency.activeWeightedFrequency(ecpu0(), P_TABLE),
                is(0L));
        assertThat("no states at all",
                CpuFrequencyResidency.activeWeightedFrequency(Collections.<String, Long>emptyMap(), E_TABLE), is(0L));
        assertThat("no table", CpuFrequencyResidency.activeWeightedFrequency(ecpu0(), new long[0]), is(0L));
    }

    @Test
    void testEveryStateIdleIsNotMistakenForActivity() {
        // A core reporting only idle states has no frequency to weight, and the table cannot explain one state
        assertThat(CpuFrequencyResidency.activeWeightedFrequency(residency(new String[] { "IDLE" }, 100L), E_TABLE),
                is(0L));
    }

    // Residency of a CPU complex that spent every active tick at one frequency of its table. A complex reports the same
    // states a core of it does, its idle one being the time every one of its cores was idle.
    private static Map<String, Long> complexAt(String[] states, int index) {
        long[] ticks = new long[states.length];
        ticks[0] = 500L;
        ticks[index + 1] = 1_000L;
        return residency(states, ticks);
    }

    private static final int RANK_E = CpuFrequencyResidency.prefixRank("ECPU0");
    private static final int RANK_P = CpuFrequencyResidency.prefixRank("PCPU0");

    @Test
    void testTheClusterStateTheHardwareRanAtIsPreferredToTheOneTheCoresAskedFor() {
        // Both channels of a complex are published, and only the one reporting what the cores got is weighted: on a
        // busy M3 Pro the cores ask for the top state throughout while the hardware runs several states below it
        Map<String, Map<String, Long>> complexes = new LinkedHashMap<>();
        complexes.put("ECPU", complexAt(E_STATES, 7));
        complexes.put("ECPM", complexAt(E_STATES, 3));
        complexes.put("PCPU", complexAt(P_STATES, 19));
        complexes.put("PCPM", complexAt(P_STATES, 13));
        Map<Integer, Map<String, Long>> byRank = CpuFrequencyResidency.realizedComplexStates(complexes);
        assertThat("one entry per core type", byRank.keySet(), is(new TreeSet<>(List.of(RANK_E, RANK_P))));
        Map<String, Long> efficiency = byRank.get(RANK_E);
        Map<String, Long> performance = byRank.get(RANK_P);
        assertNotNull(efficiency, "efficiency complex");
        assertNotNull(performance, "performance complex");
        assertThat("efficiency cluster", CpuFrequencyResidency.activeWeightedFrequency(efficiency, E_TABLE),
                is(E_TABLE[3]));
        assertThat("performance cluster", CpuFrequencyResidency.activeWeightedFrequency(performance, P_TABLE),
                is(P_TABLE[13]));
    }

    @Test
    void testEveryClusterOfOneCoreTypeIsSummed() {
        // An Ultra prefixes each die's complex, and a chip with two clusters of one type on a die is expected to number
        // them. Every cluster of a type runs the same frequencies, so one type ran at the average of its clusters.
        for (String[] names : List.of(new String[] { "DIE_0_PCPM", "DIE_1_PCPM" }, new String[] { "PCPM0", "PCPM1" })) {
            Map<String, Map<String, Long>> complexes = new LinkedHashMap<>();
            complexes.put(names[0], complexAt(P_STATES, 5));
            complexes.put(names[1], complexAt(P_STATES, 15));
            Map<String, Long> summed = CpuFrequencyResidency.realizedComplexStates(complexes).get(RANK_P);
            assertNotNull(summed, names[0] + " and " + names[1] + " are one core type");
            assertFrequency(names[0] + " with " + names[1], (P_TABLE[5] + P_TABLE[15]) / 2,
                    CpuFrequencyResidency.activeWeightedFrequency(summed, P_TABLE));
        }
    }

    @Test
    void testClustersOfOneTypeThatNameTheirStatesDifferentlyAreNotSummed() {
        // Summing them would add one cluster's ticks at a frequency to another's at a different frequency, so the type
        // is reported as one the caller cannot weight rather than as a number that is wrong throughout
        Map<String, Map<String, Long>> complexes = new LinkedHashMap<>();
        complexes.put("PCPM0", complexAt(P_STATES, 5));
        complexes.put("PCPM1", complexAt(E_STATES, 3));
        Map<String, Long> summed = CpuFrequencyResidency.realizedComplexStates(complexes).get(RANK_P);
        assertNotNull(summed, "the core type is still reported");
        assertThat("not summable", CpuFrequencyResidency.activeWeightedFrequency(summed, P_TABLE), is(0L));
    }

    @Test
    void testAClusterOfACoreTypeThisReleaseDoesNotKnowIsKeyedAsUnknown() {
        Map<String, Map<String, Long>> complexes = new LinkedHashMap<>();
        complexes.put("XCPM", complexAt(P_STATES, 5));
        // Not a complex channel at all, so it names no core type and is left out rather than keyed as unknown
        complexes.put("GPU", complexAt(P_STATES, 5));
        assertThat(CpuFrequencyResidency.realizedComplexStates(complexes).keySet(),
                is(Collections.singleton(CpuFrequencyResidency.UNKNOWN_RANK)));
    }

    @Test
    void testChannelsAreOrderedByCoreTypeThenCore() {
        assertThat("M3 Pro", CpuFrequencyResidency.orderChannels(List.of("PCPU5", "ECPU0", "PCPU0", "ECPU5", "ECPU1")),
                is(List.of("ECPU0", "ECPU1", "ECPU5", "PCPU0", "PCPU5")));
        // The M5 Pro and Max name their performance cores MCPU and their fastest cores PCPU, with no ECPU at all, so
        // the letters shift but the order does not
        assertThat("M5 Max", CpuFrequencyResidency.orderChannels(List.of("PCPU1", "MCPU11", "PCPU0", "MCPU0", "MCPU2")),
                is(List.of("MCPU0", "MCPU2", "MCPU11", "PCPU0", "PCPU1")));
        // An Ultra prefixes its channels with the die, and macOS numbers both dies' cores of one type before the next
        assertThat("Ultra",
                CpuFrequencyResidency
                        .orderChannels(List.of("DIE_1_PCPU0", "DIE_0_ECPU0", "DIE_1_ECPU0", "DIE_0_PCPU0")),
                is(List.of("DIE_0_ECPU0", "DIE_1_ECPU0", "DIE_0_PCPU0", "DIE_1_PCPU0")));
    }

    @Test
    void testACoreIndexIsOrderedNumericallyNotAsText() {
        assertThat(CpuFrequencyResidency.orderChannels(List.of("PCPU10", "PCPU9", "PCPU2")),
                is(List.of("PCPU2", "PCPU9", "PCPU10")));
    }

    @Test
    void testANameThatIsNotACoreChannelSortsLast() {
        assertThat(CpuFrequencyResidency.orderChannels(List.of("ECPM", "PCPU0", "ECPU0", "GPU")),
                is(List.of("ECPU0", "PCPU0", "ECPM", "GPU")));
        // Both a core type no release knows and a name that is not a core channel at all, so a caller can tell that a
        // sample holds something it cannot place
        assertThat("unrecognized core type", CpuFrequencyResidency.prefixRank("XCPU0"),
                is(CpuFrequencyResidency.UNKNOWN_RANK));
        assertThat("not a core channel", CpuFrequencyResidency.prefixRank("ECPM"),
                is(CpuFrequencyResidency.UNKNOWN_RANK));
    }

    @Test
    void testCoreTypesRankInAscendingPerformanceOrder() {
        assertThat("E before P", CpuFrequencyResidency.prefixRank("ECPU0") < CpuFrequencyResidency.prefixRank("PCPU0"),
                is(true));
        assertThat("M before P", CpuFrequencyResidency.prefixRank("MCPU0") < CpuFrequencyResidency.prefixRank("PCPU0"),
                is(true));
        assertThat("P before S", CpuFrequencyResidency.prefixRank("PCPU0") < CpuFrequencyResidency.prefixRank("SCPU0"),
                is(true));
        assertThat("one type per rank regardless of die",
                CpuFrequencyResidency.prefixRank("DIE_1_ECPU0") == CpuFrequencyResidency.prefixRank("ECPU0"), is(true));
    }

    @Test
    void testItemsAlignAtTheTopOfTheClasses() {
        assertArrayEqualsInt(new int[] { 0, 1 }, CpuFrequencyResidency.alignAtTop(2, 2), "one item per class");
        assertArrayEqualsInt(new int[] { 0, 0 }, CpuFrequencyResidency.alignAtTop(1, 2), "one item, two classes");
        assertArrayEqualsInt(new int[] { 1 }, CpuFrequencyResidency.alignAtTop(2, 1), "two items, one class");
        assertArrayEqualsInt(new int[] { 0, 1, 2 }, CpuFrequencyResidency.alignAtTop(3, 3), "three of each");
        assertArrayEqualsInt(new int[] { 0 }, CpuFrequencyResidency.alignAtTop(1, 0), "no classes reported");
        assertArrayEqualsInt(new int[0], CpuFrequencyResidency.alignAtTop(0, 2), "nothing read");
    }

    private static void assertArrayEqualsInt(int[] expected, int[] actual, String message) {
        assertThat(message, Arrays.toString(actual), is(Arrays.toString(expected)));
    }
}
