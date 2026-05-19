/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.perfmon;

import java.util.List;
import java.util.Map;

import oshi.util.tuples.Pair;

/**
 * Common interface for querying Windows performance counters, abstracting JNA and FFM implementations.
 * <p>
 * Implementations perform raw queries and do not check whether counters are disabled. Callers should use
 * {@link #isPerfOsDisabled()}, {@link #isPerfProcDisabled()}, and {@link #isPerfDiskDisabled()} to gate queries and
 * return empty results when the relevant counter category is disabled.
 */
public interface PerfCounterQueryExecutor {

    /**
     * Queries performance counter values for a fixed set of counters.
     *
     * @param <T>          the enum type implementing {@link PdhCounterProperty}
     * @param propertyEnum the property enum class
     * @param perfObject   the performance object name
     * @param perfWmiClass the WMI class name for fallback queries
     * @return a map of enum constants to their counter values, or an empty map if disabled
     */
    <T extends Enum<T> & PdhCounterProperty> Map<T, Long> queryValues(Class<T> propertyEnum, String perfObject,
            String perfWmiClass);

    /**
     * Queries performance counter values for wildcard (multi-instance) counters.
     *
     * @param <T>          the enum type implementing {@link PdhCounterWildcardProperty}
     * @param propertyEnum the property enum class
     * @param perfObject   the performance object name
     * @param perfWmiClass the WMI class name for fallback queries
     * @return a pair of instance names and a map of enum constants to lists of values, or empty if disabled
     */
    <T extends Enum<T> & PdhCounterWildcardProperty> Pair<List<String>, Map<T, List<Long>>> queryInstancesAndValues(
            Class<T> propertyEnum, String perfObject, String perfWmiClass);

    /**
     * Queries performance counter values for wildcard counters with a custom instance filter.
     *
     * @param <T>          the enum type implementing {@link PdhCounterWildcardProperty}
     * @param propertyEnum the property enum class
     * @param perfObject   the performance object name
     * @param perfWmiClass the WMI class name for fallback queries
     * @param customFilter custom PDH instance filter
     * @return a pair of instance names and a map of enum constants to lists of values, or empty if disabled
     */
    <T extends Enum<T> & PdhCounterWildcardProperty> Pair<List<String>, Map<T, List<Long>>> queryInstancesAndValues(
            Class<T> propertyEnum, String perfObject, String perfWmiClass, String customFilter);

    /**
     * Queries performance counter values from WMI directly (formatted/cooked data).
     *
     * @param <T>          the enum type implementing {@link PdhCounterWildcardProperty}
     * @param propertyEnum the property enum class
     * @param perfWmiClass the WMI class name
     * @return a pair of instance names and a map of enum constants to lists of values, or empty if disabled
     */
    <T extends Enum<T> & PdhCounterWildcardProperty> Pair<List<String>, Map<T, List<Long>>> queryInstancesAndValuesFromWMI(
            Class<T> propertyEnum, String perfWmiClass);

    /**
     * Queries performance counter values from PDH only (no WMI fallback).
     *
     * @param <T>          the enum type implementing {@link PdhCounterWildcardProperty}
     * @param propertyEnum the property enum class
     * @param perfObject   the performance object name
     * @return a pair of instance names and a map of enum constants to lists of values
     */
    <T extends Enum<T> & PdhCounterWildcardProperty> Pair<List<String>, Map<T, List<Long>>> queryInstancesAndValuesFromPDH(
            Class<T> propertyEnum, String perfObject);

    /**
     * Returns whether the OS performance counters are disabled.
     *
     * @return true if PerfOS counters are disabled
     */
    boolean isPerfOsDisabled();

    /**
     * Returns whether the process performance counters are disabled.
     *
     * @return true if PerfProc counters are disabled
     */
    boolean isPerfProcDisabled();

    /**
     * Returns whether the disk performance counters are disabled.
     *
     * @return true if PerfDisk counters are disabled
     */
    boolean isPerfDiskDisabled();

    /**
     * Returns whether the OS is Windows 7 or greater.
     *
     * @return true if Windows 7+
     */
    boolean isWin7OrGreater();
}
