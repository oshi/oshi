/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.mac;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.ParseUtil;

/**
 * Derives Apple Silicon CPU frequencies from the performance state residency the IOReport {@code CPU Stats} group
 * publishes, and orders that group's channels the way macOS numbers cores.
 * <p>
 * A core's channel reports how many ticks it spent in each of its performance states since the previous sample. The
 * states are its idle state or states followed by one state per frequency in the cluster's power manager
 * {@code voltage-states} table, in the same ascending order, so pairing the two lists gives the frequency the core
 * actually ran at.
 */
@ThreadSafe
public final class CpuFrequencyResidency {

    private CpuFrequencyResidency() {
    }

    /**
     * Names of the states a core occupies while it is not executing. {@code IDLE} is always present; {@code DOWN} and
     * {@code OFF} appear on some chips for a power-gated core, ahead of it.
     */
    private static final Set<String> IDLE_STATES = new HashSet<>(Arrays.asList("IDLE", "DOWN", "OFF"));

    /**
     * Matches an IOReport CPU core channel name, e.g. {@code ECPU0}, {@code PCPU11}, or {@code DIE_1_PCPU0} on a chip
     * with more than one die. The letter before {@code CPU} identifies the core type and the trailing number the core
     * within the die.
     */
    private static final Pattern CPU_CHANNEL = Pattern.compile("^(?:DIE_(\\d+)_)?([A-Z])CPU(\\d+)$");

    /**
     * The core type letters in ascending performance order. Through the M4 the letters are {@code E} and {@code P}; on
     * the M5 Pro and Max the performance cores report {@code M} and the fastest cores {@code P}, which changes the
     * letters but not their order. A letter absent from this list sorts after all of them.
     */
    private static final String CHANNEL_PREFIX_ORDER = "EMPS";

    /**
     * Rank {@link #prefixRank(String)} gives to a name that is not a core channel, or that names a core type this
     * release does not know. It orders after every known type, and a caller pairing channels with cores should treat
     * its presence as a chip it cannot interpret rather than assume where such a core belongs.
     */
    public static final int UNKNOWN_RANK = CHANNEL_PREFIX_ORDER.length();

    /** Bits reserved for the core index in a channel sort key, enough for any core count a die could report. */
    private static final int CORE_BITS = 20;

    /**
     * Computes the frequency a core ran at while it was running, as the average of its cluster's frequencies weighted
     * by the time the core spent at each of them.
     * <p>
     * Idle residency is excluded rather than weighted at zero, so the result is the frequency the core selected when it
     * had work, which is what Apple's own tooling reports. A core that did not run at all reports the lowest frequency
     * its cluster can run at, which is both the state such a core is parked in and a value that cannot be mistaken for
     * a failure.
     *
     * @param stateResidency the ticks the core spent in each state, in channel state order
     * @param table          the frequencies in Hz the core's cluster can run at, in ascending order
     * @return the frequency in Hz, or 0 if the two lists cannot be paired or the core was not observed at all
     */
    public static long activeWeightedFrequency(Map<String, Long> stateResidency, long[] table) {
        if (table.length == 0) {
            return 0L;
        }
        List<Long> residency = new ArrayList<>(stateResidency.size());
        long observedTicks = 0L;
        int firstActive = -1;
        for (Map.Entry<String, Long> state : stateResidency.entrySet()) {
            if (firstActive < 0 && !IDLE_STATES.contains(state.getKey().toUpperCase(Locale.ROOT))) {
                firstActive = residency.size();
            }
            Long ticks = state.getValue();
            // A delta cannot be negative unless a counter was reset between samples, in which case the tick count says
            // nothing about how long the core ran
            long clamped = ticks == null ? 0L : Math.max(ticks, 0L);
            observedTicks += clamped;
            residency.add(clamped);
        }
        if (firstActive < 0 || residency.size() - firstActive != table.length) {
            // Either every state is an idle state, or the chip reports a number of states this table cannot explain.
            // Aligning the lists at the end still pairs the frequencies correctly if the difference is an extra idle
            // state rather than an extra frequency.
            firstActive = residency.size() - table.length;
            if (firstActive < 0) {
                return 0L;
            }
        }
        long totalTicks = 0L;
        // Accumulated as a double because ticks times hertz overflows a long once a few states are summed. The result
        // is a frequency, far inside the range a double represents exactly.
        double weighted = 0d;
        for (int i = 0; i < table.length; i++) {
            long ticks = residency.get(firstActive + i);
            totalTicks += ticks;
            weighted += (double) ticks * table[i];
        }
        if (totalTicks == 0L) {
            // An interval so short that not even an idle tick accumulated says nothing about the core's frequency,
            // where a core that only accumulated idle ticks really did sit at the lowest frequency its cluster runs at
            return observedTicks == 0L ? 0L : table[0];
        }
        return (long) (weighted / totalTicks);
    }

    /**
     * Orders CPU core channel names the way macOS numbers logical processors: the least performant core type first, and
     * within one type each die in turn.
     *
     * @param channelNames the channel names to order
     * @return the names in core order, with any name that is not a core channel last
     */
    public static List<String> orderChannels(Collection<String> channelNames) {
        List<String> ordered = new ArrayList<>(channelNames);
        Collections.sort(ordered, (a, b) -> {
            int bySortKey = Long.compare(sortKey(a), sortKey(b));
            return bySortKey == 0 ? a.compareTo(b) : bySortKey;
        });
        return ordered;
    }

    /**
     * Gets the core type of a channel, as its rank in ascending performance order. Channels sharing a rank are the
     * cores of one type, however many clusters they are divided into.
     *
     * @param channelName the channel name
     * @return the rank, or a value ordering after every known core type if the name is not a core channel or names an
     *         unrecognized core type
     */
    public static int prefixRank(String channelName) {
        Matcher m = CPU_CHANNEL.matcher(channelName);
        if (m.matches()) {
            int rank = CHANNEL_PREFIX_ORDER.indexOf(m.group(2));
            if (rank >= 0) {
                return rank;
            }
        }
        return UNKNOWN_RANK;
    }

    /**
     * Builds a single ordering value for a channel name, ranking the core type ahead of the die so that a chip with two
     * dies interleaves them the way macOS numbers its cores.
     *
     * @param channelName the channel name
     * @return the sort key
     */
    private static long sortKey(String channelName) {
        Matcher m = CPU_CHANNEL.matcher(channelName);
        if (!m.matches()) {
            return Long.MAX_VALUE;
        }
        int rank = CHANNEL_PREFIX_ORDER.indexOf(m.group(2));
        long die = ParseUtil.parseIntOrDefault(m.group(1), 0);
        long core = ParseUtil.parseIntOrDefault(m.group(3), 0);
        return ((long) (rank < 0 ? UNKNOWN_RANK : rank) << (2 * CORE_BITS)) | (die << CORE_BITS) | core;
    }

    /**
     * Distributes a list of items over the efficiency classes, aligned at the top so that the highest class always gets
     * the last item and a class with no item of its own borrows the nearest one.
     *
     * @param itemCount  the number of items available, which may be zero
     * @param classCount the number of efficiency classes, at least 1
     * @return the index into the items for each class, or an empty array if there are no items
     */
    public static int[] alignAtTop(int itemCount, int classCount) {
        if (itemCount == 0) {
            return new int[0];
        }
        int[] indices = new int[Math.max(classCount, 1)];
        for (int i = 0; i < indices.length; i++) {
            int index = itemCount - indices.length + i;
            indices[i] = Math.min(Math.max(index, 0), itemCount - 1);
        }
        return indices;
    }
}
