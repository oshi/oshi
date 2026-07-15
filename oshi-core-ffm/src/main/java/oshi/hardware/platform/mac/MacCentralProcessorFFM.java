/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import static org.slf4j.event.Level.DEBUG;
import static org.slf4j.event.Level.ERROR;
import static oshi.ffm.ForeignFunctions.CAPTURED_STATE_LAYOUT;
import static oshi.ffm.ForeignFunctions.callInArenaIntOrDefault;
import static oshi.ffm.ForeignFunctions.callInArenaOrDefault;
import static oshi.ffm.ForeignFunctions.getErrno;
import static oshi.util.ExceptionUtil.runOrLog;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.platform.mac.MacSystem;
import oshi.ffm.platform.mac.MacSystemFunctions;
import oshi.hardware.common.platform.mac.IOKitProvider;
import oshi.hardware.common.platform.mac.MacCentralProcessor;
import oshi.hardware.common.platform.mac.SysctlProvider;

/**
 * A CPU using FFM.
 */
@ThreadSafe
final class MacCentralProcessorFFM extends MacCentralProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(MacCentralProcessorFFM.class);

    @Override
    protected SysctlProvider sysctlProvider() {
        return SysctlProviderFFM.INSTANCE;
    }

    @Override
    protected int[] queryHostCpuLoadTicks() {
        return callInArenaOrDefault(arena -> {
            MemorySegment callState = arena.allocate(CAPTURED_STATE_LAYOUT);
            MemorySegment cpuLoadInfo = arena.allocate(MacSystem.HOST_CPU_LOAD_INFO_DATA);
            MemorySegment count = arena.allocateFrom(ValueLayout.JAVA_INT,
                    (int) (MacSystem.HOST_CPU_LOAD_INFO_DATA.byteSize() / MacSystem.INT_SIZE));
            int machPort = MacSystemFunctions.mach_host_self();
            if (0 != MacSystemFunctions.host_statistics(callState, machPort, MacSystem.HOST_CPU_LOAD_INFO, cpuLoadInfo,
                    count)) {
                LOG.error("Failed to get System CPU ticks. Error code: {} ", getErrno(callState));
                return new int[0];
            }
            var cpuTicksHandle = MacSystem.HOST_CPU_LOAD_INFO_DATA.varHandle(MacSystem.CPU_TICKS,
                    MemoryLayout.PathElement.sequenceElement());
            int[] cpuTicks = new int[MacSystem.CPU_STATE_MAX];
            for (int i = 0; i < cpuTicks.length; i++) {
                cpuTicks[i] = (int) cpuTicksHandle.get(cpuLoadInfo, 0L, (long) i);
            }
            return cpuTicks;
        }, LOG, ERROR, "Failed to get System CPU ticks", new int[0]);
    }

    @Override
    protected int getloadavgNative(double[] loadavg, int nelem) {
        return callInArenaIntOrDefault(arena -> {
            MemorySegment loadavgSeg = arena.allocate(ValueLayout.JAVA_DOUBLE, nelem);
            int retval = MacSystemFunctions.getloadavg(loadavgSeg, nelem);
            for (int i = 0; i < nelem && i < retval; i++) {
                loadavg[i] = loadavgSeg.getAtIndex(ValueLayout.JAVA_DOUBLE, i);
            }
            return retval;
        }, LOG, DEBUG, "Failed to query system load average", -1);
    }

    @Override
    protected IOKitProvider ioKitProvider() {
        return IOKitProviderFFM.INSTANCE;
    }

    @Override
    protected int[] queryProcessorCpuTicks() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment callState = arena.allocate(CAPTURED_STATE_LAYOUT);
            MemorySegment procCountSeg = arena.allocate(ValueLayout.JAVA_INT);
            MemorySegment procInfoPtrSeg = arena.allocate(ValueLayout.ADDRESS);
            MemorySegment procInfoCountSeg = arena.allocate(ValueLayout.JAVA_INT);
            int machPort = MacSystemFunctions.mach_host_self();
            if (0 != MacSystemFunctions.host_processor_info(callState, machPort, MacSystem.PROCESSOR_CPU_LOAD_INFO,
                    procCountSeg, procInfoPtrSeg, procInfoCountSeg)) {
                LOG.error("Failed to update CPU Load. Error code: {}", getErrno(callState));
                return new int[0];
            }
            int procInfoCount = procInfoCountSeg.get(ValueLayout.JAVA_INT, 0);
            MemorySegment rawProcInfoPtr = procInfoPtrSeg.get(ValueLayout.ADDRESS, 0);
            MemorySegment procInfoPtr = rawProcInfoPtr.reinterpret((long) procInfoCount * MacSystem.INT_SIZE);
            try {
                int[] cpuTicks = new int[procInfoCount];
                for (int i = 0; i < procInfoCount; i++) {
                    cpuTicks[i] = procInfoPtr.getAtIndex(ValueLayout.JAVA_INT, i);
                }
                return cpuTicks;
            } finally {
                runOrLog(
                        () -> MacSystemFunctions.vm_deallocate(MacSystemFunctions.mach_task_self(),
                                rawProcInfoPtr.address(), (long) procInfoCount * MacSystem.INT_SIZE),
                        LOG, "Failed to vm_deallocate processor info buffer");
            }
        } catch (Throwable e) {
            LOG.error("Failed to update CPU Load", e);
        }
        return new int[0];
    }

}
