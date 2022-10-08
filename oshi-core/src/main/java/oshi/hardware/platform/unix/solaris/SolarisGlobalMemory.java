/*
 * Copyright 2016-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.solaris;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.solaris.kstat.SystemPages;
import oshi.hardware.VirtualMemory;
import oshi.hardware.common.AbstractGlobalMemory;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;

/**
 * Memory obtained by kstat
 */
@ThreadSafe
final class SolarisGlobalMemory extends AbstractGlobalMemory {

    private final Supplier<Pair<Long, Long>> availTotal = memoize(SystemPages::queryAvailableTotal,
            defaultExpiration());

    private final Supplier<Long> pageSize = memoize(SolarisGlobalMemory::queryPageSize);

    private final Supplier<VirtualMemory> vm = memoize(this::createVirtualMemory);

    @Override
    public long getAvailable() {
        return availTotal.get().getA() * getPageSize();
    }

    @Override
    public long getTotal() {
        return availTotal.get().getB() * getPageSize();
    }

    @Override
    public long getPageSize() {
        return pageSize.get();
    }

    @Override
    public VirtualMemory getVirtualMemory() {
        return vm.get();
    }

    private static long queryPageSize() {
        return ParseUtil.parseLongOrDefault(ExecutingCommand.getFirstAnswer("pagesize"), 4096L);
    }

    private VirtualMemory createVirtualMemory() {
        return new SolarisVirtualMemory(this);
    }
}
