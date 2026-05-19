/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.perfmon;

import java.util.List;
import java.util.Map;

import oshi.driver.common.windows.perfmon.PdhCounterProperty;
import oshi.driver.common.windows.perfmon.PdhCounterWildcardProperty;
import oshi.driver.common.windows.perfmon.PerfCounterQueryExecutor;
import oshi.ffm.util.platform.windows.PerfCounterQueryFFM;
import oshi.ffm.util.platform.windows.PerfCounterWildcardQueryFFM;
import oshi.ffm.windows.VersionHelpersFFM;
import oshi.util.tuples.Pair;

/**
 * FFM-based {@link PerfCounterQueryExecutor} implementation.
 */
public final class PerfCounterQueryExecutorFFM implements PerfCounterQueryExecutor {

    /** Singleton instance. */
    public static final PerfCounterQueryExecutorFFM INSTANCE = new PerfCounterQueryExecutorFFM();

    private static final boolean IS_WIN7_OR_GREATER = VersionHelpersFFM.IsWindows7OrGreater();

    private PerfCounterQueryExecutorFFM() {
    }

    @Override
    public <T extends Enum<T> & PdhCounterProperty> Map<T, Long> queryValues(Class<T> propertyEnum, String perfObject,
            String perfWmiClass) {
        return PerfCounterQueryFFM.queryValues(propertyEnum, perfObject, perfWmiClass);
    }

    @Override
    public <T extends Enum<T> & PdhCounterWildcardProperty> Pair<List<String>, Map<T, List<Long>>> queryInstancesAndValues(
            Class<T> propertyEnum, String perfObject, String perfWmiClass) {
        return PerfCounterWildcardQueryFFM.queryInstancesAndValues(propertyEnum, perfObject, perfWmiClass);
    }

    @Override
    public <T extends Enum<T> & PdhCounterWildcardProperty> Pair<List<String>, Map<T, List<Long>>> queryInstancesAndValues(
            Class<T> propertyEnum, String perfObject, String perfWmiClass, String customFilter) {
        return PerfCounterWildcardQueryFFM.queryInstancesAndValues(propertyEnum, perfObject, perfWmiClass,
                customFilter);
    }

    @Override
    public <T extends Enum<T> & PdhCounterWildcardProperty> Pair<List<String>, Map<T, List<Long>>> queryInstancesAndValuesFromWMI(
            Class<T> propertyEnum, String perfWmiClass) {
        return PerfCounterWildcardQueryFFM.queryInstancesAndValuesFromWMI(propertyEnum, perfWmiClass);
    }

    @Override
    public <T extends Enum<T> & PdhCounterWildcardProperty> Pair<List<String>, Map<T, List<Long>>> queryInstancesAndValuesFromPDH(
            Class<T> propertyEnum, String perfObject) {
        return PerfCounterWildcardQueryFFM.queryInstancesAndValuesFromPDH(propertyEnum, perfObject);
    }

    @Override
    public boolean isPerfOsDisabled() {
        return PerfmonDisabledFFM.PERF_OS_DISABLED;
    }

    @Override
    public boolean isPerfProcDisabled() {
        return PerfmonDisabledFFM.PERF_PROC_DISABLED;
    }

    @Override
    public boolean isPerfDiskDisabled() {
        return PerfmonDisabledFFM.PERF_DISK_DISABLED;
    }

    @Override
    public boolean isWin7OrGreater() {
        return IS_WIN7_OR_GREATER;
    }
}
