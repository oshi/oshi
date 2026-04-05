/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.linux;

import static oshi.software.os.linux.LinuxOperatingSystem.HAS_UDEV;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.jna.platform.linux.Udev;
import com.sun.jna.platform.linux.Udev.UdevContext;
import com.sun.jna.platform.linux.Udev.UdevDevice;
import com.sun.jna.platform.linux.Udev.UdevEnumerate;
import com.sun.jna.platform.linux.Udev.UdevListEntry;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.tuples.Quartet;

/**
 * JNA-based Linux central processor implementation. Extends {@link LinuxCentralProcessor}, overriding udev-dependent
 * methods with JNA implementations.
 */
@ThreadSafe
final class LinuxCentralProcessorJNA extends LinuxCentralProcessor {

    @Override
    protected Quartet<List<LogicalProcessor>, List<ProcessorCache>, Map<Integer, Integer>, Map<Integer, String>> readTopologyWithUdev() {
        if (!HAS_UDEV) {
            return readTopologyFromSysfs();
        }
        UdevContext udev = Udev.INSTANCE.udev_new();
        if (udev == null) {
            return readTopologyFromSysfs();
        }
        try {
            return readTopologyFromUdev(udev);
        } finally {
            udev.unref();
        }
    }

    @Override
    protected boolean queryCurrentFreqFromUdev(long[] freqs) {
        if (!HAS_UDEV) {
            return queryCurrentFreqFromSysfs(freqs);
        }
        UdevContext udev = Udev.INSTANCE.udev_new();
        if (udev == null) {
            return queryCurrentFreqFromSysfs(freqs);
        }
        try {
            return readCurrentFreqFromUdev(udev, freqs);
        } finally {
            udev.unref();
        }
    }

    @Override
    protected long queryMaxFreqFromUdev() {
        if (!HAS_UDEV) {
            return queryMaxFreqFromSysfs();
        }
        UdevContext udev = Udev.INSTANCE.udev_new();
        if (udev == null) {
            return queryMaxFreqFromSysfs();
        }
        try {
            return readMaxFreqFromUdev(udev);
        } finally {
            udev.unref();
        }
    }

    private static Quartet<List<LogicalProcessor>, List<ProcessorCache>, Map<Integer, Integer>, Map<Integer, String>> readTopologyFromUdev(
            UdevContext udev) {
        List<LogicalProcessor> logProcs = new ArrayList<>();
        Set<ProcessorCache> caches = new HashSet<>();
        Map<Integer, Integer> coreEfficiencyMap = new HashMap<>();
        Map<Integer, String> modAliasMap = new HashMap<>();
        UdevEnumerate enumerate = udev.enumerateNew();
        try {
            enumerate.addMatchSubsystem("cpu");
            enumerate.scanDevices();
            for (UdevListEntry entry = enumerate.getListEntry(); entry != null; entry = entry.getNext()) {
                String syspath = entry.getName(); // /sys/devices/system/cpu/cpuX
                UdevDevice device = udev.deviceNewFromSyspath(syspath);
                String modAlias = null;
                if (device != null) {
                    try {
                        modAlias = device.getPropertyValue("MODALIAS");
                    } finally {
                        device.unref();
                    }
                }
                logProcs.add(getLogicalProcessorFromSyspath(syspath, caches, modAlias, coreEfficiencyMap, modAliasMap));
            }
        } finally {
            enumerate.unref();
        }
        return new Quartet<>(logProcs, orderedProcCaches(caches), coreEfficiencyMap, modAliasMap);
    }

    private static boolean readCurrentFreqFromUdev(UdevContext udev, long[] freqs) {
        long max = 0L;
        UdevEnumerate enumerate = udev.enumerateNew();
        try {
            enumerate.addMatchSubsystem("cpu");
            enumerate.scanDevices();
            for (UdevListEntry entry = enumerate.getListEntry(); entry != null; entry = entry.getNext()) {
                String syspath = entry.getName();
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
        } finally {
            enumerate.unref();
        }
        if (max > 0L) {
            for (int i = 0; i < freqs.length; i++) {
                freqs[i] *= 1000L;
            }
            return true;
        }
        return false;
    }

    private static long readMaxFreqFromUdev(UdevContext udev) {
        UdevEnumerate enumerate = udev.enumerateNew();
        try {
            enumerate.addMatchSubsystem("cpu");
            enumerate.scanDevices();
            UdevListEntry entry = enumerate.getListEntry();
            if (entry != null) {
                String syspath = entry.getName();
                return queryMaxFreqFromCpuFreqPath(syspath.substring(0, syspath.lastIndexOf('/')) + "/cpufreq");
            }
        } finally {
            enumerate.unref();
        }
        return -1L;
    }
}
