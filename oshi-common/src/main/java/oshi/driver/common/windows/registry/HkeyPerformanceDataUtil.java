/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.PdhCounterWildcardProperty;
import oshi.util.GlobalConfig;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

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

    // Field offsets within the HKEY_PERFORMANCE_DATA structures, from winperf.h on 64-bit Windows. The walk below
    // needs a handful of fields from five structs; naming them here keeps the arithmetic in one place rather than
    // once per native backend. HkeyPerformanceDataUtilTest lays out a synthetic block at these offsets written
    // independently, so a wrong constant reads the wrong place and fails there.
    private static final long DATA_HEADER_LENGTH = 24;
    private static final long DATA_NUM_OBJECT_TYPES = 28;
    private static final long DATA_PERF_TIME_100N_SEC = 72;

    private static final long OBJECT_TOTAL_BYTE_LENGTH = 0;
    private static final long OBJECT_DEFINITION_LENGTH = 4;
    private static final long OBJECT_HEADER_LENGTH = 8;
    private static final long OBJECT_NAME_TITLE_INDEX = 12;
    private static final long OBJECT_NUM_COUNTERS = 32;
    private static final long OBJECT_NUM_INSTANCES = 40;

    private static final long COUNTER_DEF_BYTE_LENGTH = 0;
    private static final long COUNTER_DEF_NAME_TITLE_INDEX = 4;
    private static final long COUNTER_DEF_COUNTER_SIZE = 32;
    private static final long COUNTER_DEF_COUNTER_OFFSET = 36;

    private static final long INSTANCE_DEF_BYTE_LENGTH = 0;
    private static final long INSTANCE_DEF_NAME_OFFSET = 16;

    private static final long COUNTER_BLOCK_BYTE_LENGTH = 0;

    /**
     * Whether HKEY_PERFORMANCE_DATA may be read at all, per {@link GlobalConfig#OSHI_OS_WINDOWS_HKEYPERFDATA}. Lives
     * here rather than on one caller because the setting governs the registry itself, and is documented as covering
     * both processes and threads.
     */
    static final boolean PERFDATA = GlobalConfig.get(GlobalConfig.OSHI_OS_WINDOWS_HKEYPERFDATA, true);

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
    protected static Map<String, Integer> buildCounterIndexMap(String @Nullable [] counterText) {
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
    protected static <T extends Enum<T> & PdhCounterWildcardProperty> @Nullable Pair<Integer, EnumMap<T, Integer>> getCounterIndices(
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

    /**
     * Walks a populated HKEY_PERFORMANCE_DATA block and extracts the counters named by an enum.
     * <p>
     * The block is a flat buffer of variable-length records: a header, then object types, each holding counter
     * definitions followed by instances, each instance followed by its counter block. Every step is offset arithmetic,
     * so this is shared rather than written once per native backend.
     *
     * @param <T>          The counter enum type. Its first constant names the instance; the rest name counters.
     * @param buffer       The populated performance data block
     * @param objectIndex  Index of the object type to extract
     * @param enumIndexMap Counter enum values mapped to their registry index
     * @param counterEnum  The counter enum class
     * @return A triplet of the per-instance counter maps, the timestamp in 100nSec units of the Windows 1601 epoch, and
     *         the same timestamp in milliseconds of the 1970 epoch; or null if the object was not present or a counter
     *         could not be read
     */
    protected static <T extends Enum<T> & PdhCounterWildcardProperty> @Nullable Triplet<List<Map<T, Object>>, Long, Long> parsePerfData(
            PerfDataBuffer buffer, int objectIndex, EnumMap<T, Integer> enumIndexMap, Class<T> counterEnum) {
        long perfTime100nSec = buffer.getLong(DATA_PERF_TIME_100N_SEC); // 1601
        long now = ParseUtil.filetimeToUtcMs(perfTime100nSec, false); // 1970

        // Iterate object types
        long perfObjectOffset = buffer.getInt(DATA_HEADER_LENGTH);
        int numObjectTypes = buffer.getInt(DATA_NUM_OBJECT_TYPES);
        for (int obj = 0; obj < numObjectTypes; obj++) {
            if (buffer.getInt(perfObjectOffset + OBJECT_NAME_TITLE_INDEX) == objectIndex) {
                // Counter definitions start after the object header
                long perfCounterOffset = perfObjectOffset + buffer.getInt(perfObjectOffset + OBJECT_HEADER_LENGTH);
                int numCounters = buffer.getInt(perfObjectOffset + OBJECT_NUM_COUNTERS);
                Map<Integer, Integer> counterOffsetMap = new HashMap<>();
                Map<Integer, Integer> counterSizeMap = new HashMap<>();
                for (int counter = 0; counter < numCounters; counter++) {
                    int nameTitleIndex = buffer.getInt(perfCounterOffset + COUNTER_DEF_NAME_TITLE_INDEX);
                    counterOffsetMap.put(nameTitleIndex, buffer.getInt(perfCounterOffset + COUNTER_DEF_COUNTER_OFFSET));
                    counterSizeMap.put(nameTitleIndex, buffer.getInt(perfCounterOffset + COUNTER_DEF_COUNTER_SIZE));
                    perfCounterOffset += buffer.getInt(perfCounterOffset + COUNTER_DEF_BYTE_LENGTH);
                }

                // Instances start after all the object definitions
                long perfInstanceOffset = perfObjectOffset + buffer.getInt(perfObjectOffset + OBJECT_DEFINITION_LENGTH);
                int numInstances = buffer.getInt(perfObjectOffset + OBJECT_NUM_INSTANCES);
                T[] counterKeys = counterEnum.getEnumConstants();
                List<Map<T, Object>> counterMaps = new ArrayList<>(numInstances);
                for (int inst = 0; inst < numInstances; inst++) {
                    long perfCounterBlockOffset = perfInstanceOffset
                            + buffer.getInt(perfInstanceOffset + INSTANCE_DEF_BYTE_LENGTH);
                    Map<T, Object> counterMap = new EnumMap<>(counterEnum);
                    // The first enum constant names the instance rather than a counter
                    long nameOffset = buffer.getInt(perfInstanceOffset + INSTANCE_DEF_NAME_OFFSET);
                    counterMap.put(counterKeys[0], buffer.getWideString(perfInstanceOffset + nameOffset));
                    for (int i = 1; i < counterKeys.length; i++) {
                        T key = counterKeys[i];
                        int keyIndex = enumIndexMap.getOrDefault(key, -1);
                        Integer offset = counterOffsetMap.get(keyIndex);
                        if (offset == null) {
                            return null;
                        }
                        int size = counterSizeMap.getOrDefault(keyIndex, 0);
                        if (size == 4) {
                            counterMap.put(key, buffer.getInt(perfCounterBlockOffset + offset));
                        } else if (size == 8) {
                            counterMap.put(key, buffer.getLong(perfCounterBlockOffset + offset));
                        } else {
                            return null;
                        }
                    }
                    counterMaps.add(counterMap);
                    perfInstanceOffset = perfCounterBlockOffset
                            + buffer.getInt(perfCounterBlockOffset + COUNTER_BLOCK_BYTE_LENGTH);
                }
                return new Triplet<>(counterMaps, perfTime100nSec, now);
            }
            perfObjectOffset += buffer.getInt(perfObjectOffset + OBJECT_TOTAL_BYTE_LENGTH);
        }
        return null;
    }
}
