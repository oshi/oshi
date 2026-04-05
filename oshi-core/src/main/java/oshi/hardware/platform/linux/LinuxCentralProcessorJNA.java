/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.linux;

import static oshi.software.os.linux.LinuxOperatingSystem.HAS_UDEV;

import com.sun.jna.platform.linux.Udev;
import com.sun.jna.platform.linux.Udev.UdevContext;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.tuples.Quartet;

import java.util.List;
import java.util.Map;

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
            return queryCurrentFreqFromUdev(udev, freqs);
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
            return queryMaxFreqFromUdev(udev);
        } finally {
            udev.unref();
        }
    }
}
