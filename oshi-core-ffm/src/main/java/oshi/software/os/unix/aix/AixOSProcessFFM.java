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
import oshi.driver.common.unix.aix.AixPsInfo;
import oshi.driver.unix.aix.PsInfoFFM;
import oshi.driver.unix.aix.perfstat.PerfstatCpuFFM;
import oshi.driver.unix.aix.perfstat.PerfstatProcessFFM;
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
    private final Supplier<PerfstatProcessFFM.ProcessInfo[]> procCpu;
    private final AixOperatingSystemFFM os;

    public AixOSProcessFFM(int pid, Quartet<Long, Long, Long, Long> cpuMem,
            Supplier<PerfstatProcessFFM.ProcessInfo[]> procCpu, AixOperatingSystemFFM os) {
        super(pid);
        this.procCpu = procCpu;
        this.os = os;
        updateAttributes(cpuMem);
    }

    @Override
    public boolean updateAttributes() {
        for (PerfstatProcessFFM.ProcessInfo stat : procCpu.get()) {
            if ((int) stat.pid == getProcessID()) {
                return updateAttributes(new Quartet<>((long) stat.ucpu_time, (long) stat.scpu_time,
                        stat.real_inuse * 1024L, (stat.proc_real_mem_data + stat.proc_real_mem_text) * 1024L));
            }
        }
        setState(State.INVALID);
        return false;
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
    public long getSoftOpenFileLimit() {
        if (getProcessID() == this.os.getProcessId()) {
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment rlim = arena.allocate(RLIMIT_LAYOUT);
                if (AixLibcFunctions.getrlimit(RLIMIT_NOFILE, rlim) == 0) {
                    return AixLibcFunctions.rlimitCur(rlim);
                }
            } catch (Throwable t) {
                // fall through
            }
        }
        return -1L;
    }

    @Override
    public long getHardOpenFileLimit() {
        if (getProcessID() == this.os.getProcessId()) {
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment rlim = arena.allocate(RLIMIT_LAYOUT);
                if (AixLibcFunctions.getrlimit(RLIMIT_NOFILE, rlim) == 0) {
                    return AixLibcFunctions.rlimitMax(rlim);
                }
            } catch (Throwable t) {
                // fall through
            }
        }
        return -1L;
    }
}
