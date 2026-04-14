/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import static oshi.ffm.ForeignFunctions.CAPTURED_STATE_LAYOUT;
import static oshi.ffm.ForeignFunctions.getErrno;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.mac.IOKit.IOIterator;
import oshi.ffm.mac.IOKit.IORegistryEntry;
import oshi.ffm.mac.MacSystem;
import oshi.ffm.mac.MacSystemFunctions;
import oshi.hardware.common.platform.mac.MacCentralProcessor;
import oshi.util.FormatUtil;
import oshi.util.ParseUtil;
import oshi.util.Util;
import oshi.util.platform.mac.IOKitUtilFFM;
import oshi.util.platform.mac.SysctlUtilFFM;

/**
 * A CPU using FFM.
 */
@ThreadSafe
final class MacCentralProcessorFFM extends MacCentralProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(MacCentralProcessorFFM.class);
    private static final Pattern CPU_N = Pattern.compile("^cpu(\\d+)");

    @Override
    protected int sysctlInt(String name, int defaultValue) {
        return SysctlUtilFFM.sysctl(name, defaultValue);
    }

    @Override
    protected int sysctlIntNoWarn(String name, int defaultValue) {
        return SysctlUtilFFM.sysctl(name, defaultValue, false);
    }

    @Override
    protected long sysctlLong(String name, long defaultValue) {
        return SysctlUtilFFM.sysctl(name, defaultValue);
    }

    @Override
    protected String sysctlString(String name, String defaultValue) {
        return SysctlUtilFFM.sysctl(name, defaultValue);
    }

    @Override
    protected String sysctlStringNoWarn(String name, String defaultValue) {
        return SysctlUtilFFM.sysctl(name, defaultValue, false);
    }

    @Override
    public long[] querySystemCpuLoadTicks() {
        long[] ticks = new long[TickType.values().length];
        try (Arena arena = Arena.ofConfined()) {
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
        } catch (Throwable e) {
            LOG.error("Failed to get System CPU ticks", e);
        }
        return ticks;
    }

    @Override
    public double[] getSystemLoadAverage(int nelem) {
        if (nelem < 1 || nelem > 3) {
            throw new IllegalArgumentException("Must include from one to three elements.");
        }
        double[] average = new double[nelem];
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment loadavgSeg = arena.allocate(ValueLayout.JAVA_DOUBLE, nelem);
            int retval = MacSystemFunctions.getloadavg(loadavgSeg, nelem);
            if (retval < nelem) {
                Arrays.fill(average, -1d);
            } else {
                for (int i = 0; i < nelem; i++) {
                    average[i] = loadavgSeg.getAtIndex(ValueLayout.JAVA_DOUBLE, i);
                }
            }
        } catch (Throwable e) {
            Arrays.fill(average, -1d);
        }
        return average;
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
                try {
                    MacSystemFunctions.vm_deallocate(MacSystemFunctions.mach_task_self(), rawProcInfoPtr.address(),
                            (long) procInfoCount * MacSystem.INT_SIZE);
                } catch (Throwable e) {
                    LOG.warn("Failed to vm_deallocate processor info buffer", e);
                }
            }
        } catch (Throwable e) {
            LOG.error("Failed to update CPU Load", e);
        }
        return ticks;
    }

    @Override
    protected String platformExpert() {
        String manufacturer = null;
        IORegistryEntry platformExpert = IOKitUtilFFM.getMatchingService("IOPlatformExpertDevice");
        if (platformExpert != null) {
            try {
                byte[] data = platformExpert.getByteArrayProperty("manufacturer");
                if (data != null) {
                    manufacturer = new String(data, StandardCharsets.UTF_8).replace("\0", "").trim();
                }
            } finally {
                platformExpert.release();
            }
        }
        return Util.isBlank(manufacturer) ? "Apple Inc." : manufacturer;
    }

    @Override
    protected Map<Integer, String> queryCompatibleStrings() {
        Map<Integer, String> compatibleStrMap = new HashMap<>();
        IOIterator iter = IOKitUtilFFM.getMatchingServices("IOPlatformDevice");
        if (iter != null) {
            try {
                IORegistryEntry cpu = iter.next();
                while (cpu != null) {
                    try {
                        String name = cpu.getName();
                        if (name != null) {
                            Matcher m = CPU_N.matcher(name.toLowerCase(Locale.ROOT));
                            if (m.matches()) {
                                int procId = ParseUtil.parseIntOrDefault(m.group(1), 0);
                                byte[] data = cpu.getByteArrayProperty("compatible");
                                if (data != null) {
                                    compatibleStrMap.put(procId,
                                            new String(data, StandardCharsets.UTF_8).replace('\0', ' ').trim());
                                }
                            }
                        }
                    } finally {
                        cpu.release();
                    }
                    cpu = iter.next();
                }
            } finally {
                iter.release();
            }
        }
        return compatibleStrMap;
    }

    @Override
    protected void calculateNominalFrequencies() {
        IOIterator iter = IOKitUtilFFM.getMatchingServices("AppleARMIODevice");
        if (iter != null) {
            try {
                IORegistryEntry device = iter.next();
                try {
                    while (device != null) {
                        if ("pmgr".equalsIgnoreCase(device.getName())) {
                            setPerformanceCoreFrequency(
                                    getMaxFreqFromByteArray(device.getByteArrayProperty("voltage-states5-sram")));
                            setEfficiencyCoreFrequency(
                                    getMaxFreqFromByteArray(device.getByteArrayProperty("voltage-states1-sram")));
                            return;
                        }
                        device.release();
                        device = iter.next();
                    }
                } finally {
                    if (device != null) {
                        device.release();
                    }
                }
            } finally {
                iter.release();
            }
        }
    }
}
