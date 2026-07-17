/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.aix;

import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.unix.aix.AixPerfstatProcess;
import oshi.driver.unix.aix.perfstat.PerfstatConfigFFM;
import oshi.driver.unix.aix.perfstat.PerfstatProcessFFM;
import oshi.ffm.platform.unix.aix.AixLibcFunctions;
import oshi.software.common.os.unix.aix.AixOperatingSystem;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.util.tuples.Quartet;

/**
 * FFM-backed AIX OperatingSystem.
 */
@ThreadSafe
public final class AixOperatingSystemFFM extends AixOperatingSystem {

    private final Supplier<PerfstatConfigFFM.PartitionConfig> config = memoize(PerfstatConfigFFM::queryConfig);

    @Override
    protected AixPerfstatProcess[] queryPerfstatProcesses() {
        return PerfstatProcessFFM.queryProcesses();
    }

    @Override
    protected String queryOsBuildRaw() {
        return config.get().OSBuild;
    }

    @Override
    protected boolean is64BitKernel() {
        // 9th bit of conf is 64-bit kernel
        return (config.get().conf & 0x0080_0000) > 0;
    }

    @Override
    protected OSProcess createProcess(int pid, Quartet<Long, Long, Long, Long> cpuMem,
            Supplier<AixPerfstatProcess[]> procCpu) {
        return new AixOSProcessFFM(pid, cpuMem, procCpu, this);
    }

    @Override
    public int getProcessId() {
        try {
            return AixLibcFunctions.getpid();
        } catch (Throwable _) {
            return 0;
        }
    }

    @Override
    public int getThreadId() {
        try {
            return AixLibcFunctions.thread_self();
        } catch (Throwable _) {
            return 0;
        }
    }

    @Override
    public InternetProtocolStats getInternetProtocolStats() {
        return new AixInternetProtocolStatsFFM();
    }

    @Override
    public NetworkParams getNetworkParams() {
        return new AixNetworkParamsFFM();
    }
}
