/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.perfmon;

import static oshi.driver.common.windows.perfmon.PerfmonConstants.NOT_TOTAL_INSTANCE;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Physical Disk performance counter enums
 */
@ThreadSafe
public final class PhysicalDisk {

    /**
     * Physical Disk performance counters.
     */
    public enum PhysicalDiskProperty implements PdhCounterWildcardProperty {
        // First element defines WMI instance name field and PDH instance filter
        NAME(NOT_TOTAL_INSTANCE),
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
}
