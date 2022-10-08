/*
 * Copyright 2019-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native;
import com.sun.jna.platform.mac.SystemB;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.AbstractVirtualMemory;
import oshi.jna.ByRef.CloseableIntByReference;
import oshi.jna.Struct.CloseableVMStatistics;
import oshi.jna.Struct.CloseableXswUsage;
import oshi.util.ParseUtil;
import oshi.util.platform.mac.SysctlUtil;
import oshi.util.tuples.Pair;

/**
 * Memory obtained by host_statistics (vm_stat) and sysctl.
 */
@ThreadSafe
final class MacVirtualMemory extends AbstractVirtualMemory {

    private static final Logger LOG = LoggerFactory.getLogger(MacVirtualMemory.class);

    private final MacGlobalMemory global;

    private final Supplier<Pair<Long, Long>> usedTotal = memoize(MacVirtualMemory::querySwapUsage, defaultExpiration());

    private final Supplier<Pair<Long, Long>> inOut = memoize(MacVirtualMemory::queryVmStat, defaultExpiration());

    /**
     * Constructor for MacVirtualMemory.
     *
     * @param macGlobalMemory The parent global memory class instantiating this
     */
    MacVirtualMemory(MacGlobalMemory macGlobalMemory) {
        this.global = macGlobalMemory;
    }

    @Override
    public long getSwapUsed() {
        return usedTotal.get().getA();
    }

    @Override
    public long getSwapTotal() {
        return usedTotal.get().getB();
    }

    @Override
    public long getVirtualMax() {
        return this.global.getTotal() + getSwapTotal();
    }

    @Override
    public long getVirtualInUse() {
        return this.global.getTotal() - this.global.getAvailable() + getSwapUsed();
    }

    @Override
    public long getSwapPagesIn() {
        return inOut.get().getA();
    }

    @Override
    public long getSwapPagesOut() {
        return inOut.get().getB();
    }

    private static Pair<Long, Long> querySwapUsage() {
        long swapUsed = 0L;
        long swapTotal = 0L;
        try (CloseableXswUsage xswUsage = new CloseableXswUsage()) {
            if (SysctlUtil.sysctl("vm.swapusage", xswUsage)) {
                swapUsed = xswUsage.xsu_used;
                swapTotal = xswUsage.xsu_total;
            }
        }
        return new Pair<>(swapUsed, swapTotal);
    }

    private static Pair<Long, Long> queryVmStat() {
        long swapPagesIn = 0L;
        long swapPagesOut = 0L;
        try (CloseableVMStatistics vmStats = new CloseableVMStatistics();
                CloseableIntByReference size = new CloseableIntByReference(vmStats.size() / SystemB.INT_SIZE)) {
            if (0 == SystemB.INSTANCE.host_statistics(SystemB.INSTANCE.mach_host_self(), SystemB.HOST_VM_INFO, vmStats,
                    size)) {
                swapPagesIn = ParseUtil.unsignedIntToLong(vmStats.pageins);
                swapPagesOut = ParseUtil.unsignedIntToLong(vmStats.pageouts);
            } else {
                LOG.error("Failed to get host VM info. Error code: {}", Native.getLastError());
            }
        }
        return new Pair<>(swapPagesIn, swapPagesOut);
    }
}
