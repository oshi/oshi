/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.linux;

import static org.slf4j.event.Level.WARN;
import static oshi.ffm.ForeignFunctions.callInArenaBooleanOrDefault;
import static oshi.ffm.ForeignFunctions.callInArenaLongOrDefault;
import static oshi.ffm.ForeignFunctions.callInArenaOrDefault;
import static oshi.software.os.linux.LinuxOperatingSystemFFM.HAS_UDEV;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.linux.proc.AuxvFFM;
import oshi.ffm.linux.LinuxLibcFunctions;
import oshi.ffm.linux.UdevFunctions;
import oshi.hardware.common.platform.linux.LinuxCentralProcessor;
import oshi.software.os.linux.LinuxOperatingSystemFFM;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.driver.linux.proc.Auxv;
import oshi.util.tuples.Quartet;

/**
 * FFM-based Linux central processor implementation. Extends {@link LinuxCentralProcessor}, overriding udev-dependent
 * methods with FFM implementations via {@link UdevFunctions}.
 */
@ThreadSafe
public final class LinuxCentralProcessorFFM extends LinuxCentralProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxCentralProcessorFFM.class);

    public LinuxCentralProcessorFFM() {
        super(LinuxOperatingSystemFFM.hz());
    }

    @Override
    protected long queryHwcap() {
        return AuxvFFM.queryAuxv().getOrDefault(Auxv.AT_HWCAP, 0L);
    }

    @Override
    public double[] getSystemLoadAverage(int nelem) {
        if (nelem < 1 || nelem > 3) {
            throw new IllegalArgumentException("Must include from one to three elements.");
        }
        double[] average = callInArenaOrDefault(arena -> {
            MemorySegment loadavg = arena.allocate(ValueLayout.JAVA_DOUBLE, nelem);
            int retval = LinuxLibcFunctions.getloadavg(loadavg, nelem);
            double[] result = new double[nelem];
            for (int i = 0; i < nelem; i++) {
                result[i] = retval > i ? loadavg.getAtIndex(ValueLayout.JAVA_DOUBLE, i) : -1d;
            }
            return result;
        }, LOG, WARN, "FFM getloadavg failed", null);
        return average == null ? super.getSystemLoadAverage(nelem) : average;
    }

    @Override
    protected Quartet<List<LogicalProcessor>, List<ProcessorCache>, Map<Integer, Integer>, Map<Integer, String>> readTopologyWithUdev() {
        if (!HAS_UDEV) {
            return readTopologyFromSysfs();
        }
        List<LogicalProcessor> logProcs = new ArrayList<>();
        Set<ProcessorCache> caches = new HashSet<>();
        Map<Integer, Integer> coreEfficiencyMap = new HashMap<>();
        Map<Integer, String> modAliasMap = new HashMap<>();
        Quartet<List<LogicalProcessor>, List<ProcessorCache>, Map<Integer, Integer>, Map<Integer, String>> topology = callInArenaOrDefault(
                arena -> {
                    MemorySegment udev = UdevFunctions.udev_new();
                    if (MemorySegment.NULL.equals(udev)) {
                        return readTopologyFromSysfs();
                    }
                    try {
                        MemorySegment enumerate = UdevFunctions.udev_enumerate_new(udev);
                        try {
                            UdevFunctions.addMatchSubsystem(enumerate, "cpu", arena);
                            UdevFunctions.udev_enumerate_scan_devices(enumerate);
                            for (MemorySegment entry = UdevFunctions
                                    .udev_enumerate_get_list_entry(enumerate); !MemorySegment.NULL
                                            .equals(entry); entry = UdevFunctions.udev_list_entry_get_next(entry)) {
                                String syspath = UdevFunctions.getString(UdevFunctions.udev_list_entry_get_name(entry),
                                        arena);
                                if (syspath == null) {
                                    continue;
                                }
                                MemorySegment device = UdevFunctions.deviceNewFromSyspath(udev, syspath, arena);
                                String modAlias = null;
                                if (!MemorySegment.NULL.equals(device)) {
                                    try {
                                        modAlias = UdevFunctions.getPropertyValue(device, "MODALIAS", arena);
                                    } finally {
                                        UdevFunctions.udev_device_unref(device);
                                    }
                                }
                                logProcs.add(getLogicalProcessorFromSyspath(syspath, caches, modAlias,
                                        coreEfficiencyMap, modAliasMap));
                            }
                        } finally {
                            UdevFunctions.udev_enumerate_unref(enumerate);
                        }
                    } finally {
                        UdevFunctions.udev_unref(udev);
                    }
                    return new Quartet<>(logProcs, orderedProcCaches(caches), coreEfficiencyMap, modAliasMap);
                }, LOG, WARN, "Error reading CPU topology via udev, falling back to sysfs", null);
        return topology == null ? readTopologyFromSysfs() : topology;
    }

    @Override
    protected boolean queryCurrentFreqFromUdev(long[] freqs) {
        if (!HAS_UDEV) {
            return queryCurrentFreqFromSysfs(freqs);
        }
        boolean success = callInArenaBooleanOrDefault(arena -> {
            MemorySegment udev = UdevFunctions.udev_new();
            if (MemorySegment.NULL.equals(udev)) {
                return false;
            }
            try {
                MemorySegment enumerate = UdevFunctions.udev_enumerate_new(udev);
                try {
                    UdevFunctions.addMatchSubsystem(enumerate, "cpu", arena);
                    UdevFunctions.udev_enumerate_scan_devices(enumerate);
                    long max = 0L;
                    for (MemorySegment entry = UdevFunctions
                            .udev_enumerate_get_list_entry(enumerate); !MemorySegment.NULL
                                    .equals(entry); entry = UdevFunctions.udev_list_entry_get_next(entry)) {
                        String syspath = UdevFunctions.getString(UdevFunctions.udev_list_entry_get_name(entry), arena);
                        if (syspath == null) {
                            continue;
                        }
                        int cpu = ParseUtil.getFirstIntValue(syspath);
                        if (cpu >= 0 && cpu < freqs.length) {
                            freqs[cpu] = FileUtil.getLongFromFile(syspath + "/cpufreq/scaling_cur_freq");
                            if (freqs[cpu] == 0) {
                                freqs[cpu] = FileUtil.getLongFromFile(syspath + "/cpufreq/cpuinfo_cur_freq");
                            }
                            if (max < freqs[cpu]) {
                                max = freqs[cpu];
                            }
                        }
                    }
                    if (max > 0L) {
                        for (int i = 0; i < freqs.length; i++) {
                            freqs[i] *= 1000L;
                        }
                        return true;
                    }
                } finally {
                    UdevFunctions.udev_enumerate_unref(enumerate);
                }
            } finally {
                UdevFunctions.udev_unref(udev);
            }
            return false;
        }, LOG, WARN, "Error reading CPU frequencies via udev", false);
        return success || queryCurrentFreqFromSysfs(freqs);
    }

    @Override
    protected long queryMaxFreqFromUdev() {
        if (!HAS_UDEV) {
            return queryMaxFreqFromSysfs();
        }
        long maxFreq = callInArenaLongOrDefault(arena -> {
            MemorySegment udev = UdevFunctions.udev_new();
            if (MemorySegment.NULL.equals(udev)) {
                return -1L;
            }
            try {
                MemorySegment enumerate = UdevFunctions.udev_enumerate_new(udev);
                try {
                    UdevFunctions.addMatchSubsystem(enumerate, "cpu", arena);
                    UdevFunctions.udev_enumerate_scan_devices(enumerate);
                    MemorySegment entry = UdevFunctions.udev_enumerate_get_list_entry(enumerate);
                    if (!MemorySegment.NULL.equals(entry)) {
                        String syspath = UdevFunctions.getString(UdevFunctions.udev_list_entry_get_name(entry), arena);
                        if (syspath != null) {
                            return queryMaxFreqFromCpuFreqPath(
                                    syspath.substring(0, syspath.lastIndexOf('/')) + "/cpufreq");
                        }
                    }
                } finally {
                    UdevFunctions.udev_enumerate_unref(enumerate);
                }
            } finally {
                UdevFunctions.udev_unref(udev);
            }
            return -1L;
        }, LOG, WARN, "Error reading max CPU frequency via udev", -1L);
        return maxFreq < 0L ? queryMaxFreqFromSysfs() : maxFreq;
    }
}
