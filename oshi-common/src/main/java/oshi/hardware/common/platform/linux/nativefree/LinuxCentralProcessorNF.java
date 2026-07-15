/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux.nativefree;

import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.platform.linux.LinuxCentralProcessor;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Native-free Linux central processor implementation. Extends {@link LinuxCentralProcessor}, using only sysfs and
 * {@code /proc} sources: it has no udev enumeration and no native {@code getloadavg}, so the base falls back to a sysfs
 * directory scan and {@code /proc/loadavg} respectively.
 */
@ThreadSafe
public final class LinuxCentralProcessorNF extends LinuxCentralProcessor {

    private static final long USER_HZ = queryUserHz();

    /**
     * Creates a new native-free Linux central processor.
     */
    public LinuxCentralProcessorNF() {
        super(USER_HZ);
    }

    private static long queryUserHz() {
        long hz = ParseUtil.parseLongOrDefault(ExecutingCommand.getFirstAnswer("getconf CLK_TCK"), 0L);
        return hz > 0 ? hz : 100L;
    }

    @Override
    protected List<String> enumerateCpuSyspathsViaUdev() {
        // No udev; the base falls back to a sysfs directory scan
        return null;
    }

    @Override
    protected int getloadavgNative(double[] loadavg, int nelem) {
        // No native getloadavg; the base falls back to /proc/loadavg
        return -1;
    }
}
