/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.ffm.mac.MacSystem;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static java.lang.foreign.ValueLayout.JAVA_INT;
import static oshi.ffm.ForeignFunctions.CAPTURED_STATE_LAYOUT;
import static oshi.ffm.ForeignFunctions.getErrno;
import static oshi.ffm.mac.MacSystem.VM_STATISTICS;
import static oshi.ffm.mac.MacSystemFunctions.host_statistics;
import static oshi.ffm.mac.MacSystemFunctions.mach_host_self;

final class MacMemoryUtilFFM {

    private static final Logger LOG = LoggerFactory.getLogger(MacMemoryUtilFFM.class);

    private MacMemoryUtilFFM() {
    }

    /**
     * Call host_statistics and put the result in vmStats.
     *
     * @param arena   the memory arena
     * @param vmStats the VM statistics memory segment
     * @return {@code true} when the call succeeded, {@code false} otherwise.
     */
    static boolean callVmStat(Arena arena, MemorySegment vmStats) throws Throwable {
        MemorySegment count = arena.allocate(JAVA_INT);
        MemorySegment callState = arena.allocate(CAPTURED_STATE_LAYOUT);
        // Set the count to the size of the VM statistics structure in integers
        count.set(JAVA_INT, 0, (int) (VM_STATISTICS.byteSize() / JAVA_INT.byteSize()));

        int result = host_statistics(callState, mach_host_self(), MacSystem.HOST_VM_INFO, vmStats, count);
        if (result != 0) {
            LOG.error("Failed to get host VM info. Error code: {}", getErrno(callState));
            return false;
        }
        return true;
    }

}
