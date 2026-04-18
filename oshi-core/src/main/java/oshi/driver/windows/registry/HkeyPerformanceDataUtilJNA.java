/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.registry;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory;
import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinPerf.PERF_COUNTER_BLOCK;
import com.sun.jna.platform.win32.WinPerf.PERF_COUNTER_DEFINITION;
import com.sun.jna.platform.win32.WinPerf.PERF_DATA_BLOCK;
import com.sun.jna.platform.win32.WinPerf.PERF_INSTANCE_DEFINITION;
import com.sun.jna.platform.win32.WinPerf.PERF_OBJECT_TYPE;
import com.sun.jna.platform.win32.WinReg;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.PdhCounterWildcardProperty;
import oshi.driver.common.windows.registry.HkeyPerformanceDataUtil;
import oshi.jna.ByRef.CloseableIntByReference;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

/**
 * Utility to read HKEY_PERFORMANCE_DATA information.
 */
@ThreadSafe
public final class HkeyPerformanceDataUtilJNA extends HkeyPerformanceDataUtil {

    private static final Logger LOG = LoggerFactory.getLogger(HkeyPerformanceDataUtilJNA.class);

    private static final Map<String, Integer> COUNTER_INDEX_MAP = mapCounterIndicesFromRegistry();

    private static int maxPerfBufferSize = 16384;

    private HkeyPerformanceDataUtilJNA() {
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
        Pair<Integer, EnumMap<T, Integer>> indices = getCounterIndices(objectName, counterEnum, COUNTER_INDEX_MAP);
        if (indices == null) {
            return null;
        }
        int objectIndex = indices.getA();
        EnumMap<T, Integer> enumIndexMap = indices.getB();
        // The above test checks validity of objectName as an index but it could still
        // fail to read
        try (Memory pPerfData = readPerfDataBuffer(objectName)) {
            if (pPerfData == null) {
                return null;
            }
            // Buffer is now successfully populated.

            // Store timestamp
            PERF_DATA_BLOCK perfData = new PERF_DATA_BLOCK(pPerfData.share(0));
            long perfTime100nSec = perfData.PerfTime100nSec.getValue(); // 1601
            long now = ParseUtil.filetimeToUtcMs(perfTime100nSec, false); // 1970

            // Iterate object types.
            long perfObjectOffset = perfData.HeaderLength;
            for (int obj = 0; obj < perfData.NumObjectTypes; obj++) {
                PERF_OBJECT_TYPE perfObject = new PERF_OBJECT_TYPE(pPerfData.share(perfObjectOffset));
                if (perfObject.ObjectNameTitleIndex == objectIndex) {
                    // We found a matching object.

                    // Counter definitions start after the object header
                    long perfCounterOffset = perfObjectOffset + perfObject.HeaderLength;
                    // Iterate counter definitions and fill maps with counter offsets and sizes
                    Map<Integer, Integer> counterOffsetMap = new HashMap<>();
                    Map<Integer, Integer> counterSizeMap = new HashMap<>();
                    for (int counter = 0; counter < perfObject.NumCounters; counter++) {
                        PERF_COUNTER_DEFINITION perfCounter = new PERF_COUNTER_DEFINITION(
                                pPerfData.share(perfCounterOffset));
                        counterOffsetMap.put(perfCounter.CounterNameTitleIndex, perfCounter.CounterOffset);
                        counterSizeMap.put(perfCounter.CounterNameTitleIndex, perfCounter.CounterSize);
                        // Increment for next Counter
                        perfCounterOffset += perfCounter.ByteLength;
                    }

                    // Instances start after all the object definitions.
                    long perfInstanceOffset = perfObjectOffset + perfObject.DefinitionLength;

                    // Iterate instances and fill map
                    T[] counterKeys = counterEnum.getEnumConstants();
                    List<Map<T, Object>> counterMaps = new ArrayList<>(perfObject.NumInstances);
                    for (int inst = 0; inst < perfObject.NumInstances; inst++) {
                        PERF_INSTANCE_DEFINITION perfInstance = new PERF_INSTANCE_DEFINITION(
                                pPerfData.share(perfInstanceOffset));
                        long perfCounterBlockOffset = perfInstanceOffset + perfInstance.ByteLength;
                        // Populate the enumMap
                        Map<T, Object> counterMap = new EnumMap<>(counterEnum);
                        counterMap.put(counterKeys[0],
                                pPerfData.getWideString(perfInstanceOffset + perfInstance.NameOffset));
                        for (int i = 1; i < counterKeys.length; i++) {
                            T key = counterKeys[i];
                            int keyIndex = enumIndexMap.get(key);
                            int size = counterSizeMap.getOrDefault(keyIndex, 0);
                            if (size == 4) {
                                counterMap.put(key,
                                        pPerfData.getInt(perfCounterBlockOffset + counterOffsetMap.get(keyIndex)));
                            } else if (size == 8) {
                                counterMap.put(key,
                                        pPerfData.getLong(perfCounterBlockOffset + counterOffsetMap.get(keyIndex)));
                            } else {
                                return null;
                            }
                        }
                        counterMaps.add(counterMap);

                        // Increment to next instance
                        perfInstanceOffset = perfCounterBlockOffset
                                + new PERF_COUNTER_BLOCK(pPerfData.share(perfCounterBlockOffset)).ByteLength;
                    }
                    return new Triplet<>(counterMaps, perfTime100nSec, now);
                }
                // Increment for next object
                perfObjectOffset += perfObject.TotalByteLength;
            }
        }
        return null;
    }

    /**
     * Read the performance data for a counter object from the registry.
     *
     * @param objectName The counter object for which to fetch data.
     * @return A buffer containing the data if successful, null otherwise.
     */
    private static synchronized Memory readPerfDataBuffer(String objectName) {
        String objectIndexStr = Integer.toString(COUNTER_INDEX_MAP.get(objectName));

        try (CloseableIntByReference lpcbData = new CloseableIntByReference(maxPerfBufferSize)) {
            Memory pPerfData = new Memory(maxPerfBufferSize);
            int ret = Advapi32.INSTANCE.RegQueryValueEx(WinReg.HKEY_PERFORMANCE_DATA, objectIndexStr, 0, null,
                    pPerfData, lpcbData);
            if (ret != WinError.ERROR_SUCCESS && ret != WinError.ERROR_MORE_DATA) {
                LOG.error("Error reading performance data from registry for {}.", objectName);
                pPerfData.close();
                return null;
            }
            while (ret == WinError.ERROR_MORE_DATA) {
                maxPerfBufferSize += 8192;
                lpcbData.setValue(maxPerfBufferSize);
                pPerfData.close();
                pPerfData = new Memory(maxPerfBufferSize);
                ret = Advapi32.INSTANCE.RegQueryValueEx(WinReg.HKEY_PERFORMANCE_DATA, objectIndexStr, 0, null,
                        pPerfData, lpcbData);
            }
            return pPerfData;
        }
    }

    private static Map<String, Integer> mapCounterIndicesFromRegistry() {
        try {
            String[] counterText = Advapi32Util.registryGetStringArray(WinReg.HKEY_LOCAL_MACHINE, HKEY_PERFORMANCE_TEXT,
                    COUNTER);
            return buildCounterIndexMap(counterText);
        } catch (Win32Exception we) {
            LOG.error(
                    "Unable to locate English counter names in registry Perflib 009. Counters may need to be rebuilt: ",
                    we);
        }
        return buildCounterIndexMap(null);
    }
}
