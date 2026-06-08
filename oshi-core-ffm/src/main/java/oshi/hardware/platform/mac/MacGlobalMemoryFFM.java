/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static org.slf4j.event.Level.DEBUG;
import static org.slf4j.event.Level.ERROR;
import static oshi.ffm.ForeignFunctions.callInArenaLongOrDefault;
import static oshi.ffm.platform.mac.MacSystem.VM_FREE_COUNT;
import static oshi.ffm.platform.mac.MacSystem.VM_INACTIVE_COUNT;
import static oshi.ffm.platform.mac.MacSystem.VM_STATISTICS;
import static oshi.ffm.platform.mac.MacSystemFunctions.mach_host_self;

import java.lang.foreign.MemorySegment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.platform.mac.MacSystemFunctions;
import oshi.hardware.VirtualMemory;
import oshi.hardware.common.platform.mac.MacGlobalMemory;
import oshi.hardware.common.platform.mac.SysctlProvider;

@ThreadSafe
final class MacGlobalMemoryFFM extends MacGlobalMemory {

    private static final Logger LOG = LoggerFactory.getLogger(MacGlobalMemoryFFM.class);

    @Override
    protected long queryVmStats() {
        return callInArenaLongOrDefault(arena -> {
            // Allocate memory for VM statistics structure and count
            MemorySegment vmStats = arena.allocate(VM_STATISTICS);
            if (MacMemoryUtilFFM.callVmStat(arena, vmStats)) {
                // Read free_count and inactive_count from the structure
                int freeCount = vmStats.get(JAVA_INT, VM_STATISTICS.byteOffset(VM_FREE_COUNT));
                int inactiveCount = vmStats.get(JAVA_INT, VM_STATISTICS.byteOffset(VM_INACTIVE_COUNT));

                return (freeCount + inactiveCount) * getPageSize();
            }
            return 0L;
        }, LOG, DEBUG, "Failed to query host VM statistics", 0L);
    }

    @Override
    protected SysctlProvider sysctlProvider() {
        return SysctlProviderFFM.INSTANCE;
    }

    @Override
    protected long queryPageSize() {
        return callInArenaLongOrDefault(arena -> {
            MemorySegment pageSize = arena.allocate(JAVA_LONG);
            int result = MacSystemFunctions.host_page_size(mach_host_self(), pageSize);
            if (result == 0) {
                return pageSize.get(JAVA_LONG, 0);
            }
            LOG.error("Failed to get host page size. Error code: {}", result);
            return 4096L;
        }, LOG, ERROR, "Failed to get host page size", 4096L);
    }

    @Override
    protected VirtualMemory createVirtualMemory() {
        return new MacVirtualMemoryFFM(this);
    }
}
