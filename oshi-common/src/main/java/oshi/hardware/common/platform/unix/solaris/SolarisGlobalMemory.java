/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.solaris;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.hardware.VirtualMemory;
import oshi.hardware.common.AbstractGlobalMemory;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Memory obtained by kstat.
 */
public abstract class SolarisGlobalMemory extends AbstractGlobalMemory {

    private final Supplier<Long> available = memoize(this::queryAvailable, defaultExpiration());
    private final Supplier<Long> total = memoize(this::queryTotal);
    private final Supplier<Long> pageSize = memoize(this::queryPageSize);
    private final Supplier<VirtualMemory> vm = memoize(this::createVirtualMemory);

    @Override
    public long getAvailable() {
        return available.get();
    }

    @Override
    public long getTotal() {
        return total.get();
    }

    @Override
    public long getPageSize() {
        return pageSize.get();
    }

    @Override
    public VirtualMemory getVirtualMemory() {
        return vm.get();
    }

    /**
     * Queries available memory in bytes.
     *
     * @return available memory in bytes
     */
    protected abstract long queryAvailable();

    /**
     * Queries total physical memory in bytes.
     *
     * @return total physical memory in bytes
     */
    protected abstract long queryTotal();

    /**
     * Queries the system page size in bytes via the {@code pagesize} command.
     *
     * @return the page size in bytes
     */
    protected long queryPageSize() {
        return ParseUtil.parseLongOrDefault(ExecutingCommand.getFirstAnswer("pagesize"), 4096L);
    }

    /**
     * Constructs the {@link SolarisVirtualMemory} paired with this GlobalMemory instance.
     *
     * @return the VirtualMemory paired with this GlobalMemory implementation
     */
    protected VirtualMemory createVirtualMemory() {
        return new SolarisVirtualMemory(this);
    }
}
