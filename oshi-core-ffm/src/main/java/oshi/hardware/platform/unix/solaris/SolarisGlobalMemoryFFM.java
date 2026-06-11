/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.solaris;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.solaris.kstat.SystemPagesFFM;
import oshi.hardware.common.platform.unix.solaris.SolarisGlobalMemory;
import oshi.util.tuples.Pair;

@ThreadSafe
final class SolarisGlobalMemoryFFM extends SolarisGlobalMemory {

    private final Supplier<Pair<Long, Long>> availTotal = memoize(SystemPagesFFM::queryAvailableTotal,
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
