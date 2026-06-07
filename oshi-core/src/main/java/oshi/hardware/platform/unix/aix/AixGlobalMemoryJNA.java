/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.aix;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.List;
import java.util.function.Supplier;

import com.sun.jna.platform.unix.aix.Perfstat.perfstat_memory_total_t;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.aix.perfstat.PerfstatMemoryJNA;
import oshi.hardware.VirtualMemory;
import oshi.hardware.common.platform.unix.aix.AixGlobalMemory;

/**
 * JNA-backed AIX GlobalMemory. Memoizes {@code PerfstatMemoryJNA.queryMemoryTotal()} and exposes its fields for the
 * {@link AixGlobalMemory} base and paired {@link AixVirtualMemoryJNA}.
 */
@ThreadSafe
final class AixGlobalMemoryJNA extends AixGlobalMemory {

    private final Supplier<perfstat_memory_total_t> perfstatMem = memoize(PerfstatMemoryJNA::queryMemoryTotal,
            defaultExpiration());
    private final Supplier<VirtualMemory> vm = memoize(this::createVirtualMemory);

    AixGlobalMemoryJNA(Supplier<List<String>> lscfg) {
        super(lscfg);
    }

    @Override
    public VirtualMemory getVirtualMemory() {
        return vm.get();
    }

    @Override
    protected long queryRealAvail() {
        return perfstatMem.get().real_avail;
    }

    @Override
    protected long queryRealTotal() {
        return perfstatMem.get().real_total;
    }

    private VirtualMemory createVirtualMemory() {
        return new AixVirtualMemoryJNA(perfstatMem);
    }
}
