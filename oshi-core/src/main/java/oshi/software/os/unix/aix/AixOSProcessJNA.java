/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.aix;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.sun.jna.platform.unix.Resource;
import com.sun.jna.platform.unix.aix.Perfstat.perfstat_process_t;

import oshi.annotation.concurrent.ThreadSafe;
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
    private final Supplier<perfstat_process_t[]> procCpu;
    private final AixOperatingSystemJNA os;

    public AixOSProcessJNA(int pid, Quartet<Long, Long, Long, Long> cpuMem, Supplier<perfstat_process_t[]> procCpu,
            AixOperatingSystemJNA os) {
        super(pid);
        this.procCpu = procCpu;
        this.os = os;
        updateAttributes(cpuMem);
    }

    @Override
    public boolean updateAttributes() {
        for (perfstat_process_t stat : procCpu.get()) {
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
        return PsInfoJNA.queryArgsEnv(pid, psinfo);
    }

    @Override
    protected long getCpuAffinityMask() {
        return affinityMask.get();
    }

    @Override
    public long getSoftOpenFileLimit() {
        if (getProcessID() == this.os.getProcessId()) {
            final Resource.Rlimit rlimit = new Resource.Rlimit();
            AixLibc.INSTANCE.getrlimit(AixLibc.RLIMIT_NOFILE, rlimit);
            return rlimit.rlim_cur;
        }
        return -1L;
    }

    @Override
    public long getHardOpenFileLimit() {
        if (getProcessID() == this.os.getProcessId()) {
            final Resource.Rlimit rlimit = new Resource.Rlimit();
            AixLibc.INSTANCE.getrlimit(AixLibc.RLIMIT_NOFILE, rlimit);
            return rlimit.rlim_max;
        }
        return -1L;
    }
}
