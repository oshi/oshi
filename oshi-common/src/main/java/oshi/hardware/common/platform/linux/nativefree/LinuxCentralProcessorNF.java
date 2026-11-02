/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux.nativefree;

import java.util.List;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.platform.linux.LinuxCentralProcessor;
import oshi.software.common.os.linux.nativefree.LinuxOperatingSystemNF;

/**
 * Native-free Linux central processor implementation. Extends {@link LinuxCentralProcessor}, using only sysfs and
 * {@code /proc} sources: it has no udev enumeration and no native {@code getloadavg}, so the base falls back to a sysfs
 * directory scan and {@code /proc/loadavg} respectively.
 */
@ThreadSafe
public final class LinuxCentralProcessorNF extends LinuxCentralProcessor {

    /**
     * Creates a new native-free Linux central processor.
     */
    public LinuxCentralProcessorNF() {
        super(LinuxOperatingSystemNF.hz());
    }

    @Override
    protected @Nullable List<String> enumerateCpuSyspathsViaUdev() {
        // No udev; the base falls back to a sysfs directory scan
        return null;
    }

    @Override
    protected int getloadavgNative(double[] loadavg, int nelem) {
        // No native getloadavg; the base falls back to /proc/loadavg
        return -1;
    }
}
