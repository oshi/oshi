/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.openbsd;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.hardware.VirtualMemory;
import oshi.hardware.common.AbstractGlobalMemory;

public abstract class OpenBsdGlobalMemory extends AbstractGlobalMemory {

    private final Supplier<Long> available = memoize(this::queryAvailable, defaultExpiration());
    private final Supplier<Long> total = memoize(this::queryPhysMem);
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
    protected abstract long queryPhysMem();

    /**
     * Queries the system page size in bytes.
     *
     * @return the page size in bytes
     */
    protected abstract long queryPageSize();

    /**
     * Constructs the matching OpenBsdVirtualMemory concrete (JNA or FFM) for this instance.
     *
     * @return the VirtualMemory paired with this GlobalMemory implementation
     */
    protected abstract VirtualMemory createVirtualMemory();
}
