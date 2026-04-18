/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.registry;

import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static oshi.ffm.windows.Advapi32FFM.RegQueryValueEx;
import static oshi.ffm.windows.WinErrorFFM.ERROR_MORE_DATA;
import static oshi.ffm.windows.WinErrorFFM.ERROR_SUCCESS;
import static oshi.ffm.windows.WinPerfFFM.PERF_COUNTER_BLOCK_ByteLength;
import static oshi.ffm.windows.WinPerfFFM.PERF_COUNTER_DEF_ByteLength;
import static oshi.ffm.windows.WinPerfFFM.PERF_COUNTER_DEF_CounterNameTitleIndex;
import static oshi.ffm.windows.WinPerfFFM.PERF_COUNTER_DEF_CounterOffset;
import static oshi.ffm.windows.WinPerfFFM.PERF_COUNTER_DEF_CounterSize;
import static oshi.ffm.windows.WinPerfFFM.PERF_DATA_BLOCK_HeaderLength;
import static oshi.ffm.windows.WinPerfFFM.PERF_DATA_BLOCK_NumObjectTypes;
import static oshi.ffm.windows.WinPerfFFM.PERF_DATA_BLOCK_PerfTime100nSec;
import static oshi.ffm.windows.WinPerfFFM.PERF_INSTANCE_DEF_ByteLength;
import static oshi.ffm.windows.WinPerfFFM.PERF_INSTANCE_DEF_NameOffset;
import static oshi.ffm.windows.WinPerfFFM.PERF_OBJECT_TYPE_DefinitionLength;
import static oshi.ffm.windows.WinPerfFFM.PERF_OBJECT_TYPE_HeaderLength;
import static oshi.ffm.windows.WinPerfFFM.PERF_OBJECT_TYPE_NumCounters;
import static oshi.ffm.windows.WinPerfFFM.PERF_OBJECT_TYPE_NumInstances;
import static oshi.ffm.windows.WinPerfFFM.PERF_OBJECT_TYPE_ObjectNameTitleIndex;
import static oshi.ffm.windows.WinPerfFFM.PERF_OBJECT_TYPE_TotalByteLength;
import static oshi.ffm.windows.WindowsForeignFunctions.readWideString;
import static oshi.ffm.windows.WindowsForeignFunctions.toWideString;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.PdhCounterWildcardProperty;
import oshi.ffm.windows.WinRegFFM;
import oshi.util.ParseUtil;
import oshi.util.platform.windows.Advapi32UtilFFM;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

/**
 * Utility to read HKEY_PERFORMANCE_DATA information using the FFM API.
 */
@ThreadSafe
public final class HkeyPerformanceDataUtilFFM {

    private static final Logger LOG = LoggerFactory.getLogger(HkeyPerformanceDataUtilFFM.class);

    /*
     * Do a one-time lookup of the HKEY_PERFORMANCE_TEXT counter indices and store in a map for efficient lookups
     * on-demand.
     */
    private static final String HKEY_PERFORMANCE_TEXT = "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Perflib\\009";
    private static final String COUNTER = "Counter";
    private static final Map<String, Integer> COUNTER_INDEX_MAP = mapCounterIndicesFromRegistry();

    private static int maxPerfBufferSize = 16384;

    private HkeyPerformanceDataUtilFFM() {
    }

    /**
     * Reads and parses a block of performance data from the registry.
     *
     * @param <T>         PDH Counters use an Enum to identify the fields to query in either the counter or WMI backup,
     *                    and use the enum values as keys to retrieve the results.
     * @param objectName  The counter object for which to fetch data
     * @param counterEnum Which counters to return data for
     * @return A triplet containing the results. The first element maps the input enum to the counter values where the
     *         first enum will contain the instance name as a {@link String}, and the remaining values will either be
     *         {@link Long}, {@link Integer}, or {@code null} depending on whether the specified enum counter was
     *         present and the size of the counter value. The second element is a timestamp in 100nSec increments
     *         (Windows 1601 Epoch) while the third element is a timestamp in milliseconds since the 1970 Epoch.
     */
    public static <T extends Enum<T> & PdhCounterWildcardProperty> Triplet<List<Map<T, Object>>, Long, Long> readPerfDataFromRegistry(
            String objectName, Class<T> counterEnum) {
        // Load indices
        // e.g., call with "Process" and ProcessPerformanceProperty.class
        Pair<Integer, EnumMap<T, Integer>> indices = getCounterIndices(objectName, counterEnum);
        if (indices == null) {
            return null;
        }
        int objectIndex = indices.getA();
        EnumMap<T, Integer> enumIndexMap = indices.getB();
        // The above test checks validity of objectName as an index but it could still
        // fail to read
        MemorySegment pPerfData = readPerfDataBuffer(objectName);
        if (pPerfData == null) {
            return null;
        }
        // Buffer is now successfully populated.
        // See format at
        // https://msdn.microsoft.com/en-us/library/windows/desktop/aa373105(v=vs.85).aspx

        // Start with a data header (PERF_DATA_BLOCK)
        // Then iterate one or more objects
        // Each object contains
        // [ ] Object Type header (PERF_OBJECT_TYPE)
        // [ ][ ][ ] Multiple counter definitions (PERF_COUNTER_DEFINITION)
        // Then after object(s), multiple:
        // [ ] Instance Definition
        // [ ] Instance name
        // [ ] Counter Block
        // [ ][ ][ ] Counter data for each definition above

        // Store timestamp
        long perfTime100nSec = pPerfData.get(JAVA_LONG, PERF_DATA_BLOCK_PerfTime100nSec); // 1601
        long now = ParseUtil.filetimeToUtcMs(perfTime100nSec, false); // 1970

        // Iterate object types.
        long perfObjectOffset = pPerfData.get(JAVA_INT, PERF_DATA_BLOCK_HeaderLength);
        int numObjectTypes = pPerfData.get(JAVA_INT, PERF_DATA_BLOCK_NumObjectTypes);
        for (int obj = 0; obj < numObjectTypes; obj++) {
            // Some counters will require multiple objects so we iterate until we find the
            // right one. e.g. Process (230) is by itself but Thread (232) has Process
            // object first
            int objectNameTitleIndex = pPerfData.get(JAVA_INT,
                    perfObjectOffset + PERF_OBJECT_TYPE_ObjectNameTitleIndex);
            if (objectNameTitleIndex == objectIndex) {
                // We found a matching object.

                // Counter definitions start after the object header
                long perfCounterOffset = perfObjectOffset
                        + pPerfData.get(JAVA_INT, perfObjectOffset + PERF_OBJECT_TYPE_HeaderLength);
                int numCounters = pPerfData.get(JAVA_INT, perfObjectOffset + PERF_OBJECT_TYPE_NumCounters);

                // Iterate counter definitions and fill maps with counter offsets and sizes
                Map<Integer, Integer> counterOffsetMap = new HashMap<>();
                Map<Integer, Integer> counterSizeMap = new HashMap<>();
                for (int counter = 0; counter < numCounters; counter++) {
                    int counterNameTitleIndex = pPerfData.get(JAVA_INT,
                            perfCounterOffset + PERF_COUNTER_DEF_CounterNameTitleIndex);
                    int counterOffset = pPerfData.get(JAVA_INT, perfCounterOffset + PERF_COUNTER_DEF_CounterOffset);
                    int counterSize = pPerfData.get(JAVA_INT, perfCounterOffset + PERF_COUNTER_DEF_CounterSize);
                    counterOffsetMap.put(counterNameTitleIndex, counterOffset);
                    counterSizeMap.put(counterNameTitleIndex, counterSize);
                    // Increment for next Counter
                    perfCounterOffset += pPerfData.get(JAVA_INT, perfCounterOffset + PERF_COUNTER_DEF_ByteLength);
                }

                // Instances start after all the object definitions. The DefinitionLength
                // includes both the header and all the definitions.
                long perfInstanceOffset = perfObjectOffset
                        + pPerfData.get(JAVA_INT, perfObjectOffset + PERF_OBJECT_TYPE_DefinitionLength);
                int numInstances = pPerfData.get(JAVA_INT, perfObjectOffset + PERF_OBJECT_TYPE_NumInstances);

                // Iterate instances and fill map
                T[] counterKeys = counterEnum.getEnumConstants();
                List<Map<T, Object>> counterMaps = new ArrayList<>(numInstances);
                for (int inst = 0; inst < numInstances; inst++) {
                    int instanceByteLength = pPerfData.get(JAVA_INT, perfInstanceOffset + PERF_INSTANCE_DEF_ByteLength);
                    int nameOffset = pPerfData.get(JAVA_INT, perfInstanceOffset + PERF_INSTANCE_DEF_NameOffset);
                    long perfCounterBlockOffset = perfInstanceOffset + instanceByteLength;

                    // Populate the enumMap
                    Map<T, Object> counterMap = new EnumMap<>(counterEnum);
                    // First enum index is the name, ignore the counter text which is used for other
                    // purposes
                    counterMap.put(counterKeys[0], readWideString(pPerfData.asSlice(perfInstanceOffset + nameOffset)));
                    for (int i = 1; i < counterKeys.length; i++) {
                        T key = counterKeys[i];
                        int keyIndex = enumIndexMap.get(key);
                        // All entries in size map have corresponding entry in offset map
                        int size = counterSizeMap.getOrDefault(keyIndex, 0);
                        // Currently, only DWORDs (4 bytes) and ULONGLONGs (8 bytes) are used to provide
                        // counter values.
                        if (size == 4) {
                            counterMap.put(key,
                                    pPerfData.get(JAVA_INT, perfCounterBlockOffset + counterOffsetMap.get(keyIndex)));
                        } else if (size == 8) {
                            counterMap.put(key,
                                    pPerfData.get(JAVA_LONG, perfCounterBlockOffset + counterOffsetMap.get(keyIndex)));
                        } else {
                            // If counter defined in enum isn't in registry, fail
                            return null;
                        }
                    }
                    counterMaps.add(counterMap);

                    // counters at perfCounterBlockOffset + appropriate offset per enum
                    // use pPerfData.getInt or getLong as determined by counter size
                    // Currently, only DWORDs (4 bytes) and ULONGLONGs (8 bytes) are used to provide
                    // counter values.

                    // Increment to next instance
                    perfInstanceOffset = perfCounterBlockOffset
                            + pPerfData.get(JAVA_INT, perfCounterBlockOffset + PERF_COUNTER_BLOCK_ByteLength);
                }
                // We've found the necessary object and are done, no need to look at any other
                // objects (shouldn't be any). Return results
                return new Triplet<>(counterMaps, perfTime100nSec, now);
            }
            // Increment for next object
            perfObjectOffset += pPerfData.get(JAVA_INT, perfObjectOffset + PERF_OBJECT_TYPE_TotalByteLength);
        }
        // Failed, return null
        return null;
    }

    /**
     * Looks up the counter index values for the given counter object and the enum of counter names.
     *
     * @param <T>         An enum containing the counters, whose class is passed as {@code counterEnum}
     * @param objectName  The counter object to look up the index for
     * @param counterEnum The {@link Enum} containing counters to look up the indices for. The first Enum value will be
     *                    ignored.
     * @return A {@link Pair} containing the index of the counter object as the first element, and an {@link EnumMap}
     *         mapping counter enum values to their index as the second element, if the lookup is successful; null
     *         otherwise.
     */
    private static <T extends Enum<T> & PdhCounterWildcardProperty> Pair<Integer, EnumMap<T, Integer>> getCounterIndices(
            String objectName, Class<T> counterEnum) {
        if (!COUNTER_INDEX_MAP.containsKey(objectName)) {
            LOG.debug("Couldn't find counter index of {}.", objectName);
            return null;
        }
        int counterIndex = COUNTER_INDEX_MAP.get(objectName);
        T[] enumConstants = counterEnum.getEnumConstants();
        EnumMap<T, Integer> indexMap = new EnumMap<>(counterEnum);
        // Start iterating at 1 because first Enum value defines the name/instance and
        // is not a counter name
        for (int i = 1; i < enumConstants.length; i++) {
            T key = enumConstants[i];
            String counterName = key.getCounter();
            if (!COUNTER_INDEX_MAP.containsKey(counterName)) {
                LOG.debug("Couldn't find counter index of {}.", counterName);
                return null;
            }
            indexMap.put(key, COUNTER_INDEX_MAP.get(counterName));
        }
        // We have all the pieces! Return them.
        return new Pair<>(counterIndex, indexMap);
    }

    /**
     * Read the performance data for a counter object from the registry.
     *
     * @param objectName The counter object for which to fetch data. It is the user's responsibility to ensure this key
     *                   exists in {@link #COUNTER_INDEX_MAP}.
     * @return A buffer containing the data if successful, null otherwise.
     */
    private static synchronized MemorySegment readPerfDataBuffer(String objectName) {
        // Need this index as a string
        String objectIndexStr = Integer.toString(COUNTER_INDEX_MAP.get(objectName));

        // Now load the data from the registry.
        // Use a global arena so the returned segment outlives this method
        Arena arena = Arena.ofAuto();
        try {
            MemorySegment lpValueName = toWideString(arena, objectIndexStr);
            MemorySegment lpcbData = arena.allocate(JAVA_INT);
            lpcbData.set(JAVA_INT, 0, maxPerfBufferSize);
            MemorySegment pPerfData = arena.allocate(maxPerfBufferSize);

            int ret = RegQueryValueEx(MemorySegment.ofAddress(WinRegFFM.HKEY_PERFORMANCE_DATA), lpValueName, 0,
                    MemorySegment.NULL, pPerfData, lpcbData);
            if (ret != ERROR_SUCCESS && ret != ERROR_MORE_DATA) {
                LOG.error("Error reading performance data from registry for {}.", objectName);
                return null;
            }
            // Grow buffer as needed to fit the data
            while (ret == ERROR_MORE_DATA) {
                maxPerfBufferSize += 8192;
                lpcbData.set(JAVA_INT, 0, maxPerfBufferSize);
                pPerfData = arena.allocate(maxPerfBufferSize);
                ret = RegQueryValueEx(MemorySegment.ofAddress(WinRegFFM.HKEY_PERFORMANCE_DATA), lpValueName, 0,
                        MemorySegment.NULL, pPerfData, lpcbData);
            }
            if (ret != ERROR_SUCCESS) {
                LOG.error("Error reading performance data from registry for {} (ret={}).", objectName, ret);
                return null;
            }
            return pPerfData;
        } catch (Throwable t) {
            LOG.error("Error reading performance data from registry for {}.", objectName, t);
            return null;
        }
    }

    /*
     * Registry entries subordinate to HKEY_PERFORMANCE_TEXT key reference the text strings that describe counters in US
     * English. Not supported in Windows 2000.
     *
     * With the "Counter" value, the resulting array contains alternating index/name pairs "1", "1847", "2", "System",
     * "4", "Memory", ...
     *
     * These pairs are translated to a map for later lookup.
     *
     * @return An unmodifiable map containing counter name strings as keys and indices as integer values if the key is
     * read successfully; an empty map otherwise.
     */
    private static Map<String, Integer> mapCounterIndicesFromRegistry() {
        HashMap<String, Integer> indexMap = new HashMap<>();
        try {
            String[] counterText = Advapi32UtilFFM.registryGetStringArray(
                    MemorySegment.ofAddress(WinRegFFM.HKEY_LOCAL_MACHINE), HKEY_PERFORMANCE_TEXT, COUNTER);
            if (counterText != null && counterText.length > 1) {
                for (int i = 1; i < counterText.length; i += 2) {
                    int idx = ParseUtil.parseIntOrDefault(counterText[i - 1], 0);
                    if (idx > 0) {
                        indexMap.putIfAbsent(counterText[i], idx);
                    }
                }
            }
        } catch (Exception e) {
            LOG.error(
                    "Unable to locate English counter names in registry Perflib 009. Counters may need to be rebuilt: ",
                    e);
        }
        return Collections.unmodifiableMap(indexMap);
    }
}
