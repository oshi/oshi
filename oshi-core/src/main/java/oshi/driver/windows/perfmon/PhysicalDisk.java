/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.perfmon;

import static oshi.driver.windows.perfmon.PerfmonConstants.PHYSICAL_DISK;
import static oshi.driver.windows.perfmon.PerfmonConstants.WIN32_PERF_RAW_DATA_PERF_DISK_PHYSICAL_DISK_WHERE_NAME_NOT_TOTAL;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.platform.windows.PerfCounterQuery;
import oshi.util.platform.windows.PerfCounterWildcardQuery;
import oshi.util.platform.windows.PerfCounterWildcardQuery.PdhCounterWildcardProperty;
import oshi.util.tuples.Pair;

/**
 * Utility to query PhysicalDisk performance counter
 */
@ThreadSafe
public final class PhysicalDisk {

    /**
     * Physical Disk performance counters.
     */
    public enum PhysicalDiskProperty implements PdhCounterWildcardProperty {
        // First element defines WMI instance name field and PDH instance filter
        NAME(PerfCounterQuery.NOT_TOTAL_INSTANCE),
        // Remaining elements define counters
        DISKREADSPERSEC("Disk Reads/sec"), //
        DISKREADBYTESPERSEC("Disk Read Bytes/sec"), //
        DISKWRITESPERSEC("Disk Writes/sec"), //
        DISKWRITEBYTESPERSEC("Disk Write Bytes/sec"), //
        CURRENTDISKQUEUELENGTH("Current Disk Queue Length"), //
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
     * @return Performance Counters for physical disks.
     */
    public static Pair<List<String>, Map<PhysicalDiskProperty, List<Long>>> queryDiskCounters() {
        if (PerfmonDisabled.PERF_DISK_DISABLED) {
            return new Pair<>(Collections.emptyList(), Collections.emptyMap());
        }
        return PerfCounterWildcardQuery.queryInstancesAndValues(PhysicalDiskProperty.class, PHYSICAL_DISK,
                WIN32_PERF_RAW_DATA_PERF_DISK_PHYSICAL_DISK_WHERE_NAME_NOT_TOTAL);
    }
}
