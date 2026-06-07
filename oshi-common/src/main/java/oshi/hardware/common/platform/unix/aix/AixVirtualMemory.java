/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.aix;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.AbstractVirtualMemory;

/**
 * Abstract base for AIX VirtualMemory. Implements the {@link AbstractVirtualMemory} surface by translating
 * {@code perfstat_memory_total_t} fields (pages → bytes) into the shared interface. Concrete subclasses (JNA/FFM)
 * provide the platform-specific page accessors.
 */
@ThreadSafe
public abstract class AixVirtualMemory extends AbstractVirtualMemory {

    // AIX uses 4 KB pages for "pages" reported by perfstat (per the libperfstat docs).
    protected static final long PAGESIZE = 4096L;

    @Override
    public long getSwapUsed() {
        return (queryPgspTotal() - queryPgspFree()) * PAGESIZE;
    }

    @Override
    public long getSwapTotal() {
        return queryPgspTotal() * PAGESIZE;
    }

    @Override
    public long getVirtualMax() {
        return queryVirtTotal() * PAGESIZE;
    }

    @Override
    public long getVirtualInUse() {
        return queryVirtActive() * PAGESIZE;
    }

    @Override
    public long getSwapPagesIn() {
        return queryPgspins();
    }

    @Override
    public long getSwapPagesOut() {
        return queryPgspouts();
    }

    /**
     * @return {@code perfstat_memory_total_t.pgsp_total} (paging-space pages)
     */
    protected abstract long queryPgspTotal();

    /**
     * @return {@code perfstat_memory_total_t.pgsp_free} (paging-space pages)
     */
    protected abstract long queryPgspFree();

    /**
     * @return {@code perfstat_memory_total_t.virt_total} (4 KB pages)
     */
    protected abstract long queryVirtTotal();

    /**
     * @return {@code perfstat_memory_total_t.virt_active} (4 KB pages)
     */
    protected abstract long queryVirtActive();

    /**
     * @return {@code perfstat_memory_total_t.pgspins} (cumulative page-ins from paging space)
     */
    protected abstract long queryPgspins();

    /**
     * @return {@code perfstat_memory_total_t.pgspouts} (cumulative page-outs to paging space)
     */
    protected abstract long queryPgspouts();
}
