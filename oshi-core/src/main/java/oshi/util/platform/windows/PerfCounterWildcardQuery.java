/*
 * Copyright 2019-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.windows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.PdhUtil;
import com.sun.jna.platform.win32.PdhUtil.PdhEnumObjectItems;
import com.sun.jna.platform.win32.PdhUtil.PdhException;
import com.sun.jna.platform.win32.COM.Wbemcli;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.Util;
import oshi.util.platform.windows.PerfDataUtil.PerfCounter;
import oshi.util.tuples.Pair;

/**
 * Enables queries of Performance Counters using wild cards to filter instances
 */
@ThreadSafe
public final class PerfCounterWildcardQuery {

    private static final Logger LOG = LoggerFactory.getLogger(PerfCounterWildcardQuery.class);

    // Use a thread safe set to cache failed pdh queries
    private static final Set<String> FAILED_QUERY_CACHE = ConcurrentHashMap.newKeySet();

    private PerfCounterWildcardQuery() {
    }

    /**
     * Query the a Performance Counter using PDH, with WMI backup on failure, for values corresponding to the property
     * enum.
     *
     * @param <T>          The enum type of {@code propertyEnum}
     * @param propertyEnum An enum which implements
     *                     {@link oshi.util.platform.windows.PerfCounterQuery.PdhCounterProperty} and contains the WMI
     *                     field (Enum value) and PDH Counter string (instance and counter)
     * @param perfObject   The PDH object for this counter; all counters on this object will be refreshed at the same
     *                     time
     * @param perfWmiClass The WMI PerfData_RawData_* class corresponding to the PDH object
     * @return An pair containing a list of instances and an {@link EnumMap} of the corresponding values indexed by
     *         {@code propertyEnum} on success, or an empty list and empty map if both PDH and WMI queries failed.
     */
    public static <T extends Enum<T>> Pair<List<String>, Map<T, List<Long>>> queryInstancesAndValues(
            Class<T> propertyEnum, String perfObject, String perfWmiClass) {
        if (!FAILED_QUERY_CACHE.contains(perfObject)) {
            Pair<List<String>, Map<T, List<Long>>> instancesAndValuesMap = queryInstancesAndValuesFromPDH(propertyEnum,
                    perfObject);
            if (!instancesAndValuesMap.getA().isEmpty()) {
                return instancesAndValuesMap;
            }
            // If we are here, query failed
            LOG.warn("Disabling further attempts to query {}.", perfObject);
            FAILED_QUERY_CACHE.add(perfObject);
        }
        return queryInstancesAndValuesFromWMI(propertyEnum, perfWmiClass);
    }

    /**
     * Query the a Performance Counter using PDH for values corresponding to the property enum.
     *
     * @param <T>          The enum type of {@code propertyEnum}
     * @param propertyEnum An enum which implements
     *                     {@link oshi.util.platform.windows.PerfCounterQuery.PdhCounterProperty} and contains the WMI
     *                     field (Enum value) and PDH Counter string (instance and counter)
     * @param perfObject   The PDH object for this counter; all counters on this object will be refreshed at the same
     *                     time
     * @return An pair containing a list of instances and an {@link EnumMap} of the corresponding values indexed by
     *         {@code propertyEnum} on success, or an empty list and empty map if the PDH query failed.
     */
    public static <T extends Enum<T>> Pair<List<String>, Map<T, List<Long>>> queryInstancesAndValuesFromPDH(
            Class<T> propertyEnum, String perfObject) {
        T[] props = propertyEnum.getEnumConstants();
        if (props.length < 2) {
            throw new IllegalArgumentException("Enum " + propertyEnum.getName()
                    + " must have at least two elements, an instance filter and a counter.");
        }
        String instanceFilter = ((PdhCounterWildcardProperty) propertyEnum.getEnumConstants()[0]).getCounter()
                .toLowerCase();
        // Localize the perfObject using different variable for the EnumObjectItems
        // Will still use unlocalized perfObject for the query
        String perfObjectLocalized = PerfCounterQuery.localizeIfNeeded(perfObject, true);

        // Get list of instances
        PdhEnumObjectItems objectItems = null;
        try {
            objectItems = PdhUtil.PdhEnumObjectItems(null, null, perfObjectLocalized, 100);
        } catch (PdhException e) {
            LOG.warn(
                    "Failed to locate performance object for {} in the registry. Performance counters may be corrupt. {}",
                    perfObjectLocalized, e.getMessage());
        }
        if (objectItems == null) {
            return new Pair<>(Collections.emptyList(), Collections.emptyMap());
        }
        List<String> instances = objectItems.getInstances();
        // Filter out instances not matching filter
        instances.removeIf(i -> !Util.wildcardMatch(i.toLowerCase(), instanceFilter));
        EnumMap<T, List<Long>> valuesMap = new EnumMap<>(propertyEnum);
        try (PerfCounterQueryHandler pdhQueryHandler = new PerfCounterQueryHandler()) {
            // Set up the query and counter handles
            EnumMap<T, List<PerfCounter>> counterListMap = new EnumMap<>(propertyEnum);
            // Start at 1, first counter defines instance filter
            for (int i = 1; i < props.length; i++) {
                T prop = props[i];
                List<PerfCounter> counterList = new ArrayList<>(instances.size());
                for (String instance : instances) {
                    PerfCounter counter = PerfDataUtil.createCounter(perfObject, instance,
                            ((PdhCounterWildcardProperty) prop).getCounter());
                    if (!pdhQueryHandler.addCounterToQuery(counter)) {
                        return new Pair<>(Collections.emptyList(), Collections.emptyMap());
                    }
                    counterList.add(counter);
                }
                counterListMap.put(prop, counterList);
            }
            // And then query. Zero timestamp means update failed
            if (0 < pdhQueryHandler.updateQuery()) {
                // Start at 1, first counter defines instance filter
                for (int i = 1; i < props.length; i++) {
                    T prop = props[i];
                    List<Long> values = new ArrayList<>();
                    for (PerfCounter counter : counterListMap.get(prop)) {
                        values.add(pdhQueryHandler.queryCounter(counter));
                    }
                    valuesMap.put(prop, values);
                }
            }
        }
        return new Pair<>(instances, valuesMap);
    }

    /**
     * Query the a Performance Counter using WMI for values corresponding to the property enum.
     *
     * @param <T>          The enum type of {@code propertyEnum}
     * @param propertyEnum An enum which implements
     *                     {@link oshi.util.platform.windows.PerfCounterQuery.PdhCounterProperty} and contains the WMI
     *                     field (Enum value) and PDH Counter string (instance and counter)
     * @param wmiClass     The WMI PerfData_RawData_* class corresponding to the PDH object
     * @return An pair containing a list of instances and an {@link EnumMap} of the corresponding values indexed by
     *         {@code propertyEnum} on success, or an empty list and empty map if the WMI query failed.
     */
    public static <T extends Enum<T>> Pair<List<String>, Map<T, List<Long>>> queryInstancesAndValuesFromWMI(
            Class<T> propertyEnum, String wmiClass) {
        List<String> instances = new ArrayList<>();
        EnumMap<T, List<Long>> valuesMap = new EnumMap<>(propertyEnum);
        WmiQuery<T> query = new WmiQuery<>(wmiClass, propertyEnum);
        WmiResult<T> result = Objects.requireNonNull(WmiQueryHandler.createInstance()).queryWMI(query);
        if (result.getResultCount() > 0) {
            for (T prop : propertyEnum.getEnumConstants()) {
                // First element is instance name
                if (prop.ordinal() == 0) {
                    for (int i = 0; i < result.getResultCount(); i++) {
                        instances.add(WmiUtil.getString(result, prop, i));
                    }
                } else {
                    List<Long> values = new ArrayList<>();
                    for (int i = 0; i < result.getResultCount(); i++) {
                        switch (result.getCIMType(prop)) {
                        case Wbemcli.CIM_UINT16:
                            values.add((long) WmiUtil.getUint16(result, prop, i));
                            break;
                        case Wbemcli.CIM_UINT32:
                            values.add(WmiUtil.getUint32asLong(result, prop, i));
                            break;
                        case Wbemcli.CIM_UINT64:
                            values.add(WmiUtil.getUint64(result, prop, i));
                            break;
                        case Wbemcli.CIM_DATETIME:
                            values.add(WmiUtil.getDateTime(result, prop, i).toInstant().toEpochMilli());
                            break;
                        default:
                            throw new ClassCastException("Unimplemented CIM Type Mapping.");
                        }
                    }
                    valuesMap.put(prop, values);
                }
            }
        }
        return new Pair<>(instances, valuesMap);
    }

    /**
     * Contract for Counter Property Enums
     */
    public interface PdhCounterWildcardProperty {
        /**
         * @return Returns the counter. The first element of the enum will return the instance filter rather than a
         *         counter.
         */
        String getCounter();
    }

}
