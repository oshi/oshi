/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.aix;

import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.driver.unix.aix.perfstat.PerfstatMemoryFFM;
import oshi.hardware.common.platform.unix.aix.AixVirtualMemory;

/**
 * FFM-backed AIX VirtualMemory.
 */
@ThreadSafe
final class AixVirtualMemoryFFM extends AixVirtualMemory {

    private final Supplier<PerfstatMemoryFFM.MemoryTotal> perfstatMem;

    AixVirtualMemoryFFM(Supplier<PerfstatMemoryFFM.MemoryTotal> perfstatMem) {
        this.perfstatMem = perfstatMem;
    }

    @Override
    protected long queryPgspTotal() {
        return perfstatMem.get().pgsp_total;
    }

    @Override
    protected long queryPgspFree() {
        return perfstatMem.get().pgsp_free;
    }

    @Override
    protected long queryVirtTotal() {
        return perfstatMem.get().virt_total;
    }

    @Override
    protected long queryVirtActive() {
        return perfstatMem.get().virt_active;
    }

    @Override
    protected long queryPgspins() {
        return perfstatMem.get().pgspins;
    }

    @Override
    protected long queryPgspouts() {
        return perfstatMem.get().pgspouts;
    }
}
