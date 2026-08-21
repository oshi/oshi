/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import oshi.annotation.concurrent.Immutable;

/**
 * One interval of Apple Silicon CPU performance state residency, holding the two views IOReport publishes of the same
 * interval.
 * <p>
 * A core and the cluster it belongs to report their states separately, and the two do not always agree: a core reports
 * the state it asked for, which is the fastest one whenever it has work, while its cluster reports the state the
 * hardware actually ran at, which is lower whenever a power or thermal limit caps it. Measured on an M3 Pro, six busy
 * cores each report the top state throughout while their cluster reports 3576 MHz, the figure {@code powermetrics}
 * gives for the same interval. Both views are therefore needed: the cluster's for how fast a core ran, the core's own
 * for whether it ran at all.
 */
@Immutable
public final class CpuResidencySample {

    private final Map<String, Map<String, Long>> coreStates;
    private final Map<String, Map<String, Long>> complexStates;

    /**
     * Creates a new sample. Both maps and the maps they contain must preserve the order in which the channel reports
     * its states, so both are copied into insertion-ordered maps.
     *
     * @param coreStates    the ticks each core spent in each of its states, keyed by the channel naming the core
     * @param complexStates the ticks each CPU complex spent in each of its states, keyed by the channel naming the
     *                      complex
     */
    public CpuResidencySample(Map<String, Map<String, Long>> coreStates, Map<String, Map<String, Long>> complexStates) {
        this.coreStates = copy(coreStates);
        this.complexStates = copy(complexStates);
    }

    private static Map<String, Map<String, Long>> copy(Map<String, Map<String, Long>> states) {
        Map<String, Map<String, Long>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Long>> channel : states.entrySet()) {
            copy.put(channel.getKey(), Collections.unmodifiableMap(new LinkedHashMap<>(channel.getValue())));
        }
        return Collections.unmodifiableMap(copy);
    }

    /**
     * Gets the residency of the individual cores, from the {@code CPU Core Performance States} channels.
     * <p>
     * The states of one core are its idle state or states followed by one state per frequency the core's cluster can
     * run at, in ascending frequency order.
     *
     * @return a map from the channel name identifying the core to a map from state name to ticks, in channel state
     *         order. Empty if the sample held no such channel.
     */
    public Map<String, Map<String, Long>> getCoreStates() {
        return coreStates;
    }

    /**
     * Gets the residency of the CPU complexes, from the {@code CPU Complex Performance States} channels.
     * <p>
     * The states are ordered as the per-core ones are, and the channels naming a complex whose residency is the state
     * the hardware ran at can be told from the rest by their name; see
     * {@link oshi.driver.common.mac.CpuFrequencyResidency#realizedComplexStates}.
     *
     * @return a map from the channel name identifying the complex to a map from state name to ticks, in channel state
     *         order. Empty if the sample held no such channel.
     */
    public Map<String, Map<String, Long>> getComplexStates() {
        return complexStates;
    }

    @Override
    public String toString() {
        return "CpuResidencySample{cores=" + coreStates.keySet() + ", complexes=" + complexStates.keySet() + '}';
    }
}
