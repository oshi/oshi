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
}
