/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.driver.windows.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory; // NOSONAR squid:S1191
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
import com.sun.jna.ptr.IntByReference;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.platform.windows.PerfCounterWildcardQuery.PdhCounterWildcardProperty;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

/**
 * Utility to read HKEY_PERFORMANCE_DATA information.
 */
@ThreadSafe
public final class HkeyPerformanceDataUtil {

    private static final Logger LOG = LoggerFactory.getLogger(HkeyPerformanceDataUtil.class);

    /*
     * Do a one-time lookup of the HKEY_PERFORMANCE_TEXT counter indices and store
     * in a map for efficient lookups on-demand.
     */
    private static final String HKEY_PERFORMANCE_TEXT = "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Perflib\\009";
    private static final String COUNTER = "Counter";
    private static final Map<String, Integer> COUNTER_INDEX_MAP = mapCounterIndicesFromRegistry();

    private HkeyPerformanceDataUtil() {
    }

    /**
     * Reads and parses a block of performance data from the registry.
     *
     * @param <T>
     *            PDH Counters use an Enum to identify the fields to query in either
     *            the counter or WMI backup, and use the enum values as keys to
     *            retrieve the results.
     * @param objectName
     *            The counter object for which to fetch data
     * @param counterEnum
     *            Which counters to return data for
     * @return A triplet containing the results. The first element maps the input
     *         enum to the counter values where the first enum will contain the
     *         instance name as a {@link String}, and the remaining values will
     *         either be {@link Long}, {@link Integer}, or {@code null} depending on
     *         whether the specified enum counter was present and the size of the
     *         counter value. The second element is a timestamp in 100nSec
     *         increments (Windows 1601 Epoch) while the third element is a
     *         timestamp in milliseconds since the 1970 Epoch.
     */
    public static <T extends Enum<T> & PdhCounterWildcardProperty> Triplet<List<Map<T, Object>>, Long, Long> readPerfDataFromRegistry(
            String objectName, Class<T> counterEnum) {
        // Load indices
        // e.g., call with "Process" and ProcessPerformanceProperty.class
        Pair<Integer, EnumMap<T, Integer>> indices = getCounterIndices(objectName, counterEnum);
        if (indices == null) {
            return null;
        }
        // The above test checks validity of objectName as an index but it could still
        // fail to read
        Memory pPerfData = readPerfDataBuffer(objectName);
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
        PERF_DATA_BLOCK perfData = new PERF_DATA_BLOCK(pPerfData.share(0));
        long perfTime100nSec = perfData.PerfTime100nSec.getValue(); // 1601
        long now = System.currentTimeMillis(); // 1970 epoch

        // Iterate object types.
        long perfObjectOffset = perfData.HeaderLength;
        for (int obj = 0; obj < perfData.NumObjectTypes; obj++) {
            PERF_OBJECT_TYPE perfObject = new PERF_OBJECT_TYPE(pPerfData.share(perfObjectOffset));

            // Assume here only the object we're looking for is in this data block and
            // doesn't require reference to other objects. This is true for the Process
            // object. Some counters will require multiple objects and this code is not set
            // up to handle those yet.
            if (perfObject.ObjectNameTitleIndex == COUNTER_INDEX_MAP.get(objectName).intValue()) {
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

                // Instances start after all the object definitions. The DefinitionLength
                // includes both the header and all the definitions.
                long perfInstanceOffset = perfObjectOffset + perfObject.DefinitionLength;

                // Iterate instances and fill map
                List<Map<T, Object>> counterMaps = new ArrayList<>(perfObject.NumInstances);
                for (int inst = 0; inst < perfObject.NumInstances; inst++) {
                    PERF_INSTANCE_DEFINITION perfInstance = new PERF_INSTANCE_DEFINITION(
                            pPerfData.share(perfInstanceOffset));
                    long perfCounterBlockOffset = perfInstanceOffset + perfInstance.ByteLength;
                    // Populate the enumMap
                    Map<T, Object> counterMap = new EnumMap<>(counterEnum);
                    T[] counterKeys = counterEnum.getEnumConstants();
                    // First enum index is the name, ignore the counter text which is used for other
                    // purposes
                    counterMap.put(counterKeys[0],
                            pPerfData.getWideString(perfInstanceOffset + perfInstance.NameOffset));
                    for (int i = 1; i < counterKeys.length; i++) {
                        T key = counterKeys[i];
                        int keyIndex = COUNTER_INDEX_MAP.get(key.getCounter());
                        // All entries in size map have corresponding entry in offset map
                        int size = counterSizeMap.getOrDefault(keyIndex, 0);
                        // Currently, only DWORDs (4 bytes) and ULONGLONGs (8 bytes) are used to provide
                        // counter values.
                        if (size == 4) {
                            counterMap.put(key,
                                    pPerfData.getInt(perfCounterBlockOffset + counterOffsetMap.get(keyIndex)));
                        } else if (size == 8) {
                            counterMap.put(key,
                                    pPerfData.getLong(perfCounterBlockOffset + counterOffsetMap.get(keyIndex)));
                        } else {
                            counterMap.put(key, null);
                        }
                    }
                    counterMaps.add(counterMap);

                    // counters at perfCounterBlockOffset + appropriate offset per enum
                    // use pPerfData.getInt or getLong as determined by counter size
                    // Currently, only DWORDs (4 bytes) and ULONGLONGs (8 bytes) are used to provide
                    // counter values.

                    // Increment to next instance
                    perfInstanceOffset = perfCounterBlockOffset
                            + new PERF_COUNTER_BLOCK(pPerfData.share(perfCounterBlockOffset)).ByteLength;
                }
                // We've found the necessary object and are done, no need to look at any other
                // objects (shouldn't be any). Return results
                return new Triplet<>(counterMaps, perfTime100nSec, now);
            }
            // Increment for next object (should never need this)
            perfObjectOffset += perfObject.TotalByteLength;
        }
        // Failed, return null
        return null;
    }

    /**
     * Looks up the counter index values for the given counter object and the enum
     * of counter names.
     *
     * @param <T>
     *            An enum containing the counters, whose class is passed as
     *            {@code counterEnum}
     * @param objectName
     *            The counter object to look up the index for
     * @param counterEnum
     *            The {@link Enum} containing counters to look up the indices for.
     *            The first Enum value will be ignored.
     * @return A {@link Pair} containing the index of the counter object as the
     *         first element, and an {@link EnumMap} mapping counter enum values to
     *         their index as the second element, if the lookup is successful; null
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
     * @param objectName
     *            The counter object for which to fetch data. It is the user's
     *            responsibility to ensure this key exists in
     *            {@link #COUNTER_INDEX_MAP}.
     * @return A buffer containing the data if successful, null otherwise.
     */
    private static Memory readPerfDataBuffer(String objectName) {
        // Need this index as a string
        String objectIndexStr = Integer.toString(COUNTER_INDEX_MAP.get(objectName));

        // Now load the data from the regsitry.
        int bufferSize = 4096;
        IntByReference lpcbData = new IntByReference(bufferSize);
        Memory pPerfData = new Memory(bufferSize);
        int ret = Advapi32.INSTANCE.RegQueryValueEx(WinReg.HKEY_PERFORMANCE_DATA, objectIndexStr, 0, null, pPerfData,
                lpcbData);
        if (ret != WinError.ERROR_SUCCESS && ret != WinError.ERROR_MORE_DATA) {
            LOG.error("Error reading performance data from registry for {}.", objectName);
            return null;
        }
        // Grow buffer as needed to fit the data
        while (ret == WinError.ERROR_MORE_DATA) {
            bufferSize += 4096;
            lpcbData.setValue(bufferSize);
            pPerfData = new Memory(bufferSize);
            ret = Advapi32.INSTANCE.RegQueryValueEx(WinReg.HKEY_PERFORMANCE_DATA, objectIndexStr, 0, null, pPerfData,
                    lpcbData);
        }
        return pPerfData;
    }

    /*
     * Registry entries subordinate to HKEY_PERFORMANCE_TEXT key reference the text
     * strings that describe counters in US English. Not supported in Windows 2000.
     *
     * With the "Counter" value, the resulting array contains alternating index/name
     * pairs "1", "1847", "2", "System", "4", "Memory", ...
     *
     * These pairs are translated to a map for later lookup.
     *
     * @return An unmodifiable map containing counter name strings as keys and
     * indices as integer values if the key is read successfully; an empty map
     * otherwise.
     */
    private static Map<String, Integer> mapCounterIndicesFromRegistry() {
        HashMap<String, Integer> indexMap = new HashMap<>();
        try {
            String[] counterText = Advapi32Util.registryGetStringArray(WinReg.HKEY_LOCAL_MACHINE, HKEY_PERFORMANCE_TEXT,
                    COUNTER);
            for (int i = 1; i < counterText.length; i += 2) {
                indexMap.put(counterText[i], Integer.parseInt(counterText[i - 1]));
            }
        } catch (Win32Exception we) {
            LOG.error(
                    "Unable to locate English counter names in registry Perflib 009. Counters may need to be rebuilt: ",
                    we);
        } catch (NumberFormatException nfe) {
            // Unexpected to ever get this, but handling it anyway
            LOG.error("Unable to parse English counter names in registry Perflib 009.");
        }
        return Collections.unmodifiableMap(indexMap);
    }
}