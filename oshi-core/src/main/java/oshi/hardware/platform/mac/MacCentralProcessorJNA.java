/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native;
import com.sun.jna.platform.mac.IOKit.IOIterator;
import com.sun.jna.platform.mac.IOKit.IORegistryEntry;
import com.sun.jna.platform.mac.IOKitUtil;
import com.sun.jna.platform.mac.SystemB;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.platform.mac.MacCentralProcessor;
import oshi.jna.ByRef.CloseableIntByReference;
import oshi.jna.ByRef.CloseablePointerByReference;
import oshi.jna.Struct.CloseableHostCpuLoadInfo;
import oshi.util.FormatUtil;
import oshi.util.ParseUtil;
import oshi.util.Util;
import oshi.util.platform.mac.SysctlUtil;

/**
 * A CPU using JNA.
 */
@ThreadSafe
final class MacCentralProcessorJNA extends MacCentralProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(MacCentralProcessorJNA.class);
    private static final Pattern CPU_N = Pattern.compile("^cpu(\\d+)");

    @Override
    protected int sysctlInt(String name, int defaultValue) {
        return SysctlUtil.sysctl(name, defaultValue);
    }

    @Override
    protected int sysctlIntNoWarn(String name, int defaultValue) {
        return SysctlUtil.sysctl(name, defaultValue, false);
    }

    @Override
    protected long sysctlLong(String name, long defaultValue) {
        return SysctlUtil.sysctl(name, defaultValue);
    }

    @Override
    protected String sysctlString(String name, String defaultValue) {
        return SysctlUtil.sysctl(name, defaultValue);
    }

    @Override
    protected String sysctlStringNoWarn(String name, String defaultValue) {
        return SysctlUtil.sysctl(name, defaultValue, false);
    }

    @Override
    public long[] querySystemCpuLoadTicks() {
        long[] ticks = new long[TickType.values().length];
        int machPort = SystemB.INSTANCE.mach_host_self();
        try (CloseableHostCpuLoadInfo cpuLoadInfo = new CloseableHostCpuLoadInfo();
                CloseableIntByReference size = new CloseableIntByReference(cpuLoadInfo.size())) {
            int ret = SystemB.INSTANCE.host_statistics(machPort, SystemB.HOST_CPU_LOAD_INFO, cpuLoadInfo, size);
            if (0 != ret) {
                LOG.error("Failed to get System CPU ticks. Error code: {} ", ret);
                return ticks;
            }

            ticks[TickType.USER.getIndex()] = cpuLoadInfo.cpu_ticks[SystemB.CPU_STATE_USER];
            ticks[TickType.NICE.getIndex()] = cpuLoadInfo.cpu_ticks[SystemB.CPU_STATE_NICE];
            ticks[TickType.SYSTEM.getIndex()] = cpuLoadInfo.cpu_ticks[SystemB.CPU_STATE_SYSTEM];
            ticks[TickType.IDLE.getIndex()] = cpuLoadInfo.cpu_ticks[SystemB.CPU_STATE_IDLE];
        }
        return ticks;
    }

    @Override
    public double[] getSystemLoadAverage(int nelem) {
        if (nelem < 1 || nelem > 3) {
            throw new IllegalArgumentException("Must include from one to three elements.");
        }
        double[] average = new double[nelem];
        int retval = SystemB.INSTANCE.getloadavg(average, nelem);
        if (retval < nelem) {
            Arrays.fill(average, -1d);
        }
        return average;
    }

    @Override
    public long[][] queryProcessorCpuLoadTicks() {
        long[][] ticks = new long[getLogicalProcessorCount()][TickType.values().length];

        int machPort = SystemB.INSTANCE.mach_host_self();
        try (CloseableIntByReference procCount = new CloseableIntByReference();
                CloseablePointerByReference procCpuLoadInfo = new CloseablePointerByReference();
                CloseableIntByReference procInfoCount = new CloseableIntByReference()) {
            int ret = SystemB.INSTANCE.host_processor_info(machPort, SystemB.PROCESSOR_CPU_LOAD_INFO, procCount,
                    procCpuLoadInfo, procInfoCount);
            if (0 != ret) {
                LOG.error("Failed to update CPU Load. Error code: {}", ret);
                return ticks;
            }
            try {
                int[] cpuTicks = procCpuLoadInfo.getValue().getIntArray(0, procInfoCount.getValue());
                for (int cpu = 0; cpu < procCount.getValue(); cpu++) {
                    int offset = cpu * SystemB.CPU_STATE_MAX;
                    ticks[cpu][TickType.USER.getIndex()] = FormatUtil
                            .getUnsignedInt(cpuTicks[offset + SystemB.CPU_STATE_USER]);
                    ticks[cpu][TickType.NICE.getIndex()] = FormatUtil
                            .getUnsignedInt(cpuTicks[offset + SystemB.CPU_STATE_NICE]);
                    ticks[cpu][TickType.SYSTEM.getIndex()] = FormatUtil
                            .getUnsignedInt(cpuTicks[offset + SystemB.CPU_STATE_SYSTEM]);
                    ticks[cpu][TickType.IDLE.getIndex()] = FormatUtil
                            .getUnsignedInt(cpuTicks[offset + SystemB.CPU_STATE_IDLE]);
                }
            } finally {
                try {
                    oshi.jna.platform.mac.SystemB.INSTANCE.vm_deallocate(SystemB.INSTANCE.mach_task_self(),
                            com.sun.jna.Pointer.nativeValue(procCpuLoadInfo.getValue()),
                            (long) procInfoCount.getValue() * SystemB.INT_SIZE);
                } catch (Exception e) {
                    LOG.warn("Failed to vm_deallocate processor info buffer", e);
                }
            }
        }
        return ticks;
    }

    @Override
    protected String platformExpert() {
        String manufacturer = null;
        IORegistryEntry platformExpert = IOKitUtil.getMatchingService("IOPlatformExpertDevice");
        if (platformExpert != null) {
            byte[] data = platformExpert.getByteArrayProperty("manufacturer");
            if (data != null) {
                manufacturer = Native.toString(data, StandardCharsets.UTF_8);
            }
            platformExpert.release();
        }
        return Util.isBlank(manufacturer) ? "Apple Inc." : manufacturer;
    }

    @Override
    protected Map<Integer, String> queryCompatibleStrings() {
        Map<Integer, String> compatibleStrMap = new HashMap<>();
        IOIterator iter = IOKitUtil.getMatchingServices("IOPlatformDevice");
        if (iter != null) {
            IORegistryEntry cpu = iter.next();
            while (cpu != null) {
                Matcher m = CPU_N.matcher(cpu.getName().toLowerCase(Locale.ROOT));
                if (m.matches()) {
                    int procId = ParseUtil.parseIntOrDefault(m.group(1), 0);
                    byte[] data = cpu.getByteArrayProperty("compatible");
                    if (data != null) {
                        compatibleStrMap.put(procId,
                                new String(data, StandardCharsets.UTF_8).replace('\0', ' ').trim());
                    }
                }
                cpu.release();
                cpu = iter.next();
            }
            iter.release();
        }
        return compatibleStrMap;
    }

    @Override
    protected void calculateNominalFrequencies() {
        IOIterator iter = IOKitUtil.getMatchingServices("AppleARMIODevice");
        if (iter != null) {
            try {
                IORegistryEntry device = iter.next();
                try {
                    while (device != null) {
                        if (device.getName().equalsIgnoreCase("pmgr")) {
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
