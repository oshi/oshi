/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.aix;

import java.util.function.Supplier;

import com.sun.jna.platform.unix.aix.Perfstat.perfstat_memory_total_t;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.platform.unix.aix.AixVirtualMemory;

/**
 * JNA-backed AIX VirtualMemory. Reads {@code perfstat_memory_total_t} fields from a memoized supplier owned by the
 * matching {@link AixGlobalMemoryJNA}.
 */
@ThreadSafe
final class AixVirtualMemoryJNA extends AixVirtualMemory {

    private final Supplier<perfstat_memory_total_t> perfstatMem;

    AixVirtualMemoryJNA(Supplier<perfstat_memory_total_t> perfstatMem) {
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
