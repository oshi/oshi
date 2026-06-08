/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.aix;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.aix.perfstat.PerfstatConfigFFM;
import oshi.driver.unix.aix.perfstat.PerfstatCpuFFM;
import oshi.hardware.common.platform.unix.aix.AixCentralProcessor;

/**
 * FFM-backed AIX CentralProcessor.
 */
@ThreadSafe
final class AixCentralProcessorFFM extends AixCentralProcessor {

    private final Supplier<PerfstatCpuFFM.CpuTotal> cpuTotal = memoize(PerfstatCpuFFM::queryCpuTotal,
            defaultExpiration());
    private final Supplier<PerfstatCpuFFM.Cpu[]> cpuProc = memoize(PerfstatCpuFFM::queryCpu, defaultExpiration());

    @Override
    protected PartitionInfo queryPartitionInfo() {
        PerfstatConfigFFM.PartitionConfig config = PerfstatConfigFFM.queryConfig();
        PartitionInfo info = new PartitionInfo();
        info.vcpusMax = config.vcpusMax;
        info.smtthreads = config.smtthreads;
        info.machineID = config.machineID;
        info.processorMHz = config.processorMHz;
        return info;
    }

    @Override
    protected CpuTotalRow queryCpuTotal() {
        PerfstatCpuFFM.CpuTotal p = cpuTotal.get();
        CpuTotalRow row = new CpuTotalRow();
        row.ncpus = p.ncpus;
        row.processorHZ = p.processorHZ;
        row.user = p.user;
        row.sys = p.sys;
        row.idle = p.idle;
        row.wait = p.wait;
        row.pswitch = p.pswitch;
        row.devintrs = p.devintrs;
        row.softintrs = p.softintrs;
        row.idle_stolen_purr = p.idle_stolen_purr;
        row.busy_stolen_purr = p.busy_stolen_purr;
        row.loadavg[0] = p.loadavg[0];
        row.loadavg[1] = p.loadavg[1];
        row.loadavg[2] = p.loadavg[2];
        return row;
    }

    @Override
    protected CpuTickRow[] queryPerCpuTicks() {
        PerfstatCpuFFM.Cpu[] cpus = cpuProc.get();
        CpuTickRow[] rows = new CpuTickRow[cpus.length];
        for (int i = 0; i < cpus.length; i++) {
            CpuTickRow row = new CpuTickRow();
            row.user = cpus[i].user;
            row.sys = cpus[i].sys;
            row.idle = cpus[i].idle;
            row.wait = cpus[i].wait;
            row.devintrs = cpus[i].devintrs;
            row.softintrs = cpus[i].softintrs;
            row.idle_stolen_purr = cpus[i].idle_stolen_purr;
            row.busy_stolen_purr = cpus[i].busy_stolen_purr;
            rows[i] = row;
        }
        return rows;
    }
}
