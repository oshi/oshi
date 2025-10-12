/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.mac.MacSystemFunctions;
import oshi.hardware.VirtualMemory;
import oshi.util.platform.mac.SysctlUtilFFM;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static oshi.ffm.mac.MacSystem.VM_FREE_COUNT;
import static oshi.ffm.mac.MacSystem.VM_INACTIVE_COUNT;
import static oshi.ffm.mac.MacSystem.VM_STATISTICS;
import static oshi.ffm.mac.MacSystemFunctions.mach_host_self;

@ThreadSafe
final class MacGlobalMemoryFFM extends MacGlobalMemory {

    private static final Logger LOG = LoggerFactory.getLogger(MacGlobalMemoryFFM.class);

    @Override
    protected long queryVmStats() {
        try (Arena arena = Arena.ofConfined()) {
            // Allocate memory for VM statistics structure and count
            MemorySegment vmStats = arena.allocate(VM_STATISTICS);
            if (MacMemoryUtil.callVmStat(arena, vmStats)) {
                // Read free_count and inactive_count from the structure
                int freeCount = vmStats.get(JAVA_INT, VM_STATISTICS.byteOffset(VM_FREE_COUNT));
                int inactiveCount = vmStats.get(JAVA_INT, VM_STATISTICS.byteOffset(VM_INACTIVE_COUNT));

                return (freeCount + inactiveCount) * getPageSize();
            }
        } catch (Throwable e) {
            // Ignore
        }
        return 0L;
    }

    @Override
    protected long sysctl(String name, long defaultValue) {
        return SysctlUtilFFM.sysctl(name, defaultValue);
    }

    @Override
    protected long host_page_size() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pageSize = arena.allocate(JAVA_LONG);
            int result = MacSystemFunctions.host_page_size(mach_host_self(), pageSize);
            if (result == 0) {
                return pageSize.get(JAVA_LONG, 0);
            }
        } catch (Throwable t) {
            // IGNORED
        }
        return -1;
    }

    @Override
    protected VirtualMemory createVirtualMemory() {
        return new MacVirtualMemoryFFM(this);
    }
}
