/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.aix;

import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import com.sun.jna.Native;
import com.sun.jna.platform.unix.aix.Perfstat.perfstat_partition_config_t;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.unix.aix.AixPerfstatProcess;
import oshi.driver.unix.aix.perfstat.PerfstatConfigJNA;
import oshi.driver.unix.aix.perfstat.PerfstatProcessJNA;
import oshi.jna.platform.unix.AixLibc;
import oshi.software.common.os.unix.aix.AixOperatingSystem;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.util.tuples.Quartet;

/**
 * JNA-backed AIX OperatingSystem.
 */
@ThreadSafe
public final class AixOperatingSystemJNA extends AixOperatingSystem {

    private final Supplier<perfstat_partition_config_t> config = memoize(PerfstatConfigJNA::queryConfig);

    @Override
    protected AixPerfstatProcess[] queryPerfstatProcesses() {
        return PerfstatProcessJNA.queryProcesses();
    }

    @Override
    protected String queryOsBuildRaw() {
        return Native.toString(config.get().OSBuild);
    }

    @Override
    protected boolean is64BitKernel() {
        // 9th bit of conf is 64-bit kernel
        return (config.get().conf & 0x0080_0000) > 0;
    }

    @Override
    protected OSProcess createProcess(int pid, Quartet<Long, Long, Long, Long> cpuMem,
            Supplier<AixPerfstatProcess[]> procCpu) {
        return new AixOSProcessJNA(pid, cpuMem, procCpu, this);
    }

    @Override
    public int getProcessId() {
        return AixLibc.INSTANCE.getpid();
    }

    @Override
    public int getThreadId() {
        return AixLibc.INSTANCE.thread_self();
    }

    @Override
    public InternetProtocolStats getInternetProtocolStats() {
        return new AixInternetProtocolStatsJNA();
    }

    @Override
    public NetworkParams getNetworkParams() {
        return new AixNetworkParamsJNA();
    }
}
