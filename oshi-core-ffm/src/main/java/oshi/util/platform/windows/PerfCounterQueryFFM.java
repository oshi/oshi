/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.windows;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.PdhCounterProperty;
import oshi.ffm.windows.com.IWbemClassObjectFFM;

/**
 * Enables queries of Performance Counters using PDH with WMI backup, for enums implementing {@link PdhCounterProperty}.
 * FFM-based equivalent of {@code PerfCounterQuery}.
 */
@ThreadSafe
public final class PerfCounterQueryFFM {

    private static final Logger LOG = LoggerFactory.getLogger(PerfCounterQueryFFM.class);

    // Use a thread safe set to cache failed pdh queries
    private static final Set<String> FAILED_QUERY_CACHE = ConcurrentHashMap.newKeySet();

    private PerfCounterQueryFFM() {
    }

    /**
     * Query Performance Counters using PDH, with WMI backup on failure, for values corresponding to the property enum.
     *
     * @param <T>          The enum type of {@code propertyEnum}
     * @param propertyEnum An enum which implements {@link PdhCounterProperty} and contains the WMI field (Enum value)
     *                     and PDH Counter string (instance and counter)
     * @param perfObject   The PDH object for this counter; all counters on this object will be refreshed at the same
     *                     time
     * @param perfWmiClass The WMI PerfData_RawData_* class corresponding to the PDH object, optionally including a
     *                     WHERE clause
     * @return An {@link EnumMap} of the values indexed by {@code propertyEnum} on success, or an empty map if both PDH
     *         and WMI queries failed.
     */
    public static <T extends Enum<T> & PdhCounterProperty> Map<T, Long> queryValues(Class<T> propertyEnum,
            String perfObject, String perfWmiClass) {
        if (!FAILED_QUERY_CACHE.contains(perfObject)) {
            Map<T, Long> valueMap = queryValuesFromPDH(propertyEnum, perfObject);
            if (!valueMap.isEmpty()) {
                return valueMap;
            }
            LOG.info("Disabling further attempts to query {}.", perfObject);
            FAILED_QUERY_CACHE.add(perfObject);
        }
        return queryValuesFromWMI(propertyEnum, perfWmiClass);
    }

    /**
     * Query Performance Counters using PDH for values corresponding to the property enum.
     *
     * @param <T>          The enum type of {@code propertyEnum}
     * @param propertyEnum An enum which implements {@link PdhCounterProperty} and contains the WMI field (Enum value)
     *                     and PDH Counter string (instance and counter)
     * @param perfObject   The PDH object for this counter; all counters on this object will be refreshed at the same
     *                     time
     * @return An {@link EnumMap} of the values indexed by {@code propertyEnum} on success, or an empty map if the PDH
     *         query failed.
     */
    public static <T extends Enum<T> & PdhCounterProperty> Map<T, Long> queryValuesFromPDH(Class<T> propertyEnum,
            String perfObject) {
        return PerfDataUtilFFM.queryCounters(propertyEnum, perfObject);
    }

    /**
     * Query Performance Counters using WMI for values corresponding to the property enum.
     *
     * @param <T>          The enum type of {@code propertyEnum}
     * @param propertyEnum An enum which implements {@link PdhCounterProperty} and contains the WMI field (Enum value)
     *                     and PDH Counter string (instance and counter)
     * @param perfWmiClass The WMI PerfData_RawData_* class corresponding to the PDH object, optionally including a
     *                     WHERE clause
     * @return An {@link EnumMap} of the values indexed by {@code propertyEnum} if successful, an empty map if the WMI
     *         query failed.
     */
    public static <T extends Enum<T> & PdhCounterProperty> Map<T, Long> queryValuesFromWMI(Class<T> propertyEnum,
            String perfWmiClass) {
        T[] props = propertyEnum.getEnumConstants();
        EnumMap<T, Long> valueMap = new EnumMap<>(propertyEnum);

        List<Map<T, Long>> rows = WmiQueryHandlerFFM.createInstance().queryWMI(perfWmiClass, null,
                () -> new EnumMap<T, Long>(propertyEnum), (pObject, arena, row) -> {
                    for (T prop : props) {
                        row.put(prop, IWbemClassObjectFFM.getLong(pObject, prop.name(), arena));
                    }
                });

        if (!rows.isEmpty()) {
            valueMap.putAll(rows.getFirst());
        }
        return valueMap;
    }
}
