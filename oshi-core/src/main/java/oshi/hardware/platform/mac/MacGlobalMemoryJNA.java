/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native;
import com.sun.jna.platform.mac.SystemB;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.VirtualMemory;
import oshi.hardware.common.platform.mac.MacGlobalMemory;
import oshi.jna.ByRef.CloseableIntByReference;
import oshi.jna.ByRef.CloseableLongByReference;
import oshi.jna.Struct.CloseableVMStatistics;
import oshi.util.platform.mac.SysctlUtil;

@ThreadSafe
final class MacGlobalMemoryJNA extends MacGlobalMemory {

    private static final Logger LOG = LoggerFactory.getLogger(MacGlobalMemoryJNA.class);

    @Override
    protected long queryVmStats() {
        try (CloseableVMStatistics vmStats = new CloseableVMStatistics();
                CloseableIntByReference size = new CloseableIntByReference(vmStats.size() / SystemB.INT_SIZE)) {
            if (0 != SystemB.INSTANCE.host_statistics(SystemB.INSTANCE.mach_host_self(), SystemB.HOST_VM_INFO, vmStats,
                    size)) {
                LOG.error("Failed to get host VM info. Error code: {}", Native.getLastError());
                return 0L;
            }
            return (vmStats.free_count + vmStats.inactive_count) * getPageSize();
        }
    }

    @Override
    protected long sysctl(String name, long defaultValue) {
        return SysctlUtil.sysctl(name, defaultValue);
    }

    @Override
    protected long queryPageSize() {
        try (CloseableLongByReference pPageSize = new CloseableLongByReference()) {
            if (0 == SystemB.INSTANCE.host_page_size(SystemB.INSTANCE.mach_host_self(), pPageSize)) {
                return pPageSize.getValue();
            }
        }
        LOG.error("Failed to get host page size. Error code: {}", Native.getLastError());
        return 4096L;
    }

    @Override
    protected VirtualMemory createVirtualMemory() {
        return new MacVirtualMemoryJNA(this);
    }

}
