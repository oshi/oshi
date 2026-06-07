/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.driver.unix.aix.perfstat;

import static oshi.ffm.unix.aix.PerfstatFunctions.PERFSTAT_MEMORY_TOTAL_T_SIZE;
import static oshi.ffm.unix.aix.PerfstatFunctions.memPgspFree;
import static oshi.ffm.unix.aix.PerfstatFunctions.memPgspTotal;
import static oshi.ffm.unix.aix.PerfstatFunctions.memPgspins;
import static oshi.ffm.unix.aix.PerfstatFunctions.memPgspouts;
import static oshi.ffm.unix.aix.PerfstatFunctions.memRealAvail;
import static oshi.ffm.unix.aix.PerfstatFunctions.memRealTotal;
import static oshi.ffm.unix.aix.PerfstatFunctions.memVirtActive;
import static oshi.ffm.unix.aix.PerfstatFunctions.memVirtTotal;
import static oshi.ffm.unix.aix.PerfstatFunctions.perfstat_memory_total;

import java.lang.foreign.MemorySegment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.ForeignFunctions;

/**
 * FFM-backed driver for {@code perfstat_memory_total}, mirroring
 * {@code oshi.driver.unix.aix.perfstat.PerfstatMemoryJNA}.
 */
@ThreadSafe
public final class PerfstatMemoryFFM {

    private static final Logger LOG = LoggerFactory.getLogger(PerfstatMemoryFFM.class);

    private PerfstatMemoryFFM() {
    }

    /** POJO mirror of the {@code perfstat_memory_total_t} fields OSHI consumes. */
    public static final class MemoryTotal {
        public long virt_total;
        public long real_total;
        public long real_avail;
        public long virt_active;
        public long pgspins;
        public long pgspouts;
        public long pgsp_total;
        public long pgsp_free;
    }

    /**
     * Queries {@code perfstat_memory_total}.
     *
     * @return populated {@link MemoryTotal}, or an empty instance on error
     */
    public static MemoryTotal queryMemoryTotal() {
        return ForeignFunctions.callInArenaOrDefault(arena -> {
            MemoryTotal result = new MemoryTotal();
            MemorySegment buf = arena.allocate(PERFSTAT_MEMORY_TOTAL_T_SIZE);
            int ret = perfstat_memory_total(MemorySegment.NULL, buf, PERFSTAT_MEMORY_TOTAL_T_SIZE, 1);
            if (ret > 0) {
                result.virt_total = memVirtTotal(buf);
                result.real_total = memRealTotal(buf);
                result.real_avail = memRealAvail(buf);
                result.virt_active = memVirtActive(buf);
                result.pgspins = memPgspins(buf);
                result.pgspouts = memPgspouts(buf);
                result.pgsp_total = memPgspTotal(buf);
                result.pgsp_free = memPgspFree(buf);
            }
            return result;
        }, LOG, Level.TRACE, "Failed to query memory total", new MemoryTotal());
    }
}
