/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.unix.solaris.kstat;

import static oshi.software.os.unix.solaris.SolarisOperatingSystem.HAS_KSTAT2;

import com.sun.jna.platform.unix.solaris.LibKstat.Kstat;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.platform.unix.solaris.KstatUtil;
import oshi.util.platform.unix.solaris.KstatUtil.KstatChain;
import oshi.util.tuples.Pair;

/**
 * Utility to query geom part list
 */
@ThreadSafe
public final class SystemPages {

    private SystemPages() {
    }

    /**
     * Queries the {@code system_pages} kstat and returns available and physical memory
     *
     * @return A pair with the available and total memory, in pages. Mutiply by page size for bytes.
     */
    public static Pair<Long, Long> queryAvailableTotal() {
        if (HAS_KSTAT2) {
            // Use Kstat2 implementation
            return queryAvailableTotal2();
        }
        long memAvailable = 0;
        long memTotal = 0;
        // Get first result
        try (KstatChain kc = KstatUtil.openChain()) {
            Kstat ksp = kc.lookup(null, -1, "system_pages");
            // Set values
            if (ksp != null && kc.read(ksp)) {
                memAvailable = KstatUtil.dataLookupLong(ksp, "availrmem"); // not a typo
                memTotal = KstatUtil.dataLookupLong(ksp, "physmem");
            }
        }
        return new Pair<>(memAvailable, memTotal);
    }

    private static Pair<Long, Long> queryAvailableTotal2() {
        Object[] results = KstatUtil.queryKstat2("kstat:/pages/unix/system_pages", "availrmem", "physmem");
        long avail = results[0] == null ? 0L : (long) results[0];
        long total = results[1] == null ? 0L : (long) results[1];
        return new Pair<>(avail, total);
    }
}
