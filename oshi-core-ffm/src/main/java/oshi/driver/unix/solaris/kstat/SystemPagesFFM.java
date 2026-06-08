/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.unix.solaris.kstat;

import java.lang.foreign.MemorySegment;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.util.platform.unix.solaris.KstatUtilFFM;
import oshi.ffm.util.platform.unix.solaris.KstatUtilFFM.KstatChain;
import oshi.util.tuples.Pair;

/**
 * FFM equivalent of {@code oshi.driver.unix.solaris.kstat.SystemPages}. On illumos (where {@code HAS_KSTAT2} is
 * {@code false}), this only exercises the legacy {@link KstatUtilFFM} path.
 */
@ThreadSafe
public final class SystemPagesFFM {

    private SystemPagesFFM() {
    }

    /**
     * Queries the {@code system_pages} kstat and returns available and physical memory in pages.
     *
     * @return a pair (available, total) in pages; multiply by page size for bytes.
     */
    public static Pair<Long, Long> queryAvailableTotal() {
        // The JNA SolarisOperatingSystem prefers Kstat2 on Solaris 11.4+; the FFM Kstat2 binding is a
        // stub today, so this method always falls through to LibKstat regardless of HAS_KSTAT2. Lower
        // fidelity is acceptable until a downstream consumer needs Solaris 11.4-specific kstat2 data.
        long memAvailable = 0L;
        long memTotal = 0L;
        try (KstatChain kc = KstatUtilFFM.openChain()) {
            MemorySegment ksp = kc.lookup(null, -1, "system_pages");
            if (ksp.address() != 0L && kc.read(ksp)) {
                memAvailable = KstatUtilFFM.dataLookupLong(ksp, "availrmem");
                memTotal = KstatUtilFFM.dataLookupLong(ksp, "physmem");
            }
        }
        return new Pair<>(memAvailable, memTotal);
    }
}
