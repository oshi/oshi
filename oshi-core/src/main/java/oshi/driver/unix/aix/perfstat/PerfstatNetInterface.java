/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.unix.aix.perfstat;

import com.sun.jna.platform.unix.aix.Perfstat;
import com.sun.jna.platform.unix.aix.Perfstat.perfstat_id_t;
import com.sun.jna.platform.unix.aix.Perfstat.perfstat_netinterface_t;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Utility to query performance stats for network interfaces
 */
@ThreadSafe
public final class PerfstatNetInterface {

    private static final Perfstat PERF = Perfstat.INSTANCE;

    private PerfstatNetInterface() {
    }

    /**
     * Queries perfstat_netinterface for per-netinterface usage statistics
     *
     * @return an array of usage statistics
     */
    public static perfstat_netinterface_t[] queryNetInterfaces() {
        perfstat_netinterface_t netinterface = new perfstat_netinterface_t();
        // With null, null, ..., 0, returns total # of elements
        int total = PERF.perfstat_netinterface(null, null, netinterface.size(), 0);
        if (total > 0) {
            perfstat_netinterface_t[] statp = (perfstat_netinterface_t[]) netinterface.toArray(total);
            perfstat_id_t firstnetinterface = new perfstat_id_t(); // name is ""
            int ret = PERF.perfstat_netinterface(firstnetinterface, statp, netinterface.size(), total);
            if (ret > 0) {
                return statp;
            }
        }
        return new perfstat_netinterface_t[0];
    }
}
