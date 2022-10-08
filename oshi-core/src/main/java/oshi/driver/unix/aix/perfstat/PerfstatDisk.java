/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.unix.aix.perfstat;

import com.sun.jna.platform.unix.aix.Perfstat;
import com.sun.jna.platform.unix.aix.Perfstat.perfstat_disk_t;
import com.sun.jna.platform.unix.aix.Perfstat.perfstat_id_t;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Utility to query performance stats for disk_stats
 */
@ThreadSafe
public final class PerfstatDisk {

    private static final Perfstat PERF = Perfstat.INSTANCE;

    private PerfstatDisk() {
    }

    /**
     * Queries perfstat_disk for per-disk usage statistics
     *
     * @return an array of usage statistics
     */
    public static perfstat_disk_t[] queryDiskStats() {
        perfstat_disk_t diskStats = new perfstat_disk_t();
        // With null, null, ..., 0, returns total # of elements
        int total = PERF.perfstat_disk(null, null, diskStats.size(), 0);
        if (total > 0) {
            perfstat_disk_t[] statp = (perfstat_disk_t[]) diskStats.toArray(total);
            perfstat_id_t firstdiskStats = new perfstat_id_t(); // name is ""
            int ret = PERF.perfstat_disk(firstdiskStats, statp, diskStats.size(), total);
            if (ret > 0) {
                return statp;
            }
        }
        return new perfstat_disk_t[0];
    }
}
