/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.solaris;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.solaris.kstat.SystemPages;
import oshi.hardware.common.platform.unix.solaris.SolarisGlobalMemory;
import oshi.util.tuples.Pair;

/**
 * JNA-backed Solaris GlobalMemory.
 */
@ThreadSafe
final class SolarisGlobalMemoryJNA extends SolarisGlobalMemory {

    private final Supplier<Pair<Long, Long>> availTotal = memoize(SystemPages::queryAvailableTotal,
            defaultExpiration());

    @Override
    protected long queryAvailable() {
        return availTotal.get().getA() * getPageSize();
    }

    @Override
    protected long queryTotal() {
        return availTotal.get().getB() * getPageSize();
    }
}
