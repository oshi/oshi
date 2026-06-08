/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.aix;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.List;
import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.aix.perfstat.PerfstatMemoryFFM;
import oshi.hardware.VirtualMemory;
import oshi.hardware.common.platform.unix.aix.AixGlobalMemory;

/**
 * FFM-backed AIX GlobalMemory.
 */
@ThreadSafe
final class AixGlobalMemoryFFM extends AixGlobalMemory {

    private final Supplier<PerfstatMemoryFFM.MemoryTotal> perfstatMem = memoize(PerfstatMemoryFFM::queryMemoryTotal,
            defaultExpiration());
    private final Supplier<VirtualMemory> vm = memoize(this::createVirtualMemory);

    AixGlobalMemoryFFM(Supplier<List<String>> lscfg) {
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
        return new AixVirtualMemoryFFM(perfstatMem);
    }
}
