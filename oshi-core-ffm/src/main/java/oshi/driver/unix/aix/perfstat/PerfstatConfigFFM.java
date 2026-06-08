/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.unix.aix.perfstat;

import static oshi.ffm.unix.aix.PerfstatFunctions.PERFSTAT_PARTITION_CONFIG_T_SIZE;
import static oshi.ffm.unix.aix.PerfstatFunctions.configConf;
import static oshi.ffm.unix.aix.PerfstatFunctions.configMachineID;
import static oshi.ffm.unix.aix.PerfstatFunctions.configOSBuild;
import static oshi.ffm.unix.aix.PerfstatFunctions.configProcessorMHz;
import static oshi.ffm.unix.aix.PerfstatFunctions.configSmtthreads;
import static oshi.ffm.unix.aix.PerfstatFunctions.configVcpusMax;
import static oshi.ffm.unix.aix.PerfstatFunctions.perfstat_partition_config;

import java.lang.foreign.MemorySegment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.ForeignFunctions;

/**
 * FFM-backed driver for {@code perfstat_partition_config}, mirroring
 * {@code oshi.driver.unix.aix.perfstat.PerfstatConfigJNA}.
 */
@ThreadSafe
public final class PerfstatConfigFFM {

    private static final Logger LOG = LoggerFactory.getLogger(PerfstatConfigFFM.class);

    private PerfstatConfigFFM() {
    }

    /** POJO mirror of the {@code perfstat_partition_config_t} fields OSHI consumes. */
    public static final class PartitionConfig {
        public int conf;
        public String machineID = "";
        public double processorMHz;
        public String OSBuild = "";
        public int smtthreads;
        public long vcpusMax;
    }

    /**
     * Queries {@code perfstat_partition_config}.
     *
     * @return populated {@link PartitionConfig}, or an empty instance on error
     */
    public static PartitionConfig queryConfig() {
        return ForeignFunctions.callInArenaOrDefault(arena -> {
            PartitionConfig result = new PartitionConfig();
            MemorySegment buf = arena.allocate(PERFSTAT_PARTITION_CONFIG_T_SIZE);
            int ret = perfstat_partition_config(MemorySegment.NULL, buf, PERFSTAT_PARTITION_CONFIG_T_SIZE, 1);
            if (ret > 0) {
                result.conf = configConf(buf);
                result.machineID = configMachineID(buf);
                result.processorMHz = configProcessorMHz(buf);
                result.OSBuild = configOSBuild(buf);
                result.smtthreads = configSmtthreads(buf);
                result.vcpusMax = configVcpusMax(buf);
            }
            return result;
        }, LOG, Level.TRACE, "Failed to query partition config", new PartitionConfig());
    }
}
