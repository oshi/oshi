/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.netbsd;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.VirtualMemory;
import oshi.hardware.common.AbstractGlobalMemory;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.common.platform.unix.bsd.BsdSysctlUtil;

/**
 * Memory obtained by sysctl and vmstat
 */
@ThreadSafe
final class NetBsdGlobalMemory extends AbstractGlobalMemory {

    private final Supplier<Long> available = memoize(NetBsdGlobalMemory::queryAvailable, defaultExpiration());

    private final Supplier<Long> total = memoize(NetBsdGlobalMemory::queryPhysMem);

    private final Supplier<Long> pageSize = memoize(NetBsdGlobalMemory::queryPageSize);

    private final Supplier<VirtualMemory> vm = memoize(this::createVirtualMemory);

    @Override
    public long getAvailable() {
        return available.get() * getPageSize();
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

    private static long queryAvailable() {
        long free = 0L;
        long inactive = 0L;
        for (String line : ExecutingCommand.runNative("vmstat -s")) {
            if (line.endsWith("pages free")) {
                free = ParseUtil.getFirstIntValue(line);
            } else if (line.endsWith("pages inactive")) {
                inactive = ParseUtil.getFirstIntValue(line);
            }
        }
        return free + inactive;
    }

    private static long queryPhysMem() {
        return BsdSysctlUtil.sysctl("hw.physmem64", 0L);
    }

    private static long queryPageSize() {
        return BsdSysctlUtil.sysctl("hw.pagesize", 4096L);
    }

    private VirtualMemory createVirtualMemory() {
        return new NetBsdVirtualMemory(this);
    }
}
