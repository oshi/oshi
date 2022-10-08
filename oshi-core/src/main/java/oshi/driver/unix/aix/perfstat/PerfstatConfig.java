/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.unix.aix.perfstat;

import com.sun.jna.platform.unix.aix.Perfstat;
import com.sun.jna.platform.unix.aix.Perfstat.perfstat_partition_config_t;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Utility to query partition config
 */
@ThreadSafe
public final class PerfstatConfig {

    private static final Perfstat PERF = Perfstat.INSTANCE;

    private PerfstatConfig() {
    }

    /**
     * Queries perfstat_partition_config for config
     *
     * @return usage statistics
     */
    public static perfstat_partition_config_t queryConfig() {
        perfstat_partition_config_t config = new perfstat_partition_config_t();
        int ret = PERF.perfstat_partition_config(null, config, config.size(), 1);
        if (ret > 0) {
            return config;
        }
        return new perfstat_partition_config_t();
    }
}
