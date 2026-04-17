/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.windows;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static oshi.ffm.windows.PdhFFM.PDH_CSTATUS_NEW_DATA;
import static oshi.ffm.windows.PdhFFM.PDH_CSTATUS_VALID_DATA;
import static oshi.ffm.windows.PdhFFM.PDH_MORE_DATA;
import static oshi.ffm.windows.PdhFFM.PDH_RAW_COUNTER_LAYOUT;
import static oshi.ffm.windows.PdhFFM.PdhAddEnglishCounter;
import static oshi.ffm.windows.PdhFFM.PdhCloseQuery;
import static oshi.ffm.windows.PdhFFM.PdhCollectQueryData;
import static oshi.ffm.windows.PdhFFM.PdhEnumObjectItemsW;
import static oshi.ffm.windows.PdhFFM.PdhGetRawCounterValue;
import static oshi.ffm.windows.PdhFFM.PdhLookupPerfNameByIndexW;
import static oshi.ffm.windows.PdhFFM.PdhOpenQuery;
import static oshi.ffm.windows.WinErrorFFM.ERROR_SUCCESS;
import static oshi.ffm.windows.WindowsForeignFunctions.checkSuccess;
import static oshi.ffm.windows.WindowsForeignFunctions.readWideString;
import static oshi.ffm.windows.WindowsForeignFunctions.toWideString;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.driver.common.windows.perfmon.PdhCounterProperty;
import oshi.driver.common.windows.perfmon.PdhCounterWildcardProperty;
import oshi.ffm.windows.WinRegFFM;
import oshi.util.ParseUtil;
import oshi.util.Util;
import oshi.util.tuples.Pair;

/**
 * Helper class to centralize the boilerplate portions of PDH counter setup using the FFM API.
 * <p>
 * This utility collects counter data with a single call to {@code PdhCollectQueryData}, which yields valid results for
 * raw counters. OSHI queries raw counter values and computes rates in Java rather than using PDH formatted rate
 * counters, which would require two samples separated by an interval.
 */
public final class PerfDataUtilFFM {

    private PerfDataUtilFFM() {
    }

    private static final Logger LOG = LoggerFactory.getLogger(PerfDataUtilFFM.class);

    // Cache for English-to-localized object name mapping
    private static final Map<String, String> LOCALIZE_CACHE = new ConcurrentHashMap<>();

    // English counter index registry key
    private static final String ENGLISH_COUNTER_KEY = "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Perflib\\009";
    private static final String ENGLISH_COUNTER_VALUE = "Counter";

    /**
     * Query multiple PDH counter values in a single query, one per enum constant.
     *
     * @param <T>          An enum implementing {@link PdhCounterProperty}
     * @param propertyEnum The enum class whose constants define the counters to query
     * @param perfObject   The PDH object name (e.g., "Process")
     * @return An {@link EnumMap} of values indexed by enum constant. May contain a subset of constants if individual
     *         counters fail to add or read. Returns an empty map if the query itself fails to open or collect.
     */
    public static <T extends Enum<T> & PdhCounterProperty> Map<T, Long> queryCounters(Class<T> propertyEnum,
            String perfObject) {
        T[] props = propertyEnum.getEnumConstants();
        EnumMap<T, Long> valueMap = new EnumMap<>(propertyEnum);

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment queryPtr = arena.allocate(ADDRESS);
            checkSuccess(PdhOpenQuery(MemorySegment.NULL, MemorySegment.NULL, queryPtr));
            MemorySegment query = null;

            try {
                query = queryPtr.get(ADDRESS, 0);

                // Add all counters to the single query, skipping any that fail
                EnumMap<T, MemorySegment> counterHandles = new EnumMap<>(propertyEnum);
                for (T prop : props) {
                    String path = counterPath(perfObject, prop.getInstance(), prop.getCounter());
                    try {
                        counterHandles.put(prop, addEnglishCounter(arena, query, path));
                    } catch (Throwable t) {
                        LOG.debug("Failed to add counter {}: {}", path, t.getMessage());
                    }
                }
                if (counterHandles.isEmpty()) {
                    return valueMap;
                }

                // Collect once
                checkSuccess(PdhCollectQueryData(query));

                // Read all values, skipping any that fail
                for (var entry : counterHandles.entrySet()) {
                    try {
                        valueMap.put(entry.getKey(), readRawCounterValue(arena, entry.getValue()));
                    } catch (Throwable t) {
                        LOG.debug("Failed to read counter for {}: {}", entry.getKey(), t.getMessage());
                    }
                }

            } finally {
                if (query != null) {
                    PdhCloseQuery(query);
                }
            }

        } catch (Throwable t) {
            LOG.debug("PDH queryCounters failed for {}: {}", perfObject, t.getMessage(), t);
            return new EnumMap<>(propertyEnum);
        }
        return valueMap;
    }

    private static String counterPath(String object, String instance, String counter) {
        StringBuilder path = new StringBuilder();
        path.append('\\').append(object);
        if (instance != null) {
            path.append('(').append(instance).append(')');
        }
        path.append('\\').append(counter);
        return path.toString();
    }

    private static MemorySegment addEnglishCounter(Arena arena, MemorySegment query, String path) throws Throwable {
        MemorySegment counterPtr = arena.allocate(ADDRESS);
        checkSuccess(PdhAddEnglishCounter(query, toWideString(arena, path), MemorySegment.NULL, counterPtr));
        return counterPtr.get(ADDRESS, 0);
    }

    private static final long RAW_CSTATUS_OFFSET = PDH_RAW_COUNTER_LAYOUT.byteOffset(groupElement("CStatus"));
    private static final long RAW_FIRST_VALUE_OFFSET = PDH_RAW_COUNTER_LAYOUT.byteOffset(groupElement("FirstValue"));

    private static long readRawCounterValue(Arena arena, MemorySegment counterHandle) throws Throwable {
        MemorySegment value = arena.allocate(PDH_RAW_COUNTER_LAYOUT);
        checkSuccess(PdhGetRawCounterValue(counterHandle, MemorySegment.NULL, value));
        int cStatus = value.get(JAVA_INT, RAW_CSTATUS_OFFSET);
        if (cStatus != PDH_CSTATUS_VALID_DATA && cStatus != PDH_CSTATUS_NEW_DATA) {
            throw new IllegalStateException(
                    String.format(Locale.ROOT, "PDH raw counter CStatus invalid: 0x%08X", cStatus));
        }
        return value.get(JAVA_LONG, RAW_FIRST_VALUE_OFFSET);
    }

    /**
     * Enumerate instances of a PDH performance object, filtering by the wildcard pattern from the first enum constant.
     *
     * @param perfObject     The PDH object name (e.g., "Process")
     * @param instanceFilter The wildcard filter pattern (lowercase)
     * @return A list of matching instance names, or an empty list on failure
     */
    static List<String> enumInstances(String perfObject, String instanceFilter) {
        // PdhEnumObjectItemsW requires the localized object name
        String localizedObject = localizeObjectName(perfObject);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment objectName = toWideString(arena, localizedObject);
            MemorySegment counterLen = arena.allocate(JAVA_INT);
            MemorySegment instanceLen = arena.allocate(JAVA_INT);

            // First call: get required buffer sizes
            counterLen.set(JAVA_INT, 0, 0);
            instanceLen.set(JAVA_INT, 0, 0);
            int result = PdhEnumObjectItemsW(MemorySegment.NULL, MemorySegment.NULL, objectName, MemorySegment.NULL,
                    counterLen, MemorySegment.NULL, instanceLen, 100, 0);
            if (result != ERROR_SUCCESS && result != PDH_MORE_DATA) {
                LOG.warn("Failed to enumerate instances for {}: 0x{}", perfObject, Integer.toHexString(result));
                return Collections.emptyList();
            }

            // Retry loop for race conditions (e.g., process/thread list changes)
            MemorySegment counterBuf;
            MemorySegment instanceBuf;
            int retries = 0;
            do {
                int cLen = counterLen.get(JAVA_INT, 0);
                int iLen = instanceLen.get(JAVA_INT, 0);
                counterBuf = cLen > 0 ? arena.allocate((long) cLen * 2) : MemorySegment.NULL;
                instanceBuf = iLen > 0 ? arena.allocate((long) iLen * 2) : MemorySegment.NULL;

                result = PdhEnumObjectItemsW(MemorySegment.NULL, MemorySegment.NULL, objectName, counterBuf, counterLen,
                        instanceBuf, instanceLen, 100, 0);
                // On PDH_MORE_DATA, counterLen/instanceLen are updated with required sizes;
                // add headroom for race conditions where instances change between calls
                if (result == PDH_MORE_DATA) {
                    counterLen.set(JAVA_INT, 0, counterLen.get(JAVA_INT, 0) + 1024);
                    instanceLen.set(JAVA_INT, 0, instanceLen.get(JAVA_INT, 0) + 1024);
                }
            } while (result == PDH_MORE_DATA && ++retries < 5);

            if (result != ERROR_SUCCESS) {
                LOG.warn("Failed to enumerate instances for {}: 0x{}", localizedObject, Integer.toHexString(result));
                return Collections.emptyList();
            }

            // Parse multi-sz instance list and filter
            List<String> instances = parseMultiSz(instanceBuf);
            instances.removeIf(i -> !Util.wildcardMatch(i.toLowerCase(Locale.ROOT), instanceFilter));
            return instances;
        } catch (Throwable t) {
            LOG.warn("Failed to enumerate instances for {}: {}", localizedObject, t.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Parse a null-terminated double-null-terminated wide string list (multi-sz) into a list of strings.
     *
     * @param buf the memory segment containing the multi-sz wide string data
     * @return a list of parsed strings
     */
    private static List<String> parseMultiSz(MemorySegment buf) {
        List<String> result = new ArrayList<>();
        if (buf == null || buf.equals(MemorySegment.NULL)) {
            return result;
        }
        long offset = 0;
        long size = buf.byteSize();
        while (offset < size) {
            StringBuilder sb = new StringBuilder();
            while (offset < size) {
                char c = buf.get(JAVA_CHAR, offset);
                offset += 2;
                if (c == '\0') {
                    break;
                }
                sb.append(c);
            }
            if (sb.isEmpty()) {
                break; // double null = end of list
            }
            result.add(sb.toString());
        }
        return result;
    }

    /**
     * Query wildcard PDH counter values for all instances matching the filter defined by the first enum constant.
     *
     * @param <T>          An enum implementing {@link PdhCounterWildcardProperty}
     * @param propertyEnum The enum class whose constants define the counters to query. The first constant defines the
     *                     instance filter; remaining constants define counter names.
     * @param perfObject   The PDH object name (e.g., "Process")
     * @return A pair of (instances, valueMap) where valueMap is indexed by enum constant (excluding the first). Returns
     *         empty list and empty map on failure.
     */
    public static <T extends Enum<T> & PdhCounterWildcardProperty> Pair<List<String>, Map<T, List<Long>>> queryWildcardCounters(
            Class<T> propertyEnum, String perfObject) {

        T[] props = propertyEnum.getEnumConstants();
        if (props.length < 2) {
            throw new IllegalArgumentException("Enum " + propertyEnum.getName()
                    + " must have at least two elements, an instance filter and a counter.");
        }
        String instanceFilter = props[0].getCounter().toLowerCase(Locale.ROOT);

        List<String> instances = enumInstances(perfObject, instanceFilter);
        EnumMap<T, List<Long>> valuesMap = new EnumMap<>(propertyEnum);
        if (instances.isEmpty()) {
            return new Pair<>(Collections.emptyList(), valuesMap);
        }

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment queryPtr = arena.allocate(ADDRESS);
            checkSuccess(PdhOpenQuery(MemorySegment.NULL, MemorySegment.NULL, queryPtr));
            MemorySegment query = null;

            try {
                query = queryPtr.get(ADDRESS, 0);

                // Add counters for each instance × property (skip first which is the filter)
                // counterHandles[propIndex - 1][instanceIndex]
                @SuppressWarnings("unchecked")
                List<MemorySegment>[] counterHandles = new List[props.length - 1];
                for (int p = 1; p < props.length; p++) {
                    counterHandles[p - 1] = new ArrayList<>(instances.size());
                    for (String instance : instances) {
                        String path = counterPath(perfObject, instance, props[p].getCounter());
                        try {
                            counterHandles[p - 1].add(addEnglishCounter(arena, query, path));
                        } catch (Throwable t) {
                            LOG.debug("Failed to add counter {}: {}", path, t.getMessage());
                            return new Pair<>(Collections.emptyList(), new EnumMap<>(propertyEnum));
                        }
                    }
                }

                // Collect once
                checkSuccess(PdhCollectQueryData(query));

                // Read values
                for (int p = 1; p < props.length; p++) {
                    List<Long> values = new ArrayList<>(instances.size());
                    for (MemorySegment handle : counterHandles[p - 1]) {
                        try {
                            values.add(readRawCounterValue(arena, handle));
                        } catch (Throwable t) {
                            LOG.debug("Failed to read counter for {}: {}", props[p], t.getMessage());
                            values.add(0L);
                        }
                    }
                    valuesMap.put(props[p], values);
                }
            } finally {
                if (query != null) {
                    PdhCloseQuery(query);
                }
            }
        } catch (Throwable t) {
            LOG.debug("PDH queryWildcardCounters failed for {}: {}", perfObject, t.getMessage(), t);
            return new Pair<>(Collections.emptyList(), new EnumMap<>(propertyEnum));
        }
        return new Pair<>(instances, valuesMap);
    }

    /**
     * Localize a PDH object name. {@code PdhEnumObjectItemsW} requires the localized name unlike
     * {@code PdhAddEnglishCounterW} which handles English names natively.
     *
     * @param perfObject The English object name
     * @return The localized name, or the original if localization fails
     */
    private static String localizeObjectName(String perfObject) {
        return LOCALIZE_CACHE.computeIfAbsent(perfObject, PerfDataUtilFFM::localizeUsingPerfIndex);
    }

    private static String localizeUsingPerfIndex(String perfObject) {
        int index = lookupPerfIndexByEnglishName(perfObject);
        if (index == 0) {
            LOG.debug("Unable to find English counter index for {}", perfObject);
            return perfObject;
        }
        String localized = lookupPerfNameByIndex(index);
        if (localized.isEmpty()) {
            return perfObject;
        }
        LOG.debug("Localized {} to {}", perfObject, localized);
        return localized;
    }

    private static int lookupPerfIndexByEnglishName(String name) {
        String[] counters = Advapi32UtilFFM.registryGetStringArray(
                MemorySegment.ofAddress(WinRegFFM.HKEY_LOCAL_MACHINE), ENGLISH_COUNTER_KEY, ENGLISH_COUNTER_VALUE);
        // Array contains alternating index/name pairs: {"1", "1847", "2", "System", "4", "Memory", ...}
        for (int i = 1; i < counters.length; i += 2) {
            if (counters[i].equals(name)) {
                int idx = ParseUtil.parseIntOrDefault(counters[i - 1], 0);
                if (idx > 0) {
                    return idx;
                }
            }
        }
        return 0;
    }

    private static String lookupPerfNameByIndex(int index) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment bufSize = arena.allocate(JAVA_INT);
            bufSize.set(JAVA_INT, 0, 0);
            int result = PdhLookupPerfNameByIndexW(MemorySegment.NULL, index, MemorySegment.NULL, bufSize);
            if (result != PDH_MORE_DATA && result != ERROR_SUCCESS) {
                return "";
            }
            int size = bufSize.get(JAVA_INT, 0);
            if (size <= 0) {
                return "";
            }
            MemorySegment nameBuf = arena.allocate((long) size * 2);
            result = PdhLookupPerfNameByIndexW(MemorySegment.NULL, index, nameBuf, bufSize);
            if (result != ERROR_SUCCESS) {
                return "";
            }
            return readWideString(nameBuf);
        } catch (Throwable t) {
            LOG.debug("Failed to lookup perf name by index {}: {}", index, t.getMessage());
            return "";
        }
    }
}
