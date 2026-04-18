/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.registry;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.PdhCounterWildcardProperty;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;

/**
 * Base class for HKEY_PERFORMANCE_DATA utilities. Contains the common logic for building the counter name/index map and
 * resolving counter indices from enum definitions. Subclasses provide the platform-specific native call to read the
 * registry string array.
 */
@ThreadSafe
public abstract class HkeyPerformanceDataUtil {

    private static final Logger LOG = LoggerFactory.getLogger(HkeyPerformanceDataUtil.class);

    /**
     * Subclass-only constructor.
     */
    protected HkeyPerformanceDataUtil() {
    }

    /**
     * Registry key containing English counter name/index pairs.
     */
    public static final String HKEY_PERFORMANCE_TEXT = "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Perflib\\009";

    /**
     * Registry value name for the counter text.
     */
    public static final String COUNTER = "Counter";

    /**
     * Builds the counter index map from a string array of alternating index/name pairs.
     * <p>
     * The array format is pairs of {@code "<index>", "<name>"}. The first pair is a length/count entry (e.g.,
     * {@code "1", "1847"}) followed by actual counter definitions such as {@code "2", "System", "4", "Memory", "6",
     * "% Processor Time", ...}. Even positions (0, 2, 4, ...) hold numeric index strings and odd positions (1, 3, 5,
     * ...) hold the corresponding counter names.
     *
     * @param counterText The string array from the registry, or null if the read failed
     * @return An unmodifiable map of counter name to index
     */
    protected static Map<String, Integer> buildCounterIndexMap(String[] counterText) {
        HashMap<String, Integer> indexMap = new HashMap<>();
        if (counterText != null && counterText.length > 1) {
            for (int i = 1; i < counterText.length; i += 2) {
                int idx = ParseUtil.parseIntOrDefault(counterText[i - 1], 0);
                if (idx > 0) {
                    indexMap.putIfAbsent(counterText[i], idx);
                }
            }
        }
        return Collections.unmodifiableMap(indexMap);
    }

    /**
     * Looks up the counter index values for the given counter object and the enum of counter names.
     *
     * @param <T>             An enum containing the counters, whose class is passed as {@code counterEnum}
     * @param objectName      The counter object to look up the index for
     * @param counterEnum     The {@link Enum} containing counters to look up the indices for. The first Enum value will
     *                        be ignored.
     * @param counterIndexMap The map of counter names to their indices
     * @return A {@link Pair} containing the index of the counter object as the first element, and an {@link EnumMap}
     *         mapping counter enum values to their index as the second element, if the lookup is successful; null
     *         otherwise.
     */
    protected static <T extends Enum<T> & PdhCounterWildcardProperty> Pair<Integer, EnumMap<T, Integer>> getCounterIndices(
            String objectName, Class<T> counterEnum, Map<String, Integer> counterIndexMap) {
        Integer counterIndex = counterIndexMap.get(objectName);
        if (counterIndex == null) {
            LOG.debug("Couldn't find counter index of {}.", objectName);
            return null;
        }
        T[] enumConstants = counterEnum.getEnumConstants();
        EnumMap<T, Integer> indexMap = new EnumMap<>(counterEnum);
        // Start iterating at 1 because first Enum value defines the name/instance and
        // is not a counter name
        for (int i = 1; i < enumConstants.length; i++) {
            T key = enumConstants[i];
            Integer idx = counterIndexMap.get(key.getCounter());
            if (idx == null) {
                LOG.debug("Couldn't find counter index of {}.", key.getCounter());
                return null;
            }
            indexMap.put(key, idx);
        }
        // We have all the pieces! Return them.
        return new Pair<>(counterIndex, indexMap);
    }
}
