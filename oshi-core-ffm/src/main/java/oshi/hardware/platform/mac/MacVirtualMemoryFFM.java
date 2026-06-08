/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static org.slf4j.event.Level.DEBUG;
import static oshi.ffm.ForeignFunctions.callInArenaOrDefault;
import static oshi.ffm.platform.mac.MacSystem.VM_STATISTICS;
import static oshi.ffm.platform.mac.MacSystem.XSW_USAGE_TOTAL;
import static oshi.ffm.platform.mac.MacSystem.XSW_USAGE_USED;

import java.lang.foreign.MemorySegment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.platform.mac.MacSystem;
import oshi.ffm.util.platform.mac.SysctlUtilFFM;
import oshi.hardware.common.platform.mac.MacVirtualMemory;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;

@ThreadSafe
final class MacVirtualMemoryFFM extends MacVirtualMemory {
    private static final Logger LOG = LoggerFactory.getLogger(MacVirtualMemoryFFM.class);

    MacVirtualMemoryFFM(MacGlobalMemoryFFM macGlobalMemory) {
        super(macGlobalMemory);
    }

    @Override
    protected Pair<Long, Long> querySwapUsage() {
        return callInArenaOrDefault(arena -> {
            var xswUsage = arena.allocate(MacSystem.XSW_USAGE);
            if (SysctlUtilFFM.sysctl("vm.swapusage", xswUsage)) {
                long swapUsed = xswUsage.get(JAVA_LONG, MacSystem.XSW_USAGE.byteOffset(XSW_USAGE_USED));
                long swapTotal = xswUsage.get(JAVA_LONG, MacSystem.XSW_USAGE.byteOffset(XSW_USAGE_TOTAL));
                return new Pair<>(swapUsed, swapTotal);
            }
            return new Pair<>(0L, 0L);
        }, LOG, DEBUG, "Failed to query swap usage", new Pair<>(0L, 0L));
    }

    @Override
    protected Pair<Long, Long> queryVmStat() {
        return callInArenaOrDefault(arena -> {
            // Allocate memory for VM statistics structure and count
            MemorySegment vmStats = arena.allocate(VM_STATISTICS);
            if (MacMemoryUtilFFM.callVmStat(arena, vmStats)) {
                long swapPagesIn = ParseUtil.unsignedIntToLong(
                        vmStats.get(JAVA_INT, MacSystem.VM_STATISTICS.byteOffset(MacSystem.VM_PAGEINS)));
                long swapPagesOut = ParseUtil.unsignedIntToLong(
                        vmStats.get(JAVA_INT, MacSystem.VM_STATISTICS.byteOffset(MacSystem.VM_PAGEOUTS)));
                return new Pair<>(swapPagesIn, swapPagesOut);
            }
            return new Pair<>(0L, 0L);
        }, LOG, DEBUG, "Failed to query VM statistics", new Pair<>(0L, 0L));
    }
}
