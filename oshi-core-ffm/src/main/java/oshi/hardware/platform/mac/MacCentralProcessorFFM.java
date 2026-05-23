/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import static org.slf4j.event.Level.DEBUG;
import static org.slf4j.event.Level.ERROR;
import static oshi.ffm.ForeignFunctions.CAPTURED_STATE_LAYOUT;
import static oshi.ffm.ForeignFunctions.callInArenaOrDefault;
import static oshi.ffm.ForeignFunctions.getErrno;
import static oshi.util.ExceptionUtil.runOrLog;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.mac.MacSystem;
import oshi.ffm.mac.MacSystemFunctions;
import oshi.hardware.common.platform.mac.IOKitProvider;
import oshi.hardware.common.platform.mac.MacCentralProcessor;
import oshi.hardware.common.platform.mac.SysctlProvider;
import oshi.util.FormatUtil;

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
    public long[] querySystemCpuLoadTicks() {
        return callInArenaOrDefault(arena -> {
            long[] ticks = new long[TickType.values().length];
            MemorySegment callState = arena.allocate(CAPTURED_STATE_LAYOUT);
            MemorySegment cpuLoadInfo = arena.allocate(MacSystem.HOST_CPU_LOAD_INFO_DATA);
            MemorySegment count = arena.allocateFrom(ValueLayout.JAVA_INT,
                    (int) (MacSystem.HOST_CPU_LOAD_INFO_DATA.byteSize() / MacSystem.INT_SIZE));
            int machPort = MacSystemFunctions.mach_host_self();
            if (0 != MacSystemFunctions.host_statistics(callState, machPort, MacSystem.HOST_CPU_LOAD_INFO, cpuLoadInfo,
                    count)) {
                LOG.error("Failed to get System CPU ticks. Error code: {} ", getErrno(callState));
                return ticks;
            }
            var cpuTicksHandle = MacSystem.HOST_CPU_LOAD_INFO_DATA.varHandle(MacSystem.CPU_TICKS,
                    MemoryLayout.PathElement.sequenceElement());
            ticks[TickType.USER.getIndex()] = Integer
                    .toUnsignedLong((int) cpuTicksHandle.get(cpuLoadInfo, 0L, (long) MacSystem.CPU_STATE_USER));
            ticks[TickType.NICE.getIndex()] = Integer
                    .toUnsignedLong((int) cpuTicksHandle.get(cpuLoadInfo, 0L, (long) MacSystem.CPU_STATE_NICE));
            ticks[TickType.SYSTEM.getIndex()] = Integer
                    .toUnsignedLong((int) cpuTicksHandle.get(cpuLoadInfo, 0L, (long) MacSystem.CPU_STATE_SYSTEM));
            ticks[TickType.IDLE.getIndex()] = Integer
                    .toUnsignedLong((int) cpuTicksHandle.get(cpuLoadInfo, 0L, (long) MacSystem.CPU_STATE_IDLE));
            return ticks;
        }, LOG, ERROR, "Failed to get System CPU ticks", new long[TickType.values().length]);
    }

    @Override
    public double[] getSystemLoadAverage(int nelem) {
        if (nelem < 1 || nelem > 3) {
            throw new IllegalArgumentException("Must include from one to three elements.");
        }
        double[] defaultAverage = new double[nelem];
        Arrays.fill(defaultAverage, -1d);
        return callInArenaOrDefault(arena -> {
            double[] average = new double[nelem];
            MemorySegment loadavgSeg = arena.allocate(ValueLayout.JAVA_DOUBLE, nelem);
            int retval = MacSystemFunctions.getloadavg(loadavgSeg, nelem);
            if (retval < nelem) {
                Arrays.fill(average, -1d);
            } else {
                for (int i = 0; i < nelem; i++) {
                    average[i] = loadavgSeg.getAtIndex(ValueLayout.JAVA_DOUBLE, i);
                }
            }
            return average;
        }, LOG, DEBUG, "Failed to query system load average", defaultAverage);
    }

    @Override
    protected IOKitProvider ioKitProvider() {
        return IOKitProviderFFM.INSTANCE;
    }

    @Override
    public long[][] queryProcessorCpuLoadTicks() {
        long[][] ticks = new long[getLogicalProcessorCount()][TickType.values().length];
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment callState = arena.allocate(CAPTURED_STATE_LAYOUT);
            MemorySegment procCountSeg = arena.allocate(ValueLayout.JAVA_INT);
            MemorySegment procInfoPtrSeg = arena.allocate(ValueLayout.ADDRESS);
            MemorySegment procInfoCountSeg = arena.allocate(ValueLayout.JAVA_INT);
            int machPort = MacSystemFunctions.mach_host_self();
            if (0 != MacSystemFunctions.host_processor_info(callState, machPort, MacSystem.PROCESSOR_CPU_LOAD_INFO,
                    procCountSeg, procInfoPtrSeg, procInfoCountSeg)) {
                LOG.error("Failed to update CPU Load. Error code: {}", getErrno(callState));
                return ticks;
            }
            int procCount = procCountSeg.get(ValueLayout.JAVA_INT, 0);
            int procInfoCount = procInfoCountSeg.get(ValueLayout.JAVA_INT, 0);
            MemorySegment rawProcInfoPtr = procInfoPtrSeg.get(ValueLayout.ADDRESS, 0);
            MemorySegment procInfoPtr = rawProcInfoPtr.reinterpret((long) procInfoCount * MacSystem.INT_SIZE);
            try {
                if (procCount != ticks.length) {
                    LOG.warn("host_processor_info returned {} CPUs but expected {}; capping iteration", procCount,
                            ticks.length);
                }
                int cpuLimit = Math.min(procCount, ticks.length);
                for (int cpu = 0; cpu < cpuLimit; cpu++) {
                    int offset = cpu * MacSystem.CPU_STATE_MAX;
                    ticks[cpu][TickType.USER.getIndex()] = FormatUtil.getUnsignedInt(
                            procInfoPtr.getAtIndex(ValueLayout.JAVA_INT, (long) offset + MacSystem.CPU_STATE_USER));
                    ticks[cpu][TickType.NICE.getIndex()] = FormatUtil.getUnsignedInt(
                            procInfoPtr.getAtIndex(ValueLayout.JAVA_INT, (long) offset + MacSystem.CPU_STATE_NICE));
                    ticks[cpu][TickType.SYSTEM.getIndex()] = FormatUtil.getUnsignedInt(
                            procInfoPtr.getAtIndex(ValueLayout.JAVA_INT, (long) offset + MacSystem.CPU_STATE_SYSTEM));
                    ticks[cpu][TickType.IDLE.getIndex()] = FormatUtil.getUnsignedInt(
                            procInfoPtr.getAtIndex(ValueLayout.JAVA_INT, (long) offset + MacSystem.CPU_STATE_IDLE));
                }
            } finally {
                runOrLog(() -> {
                    MacSystemFunctions.vm_deallocate(MacSystemFunctions.mach_task_self(), rawProcInfoPtr.address(),
                            (long) procInfoCount * MacSystem.INT_SIZE);
                }, LOG, "Failed to vm_deallocate processor info buffer");
            }
        } catch (Throwable e) {
            LOG.error("Failed to update CPU Load", e);
        }
        return ticks;
    }

}
