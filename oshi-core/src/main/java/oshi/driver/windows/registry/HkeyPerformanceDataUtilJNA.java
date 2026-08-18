/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.registry;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory;
import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinReg;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.PdhCounterWildcardProperty;
import oshi.driver.common.windows.registry.HkeyPerformanceDataUtil;
import oshi.driver.common.windows.registry.PerfDataBuffer;
import oshi.jna.ByRef.CloseableIntByReference;
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
     *         present and the size of the counter value, or {@code null} if the object could not be read. The second
     *         element is a timestamp in 100nSec increments (Windows 1601 Epoch) while the third element is a timestamp
     *         in milliseconds since the 1970 Epoch.
     */
    public static <T extends Enum<T> & PdhCounterWildcardProperty> @Nullable Triplet<List<Map<T, Object>>, Long, Long> readPerfDataFromRegistry(
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
            return parsePerfData(new MemoryPerfDataBuffer(pPerfData), objectIndex, enumIndexMap, counterEnum);
        }
    }

    /**
     * Reads the performance data block through JNA's {@link Memory}.
     */
    private static final class MemoryPerfDataBuffer implements PerfDataBuffer {

        private final Memory memory;

        private MemoryPerfDataBuffer(Memory memory) {
            this.memory = memory;
        }

        @Override
        public int getInt(long offset) {
            return this.memory.getInt(offset);
        }

        @Override
        public long getLong(long offset) {
            return this.memory.getLong(offset);
        }

        @Override
        public String getWideString(long offset) {
            return this.memory.getWideString(offset);
        }
    }

    /**
     * Read the performance data for a counter object from the registry.
     *
     * @param objectName The counter object for which to fetch data.
     * @return A buffer containing the data if successful, null otherwise.
     */
    private static synchronized @Nullable Memory readPerfDataBuffer(String objectName) {
        Integer objectIndex = COUNTER_INDEX_MAP.get(objectName);
        if (objectIndex == null) {
            LOG.error("No counter index for performance object {}.", objectName);
            return null;
        }
        String objectIndexStr = objectIndex.toString();

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
            // The buffer-grow retry can exit on a hard error rather than ERROR_SUCCESS; return null instead of an
            // unvalidated buffer. Matches HkeyPerformanceDataUtilFFM.
            if (ret != WinError.ERROR_SUCCESS) {
                LOG.error("Error reading performance data from registry for {}.", objectName);
                pPerfData.close();
                return null;
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
