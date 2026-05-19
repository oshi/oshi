/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.perfmon;

import static oshi.driver.common.windows.perfmon.PerfmonConstants.NOT_TOTAL_INSTANCE;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.PHYSICAL_DISK;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.WIN32_PERF_RAW_DATA_PERF_DISK_PHYSICAL_DISK_WHERE_NAME_NOT_TOTAL;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.tuples.Pair;

/**
 * Physical Disk performance counter enums
 */
@ThreadSafe
public final class PhysicalDisk {

    /**
     * Physical Disk performance counters.
     */
    public enum PhysicalDiskProperty implements PdhCounterWildcardProperty {
        /** Instance filter (not _Total). */
        NAME(NOT_TOTAL_INSTANCE),
        /** Disk reads per second. */
        DISKREADSPERSEC("Disk Reads/sec"),
        /** Disk read bytes per second. */
        DISKREADBYTESPERSEC("Disk Read Bytes/sec"),
        /** Disk writes per second. */
        DISKWRITESPERSEC("Disk Writes/sec"),
        /** Disk write bytes per second. */
        DISKWRITEBYTESPERSEC("Disk Write Bytes/sec"),
        /** Current disk queue length. */
        CURRENTDISKQUEUELENGTH("Current Disk Queue Length"),
        /** Percent disk time. */
        PERCENTDISKTIME("% Disk Time");

        private final String counter;

        PhysicalDiskProperty(String counter) {
            this.counter = counter;
        }

        @Override
        public String getCounter() {
            return counter;
        }
    }

    private PhysicalDisk() {
    }

    /**
     * Returns physical disk performance counters.
     *
     * @param executor the performance counter query executor
     * @return Performance Counters for physical disks, or empty if disk counters are disabled.
     */
    public static Pair<List<String>, Map<PhysicalDiskProperty, List<Long>>> queryDiskCounters(
            PerfCounterQueryExecutor executor) {
        if (executor.isPerfDiskDisabled()) {
            return new Pair<>(Collections.emptyList(), Collections.emptyMap());
        }
        return executor.queryInstancesAndValues(PhysicalDiskProperty.class, PHYSICAL_DISK,
                WIN32_PERF_RAW_DATA_PERF_DISK_PHYSICAL_DISK_WHERE_NAME_NOT_TOTAL);
    }
}
