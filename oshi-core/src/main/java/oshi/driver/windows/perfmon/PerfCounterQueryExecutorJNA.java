/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.perfmon;

import java.util.List;
import java.util.Map;

import com.sun.jna.platform.win32.VersionHelpers;

import oshi.driver.common.windows.perfmon.PdhCounterProperty;
import oshi.driver.common.windows.perfmon.PdhCounterWildcardProperty;
import oshi.driver.common.windows.perfmon.PerfCounterQueryExecutor;
import oshi.util.platform.windows.PerfCounterQuery;
import oshi.util.platform.windows.PerfCounterWildcardQuery;
import oshi.util.tuples.Pair;

/**
 * JNA-based {@link PerfCounterQueryExecutor} implementation.
 */
public final class PerfCounterQueryExecutorJNA implements PerfCounterQueryExecutor {

    /** Singleton instance. */
    public static final PerfCounterQueryExecutorJNA INSTANCE = new PerfCounterQueryExecutorJNA();

    private static final boolean IS_WIN7_OR_GREATER = VersionHelpers.IsWindows7OrGreater();

    private PerfCounterQueryExecutorJNA() {
    }

    @Override
    public <T extends Enum<T> & PdhCounterProperty> Map<T, Long> queryValues(Class<T> propertyEnum, String perfObject,
            String perfWmiClass) {
        return PerfCounterQuery.queryValues(propertyEnum, perfObject, perfWmiClass);
    }

    @Override
    public <T extends Enum<T> & PdhCounterWildcardProperty> Pair<List<String>, Map<T, List<Long>>> queryInstancesAndValues(
            Class<T> propertyEnum, String perfObject, String perfWmiClass) {
        return PerfCounterWildcardQuery.queryInstancesAndValues(propertyEnum, perfObject, perfWmiClass);
    }

    @Override
    public <T extends Enum<T> & PdhCounterWildcardProperty> Pair<List<String>, Map<T, List<Long>>> queryInstancesAndValues(
            Class<T> propertyEnum, String perfObject, String perfWmiClass, String customFilter) {
        return PerfCounterWildcardQuery.queryInstancesAndValues(propertyEnum, perfObject, perfWmiClass, customFilter);
    }

    @Override
    public <T extends Enum<T> & PdhCounterWildcardProperty> Pair<List<String>, Map<T, List<Long>>> queryInstancesAndValuesFromWMI(
            Class<T> propertyEnum, String perfWmiClass) {
        return PerfCounterWildcardQuery.queryInstancesAndValuesFromWMI(propertyEnum, perfWmiClass);
    }

    @Override
    public <T extends Enum<T> & PdhCounterWildcardProperty> Pair<List<String>, Map<T, List<Long>>> queryInstancesAndValuesFromPDH(
            Class<T> propertyEnum, String perfObject) {
        return PerfCounterWildcardQuery.queryInstancesAndValuesFromPDH(propertyEnum, perfObject);
    }

    @Override
    public boolean isPerfOsDisabled() {
        return PerfmonDisabled.PERF_OS_DISABLED;
    }

    @Override
    public boolean isPerfProcDisabled() {
        return PerfmonDisabled.PERF_PROC_DISABLED;
    }

    @Override
    public boolean isPerfDiskDisabled() {
        return PerfmonDisabled.PERF_DISK_DISABLED;
    }

    @Override
    public boolean isWin7OrGreater() {
        return IS_WIN7_OR_GREATER;
    }
}
