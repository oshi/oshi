/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.aix;

import static oshi.ffm.platform.unix.aix.AixLibcFunctions.RLIMIT_LAYOUT;
import static oshi.ffm.platform.unix.aix.AixLibcFunctions.RLIMIT_NOFILE;
import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.unix.aix.AixPerfstatProcess;
import oshi.driver.common.unix.aix.AixPsInfo;
import oshi.driver.unix.aix.PsInfoFFM;
import oshi.driver.unix.aix.perfstat.PerfstatCpuFFM;
import oshi.ffm.platform.unix.aix.AixLibcFunctions;
import oshi.software.common.os.unix.aix.AixOSProcess;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Quartet;

/**
 * FFM-backed AIX OSProcess.
 */
@ThreadSafe
public final class AixOSProcessFFM extends AixOSProcess {

    private final Supplier<Long> affinityMask = memoize(PerfstatCpuFFM::queryCpuAffinityMask, defaultExpiration());

    public AixOSProcessFFM(int pid, Quartet<Long, Long, Long, Long> cpuMem, Supplier<AixPerfstatProcess[]> procCpu,
            AixOperatingSystemFFM os) {
        super(pid, cpuMem, procCpu, os);
    }

    @Override
    protected Pair<List<String>, Map<String, String>> queryArgsEnv(int pid, AixPsInfo psinfo) {
        return PsInfoFFM.queryArgsEnv(pid, psinfo);
    }

    @Override
    protected long getCpuAffinityMask() {
        return affinityMask.get();
    }

    @Override
    protected long queryRlimitNofile(boolean soft) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment rlim = arena.allocate(RLIMIT_LAYOUT);
            if (AixLibcFunctions.getrlimit(RLIMIT_NOFILE, rlim) == 0) {
                return soft ? AixLibcFunctions.rlimitCur(rlim) : AixLibcFunctions.rlimitMax(rlim);
            }
        } catch (Throwable _) {
            // fall through
        }
        return -1L;
    }
}
