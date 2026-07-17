/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.aix;

import static com.sun.jna.platform.unix.Resource.RLIMIT_NOFILE;
import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.sun.jna.platform.unix.Resource;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.unix.aix.AixPerfstatProcess;
import oshi.driver.common.unix.aix.AixPsInfo;
import oshi.driver.unix.aix.PsInfoJNA;
import oshi.driver.unix.aix.perfstat.PerfstatCpuJNA;
import oshi.jna.platform.unix.AixLibc;
import oshi.software.common.os.unix.aix.AixOSProcess;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Quartet;

/**
 * JNA-backed AIX OSProcess.
 */
@ThreadSafe
public final class AixOSProcessJNA extends AixOSProcess {

    private final Supplier<Long> affinityMask = memoize(PerfstatCpuJNA::queryCpuAffinityMask, defaultExpiration());

    public AixOSProcessJNA(int pid, Quartet<Long, Long, Long, Long> cpuMem, Supplier<AixPerfstatProcess[]> procCpu,
            AixOperatingSystemJNA os) {
        super(pid, cpuMem, procCpu, os);
    }

    @Override
    protected Pair<List<String>, Map<String, String>> queryArgsEnv(int pid, AixPsInfo psinfo) {
        return PsInfoJNA.queryArgsEnv(pid, psinfo);
    }

    @Override
    protected long getCpuAffinityMask() {
        return affinityMask.get();
    }

    @Override
    protected long queryRlimitNofile(boolean soft) {
        Resource.Rlimit rlimit = new Resource.Rlimit();
        if (AixLibc.INSTANCE.getrlimit(RLIMIT_NOFILE, rlimit) == 0) {
            return soft ? rlimit.rlim_cur : rlimit.rlim_max;
        }
        return -1L;
    }
}
