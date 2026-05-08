/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux.nativefree;

import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.platform.linux.LinuxCentralProcessor;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.tuples.Quartet;

/**
 * Native-free Linux central processor implementation. Extends {@link LinuxCentralProcessor}, overriding udev-dependent
 * methods with sysfs-based alternatives.
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
    protected Quartet<List<LogicalProcessor>, List<ProcessorCache>, Map<Integer, Integer>, Map<Integer, String>> readTopologyWithUdev() {
        return readTopologyFromSysfs();
    }

    @Override
    protected boolean queryCurrentFreqFromUdev(long[] freqs) {
        return queryCurrentFreqFromSysfs(freqs);
    }

    @Override
    protected long queryMaxFreqFromUdev() {
        return queryMaxFreqFromSysfs();
    }
}
