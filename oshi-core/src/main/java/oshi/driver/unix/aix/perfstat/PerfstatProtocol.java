/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.unix.aix.perfstat;

import com.sun.jna.platform.unix.aix.Perfstat;
import com.sun.jna.platform.unix.aix.Perfstat.perfstat_id_t;
import com.sun.jna.platform.unix.aix.Perfstat.perfstat_protocol_t;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Utility to query performance stats for network interfaces
 */
@ThreadSafe
public final class PerfstatProtocol {

    private static final Perfstat PERF = Perfstat.INSTANCE;

    private PerfstatProtocol() {
    }

    /**
     * Queries perfstat_protocol for per-protocol usage statistics
     *
     * @return an array of usage statistics
     */
    public static perfstat_protocol_t[] queryProtocols() {
        perfstat_protocol_t protocol = new perfstat_protocol_t();
        // With null, null, ..., 0, returns total # of elements
        int total = PERF.perfstat_protocol(null, null, protocol.size(), 0);
        if (total > 0) {
            perfstat_protocol_t[] statp = (perfstat_protocol_t[]) protocol.toArray(total);
            perfstat_id_t firstprotocol = new perfstat_id_t(); // name is ""
            int ret = PERF.perfstat_protocol(firstprotocol, statp, protocol.size(), total);
            if (ret > 0) {
                return statp;
            }
        }
        return new perfstat_protocol_t[0];
    }
}
