/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.linux;

import static oshi.software.os.linux.LinuxOperatingSystemFFM.HAS_UDEV;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.linux.UdevFunctions;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.tuples.Quartet;

/**
 * FFM-based Linux central processor implementation. Extends {@link LinuxCentralProcessor}, overriding udev-dependent
 * methods with FFM implementations via {@link UdevFunctions}.
 */
@ThreadSafe
public final class LinuxCentralProcessorFFM extends LinuxCentralProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxCentralProcessorFFM.class);

    @Override
    protected Quartet<List<LogicalProcessor>, List<ProcessorCache>, Map<Integer, Integer>, Map<Integer, String>> readTopologyWithUdev() {
        if (!HAS_UDEV) {
            return readTopologyFromSysfs();
        }
        List<LogicalProcessor> logProcs = new ArrayList<>();
        Set<ProcessorCache> caches = new HashSet<>();
        Map<Integer, Integer> coreEfficiencyMap = new HashMap<>();
        Map<Integer, String> modAliasMap = new HashMap<>();
        try (Arena arena = Arena.ofConfined()) {
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
                        String syspath = UdevFunctions.getString(UdevFunctions.udev_list_entry_get_name(entry), arena);
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
                        logProcs.add(getLogicalProcessorFromSyspath(syspath, caches, modAlias, coreEfficiencyMap,
                                modAliasMap));
                    }
                } finally {
                    UdevFunctions.udev_enumerate_unref(enumerate);
                }
            } finally {
                UdevFunctions.udev_unref(udev);
            }
        } catch (Throwable e) {
            LOG.warn("Error reading CPU topology via udev, falling back to sysfs: {}", e.toString());
            return readTopologyFromSysfs();
        }
        return new Quartet<>(logProcs, orderedProcCaches(caches), coreEfficiencyMap, modAliasMap);
    }

    @Override
    protected boolean queryCurrentFreqFromUdev(long[] freqs) {
        if (!HAS_UDEV) {
            return queryCurrentFreqFromSysfs(freqs);
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment udev = UdevFunctions.udev_new();
            if (MemorySegment.NULL.equals(udev)) {
                return queryCurrentFreqFromSysfs(freqs);
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
        } catch (Throwable e) {
            LOG.warn("Error reading CPU frequencies via udev: {}", e.toString());
        }
        return queryCurrentFreqFromSysfs(freqs);
    }

    @Override
    protected long queryMaxFreqFromUdev() {
        if (!HAS_UDEV) {
            return queryMaxFreqFromSysfs();
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment udev = UdevFunctions.udev_new();
            if (MemorySegment.NULL.equals(udev)) {
                return queryMaxFreqFromSysfs();
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
        } catch (Throwable e) {
            LOG.warn("Error reading max CPU frequency via udev: {}", e.toString());
        }
        return queryMaxFreqFromSysfs();
    }

}
