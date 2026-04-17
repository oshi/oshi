/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.windows;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.PdhCounterWildcardProperty;
import oshi.ffm.windows.com.IWbemClassObjectFFM;
import oshi.util.tuples.Pair;

/**
 * Enables queries of Performance Counters using wild cards to filter instances. FFM-based equivalent of
 * {@code PerfCounterWildcardQuery}.
 */
@ThreadSafe
public final class PerfCounterWildcardQueryFFM {

    private static final Logger LOG = LoggerFactory.getLogger(PerfCounterWildcardQueryFFM.class);

    // Use a thread safe set to cache failed pdh queries
    private static final Set<String> FAILED_QUERY_CACHE = ConcurrentHashMap.newKeySet();

    private PerfCounterWildcardQueryFFM() {
    }

    /**
     * Query Performance Counters using PDH, with WMI backup on failure, for values corresponding to the property enum.
     *
     * @param <T>          The enum type of {@code propertyEnum}
     * @param propertyEnum An enum which implements {@link PdhCounterWildcardProperty} and contains the WMI field (Enum
     *                     value) and PDH Counter string (instance and counter)
     * @param perfObject   The PDH object for this counter; all counters on this object will be refreshed at the same
     *                     time
     * @param perfWmiClass The WMI PerfData_RawData_* class corresponding to the PDH object, optionally including a
     *                     WHERE clause
     * @return A pair containing a list of instances and an {@link EnumMap} of the corresponding values indexed by
     *         {@code propertyEnum} on success, or an empty list and empty map if both PDH and WMI queries failed.
     */
    public static <T extends Enum<T> & PdhCounterWildcardProperty> Pair<List<String>, Map<T, List<Long>>> queryInstancesAndValues(
            Class<T> propertyEnum, String perfObject, String perfWmiClass) {
        if (!FAILED_QUERY_CACHE.contains(perfObject)) {
            Pair<List<String>, Map<T, List<Long>>> result = queryInstancesAndValuesFromPDH(propertyEnum, perfObject);
            if (!result.getA().isEmpty()) {
                return result;
            }
            LOG.info("Disabling further attempts to query {}.", perfObject);
            FAILED_QUERY_CACHE.add(perfObject);
        }
        return queryInstancesAndValuesFromWMI(propertyEnum, perfWmiClass);
    }

    /**
     * Query Performance Counters using PDH for values corresponding to the property enum.
     *
     * @param <T>          The enum type of {@code propertyEnum}
     * @param propertyEnum An enum which implements {@link PdhCounterWildcardProperty} and contains the WMI field (Enum
     *                     value) and PDH Counter string (instance and counter)
     * @param perfObject   The PDH object for this counter; all counters on this object will be refreshed at the same
     *                     time
     * @return A pair containing a list of instances and an {@link EnumMap} of the corresponding values indexed by
     *         {@code propertyEnum} on success, or an empty list and empty map if the PDH query failed.
     */
    public static <T extends Enum<T> & PdhCounterWildcardProperty> Pair<List<String>, Map<T, List<Long>>> queryInstancesAndValuesFromPDH(
            Class<T> propertyEnum, String perfObject) {
        return PerfDataUtilFFM.queryWildcardCounters(propertyEnum, perfObject);
    }

    /**
     * Query Performance Counters using WMI for values corresponding to the property enum.
     *
     * @param <T>          The enum type of {@code propertyEnum}
     * @param propertyEnum An enum which implements {@link PdhCounterWildcardProperty} and contains the WMI field (Enum
     *                     value) and PDH Counter string (instance and counter)
     * @param perfWmiClass The WMI PerfData_RawData_* class corresponding to the PDH object, optionally including a
     *                     WHERE clause
     * @return A pair containing a list of instances and an {@link EnumMap} of the corresponding values indexed by
     *         {@code propertyEnum} on success, or an empty list and empty map if the WMI query failed.
     */
    public static <T extends Enum<T> & PdhCounterWildcardProperty> Pair<List<String>, Map<T, List<Long>>> queryInstancesAndValuesFromWMI(
            Class<T> propertyEnum, String perfWmiClass) {
        T[] props = propertyEnum.getEnumConstants();
        List<String> instances = new ArrayList<>();
        EnumMap<T, List<Long>> valuesMap = new EnumMap<>(propertyEnum);

        List<Map<T, Object>> rows = WmiQueryHandlerFFM.createInstance().queryWMI(perfWmiClass, null,
                () -> new EnumMap<T, Object>(propertyEnum), (pObject, arena, row) -> {
                    for (T prop : props) {
                        if (prop.ordinal() == 0) {
                            row.put(prop, IWbemClassObjectFFM.getString(pObject, prop.name(), arena));
                        } else {
                            row.put(prop, IWbemClassObjectFFM.getLong(pObject, prop.name(), arena));
                        }
                    }
                });

        for (Map<T, Object> row : rows) {
            instances.add((String) row.get(props[0]));
            for (int i = 1; i < props.length; i++) {
                valuesMap.computeIfAbsent(props[i], k -> new ArrayList<>()).add((Long) row.get(props[i]));
            }
        }
        return new Pair<>(instances, valuesMap);
    }
}
