/*
 * Copyright 2019-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.List;
import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.AbstractVirtualMemory;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.linux.ProcPath;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

/**
 * Memory obtained by /proc/meminfo and /proc/vmstat
 */
@ThreadSafe
final class LinuxVirtualMemory extends AbstractVirtualMemory {

    private final LinuxGlobalMemory global;

    private final Supplier<Triplet<Long, Long, Long>> usedTotalCommitLim = memoize(LinuxVirtualMemory::queryMemInfo,
            defaultExpiration());

    private final Supplier<Pair<Long, Long>> inOut = memoize(LinuxVirtualMemory::queryVmStat, defaultExpiration());

    /**
     * Constructor for LinuxVirtualMemory.
     *
     * @param linuxGlobalMemory The parent global memory class instantiating this
     */
    LinuxVirtualMemory(LinuxGlobalMemory linuxGlobalMemory) {
        this.global = linuxGlobalMemory;
    }

    @Override
    public long getSwapUsed() {
        return usedTotalCommitLim.get().getA();
    }

    @Override
    public long getSwapTotal() {
        return usedTotalCommitLim.get().getB();
    }

    @Override
    public long getVirtualMax() {
        return usedTotalCommitLim.get().getC();
    }

    @Override
    public long getVirtualInUse() {
        return this.global.getTotal() - this.global.getAvailable() + getSwapUsed();
    }

    @Override
    public long getSwapPagesIn() {
        return inOut.get().getA();
    }

    @Override
    public long getSwapPagesOut() {
        return inOut.get().getB();
    }

    private static Triplet<Long, Long, Long> queryMemInfo() {
        return parseMemInfo(FileUtil.readFile(ProcPath.MEMINFO));
    }

    /**
     * Parses {@code /proc/meminfo} into a triplet of swap (used, total, commitLimit) bytes. Package-private for
     * testing.
     *
     * @param procMemInfo the lines of {@code /proc/meminfo}
     * @return a triplet of (swap used, swap total, commit limit) in bytes
     */
    static Triplet<Long, Long, Long> parseMemInfo(List<String> procMemInfo) {
        long swapFree = 0L;
        long swapTotal = 0L;
        long commitLimit = 0L;

        for (String checkLine : procMemInfo) {
            String[] memorySplit = ParseUtil.whitespaces.split(checkLine);
            if (memorySplit.length > 1) {
                switch (memorySplit[0]) {
                    case "SwapTotal:":
                        swapTotal = parseMeminfo(memorySplit);
                        break;
                    case "SwapFree:":
                        swapFree = parseMeminfo(memorySplit);
                        break;
                    case "CommitLimit:":
                        commitLimit = parseMeminfo(memorySplit);
                        break;
                    default:
                        // do nothing with other lines
                        break;
                }
            }
        }
        return new Triplet<>(swapTotal - swapFree, swapTotal, commitLimit);
    }

    private static Pair<Long, Long> queryVmStat() {
        return parseVmStat(FileUtil.readFile(ProcPath.VMSTAT));
    }

    /**
     * Parses {@code /proc/vmstat} into a pair of (swap pages in, swap pages out). Package-private for testing.
     *
     * @param procVmStat the lines of {@code /proc/vmstat}
     * @return a pair of (pswpin, pswpout)
     */
    static Pair<Long, Long> parseVmStat(List<String> procVmStat) {
        long swapPagesIn = 0L;
        long swapPagesOut = 0L;
        for (String checkLine : procVmStat) {
            String[] memorySplit = ParseUtil.whitespaces.split(checkLine);
            if (memorySplit.length > 1) {
                switch (memorySplit[0]) {
                    case "pswpin":
                        swapPagesIn = ParseUtil.parseLongOrDefault(memorySplit[1], 0L);
                        break;
                    case "pswpout":
                        swapPagesOut = ParseUtil.parseLongOrDefault(memorySplit[1], 0L);
                        break;
                    default:
                        // do nothing with other lines
                        break;
                }
            }
        }
        return new Pair<>(swapPagesIn, swapPagesOut);
    }

    /**
     * Parses lines from the display of /proc/meminfo
     *
     * @param memorySplit Array of Strings representing the 3 columns of /proc/meminfo
     * @return value, multiplied by 1024 if kB is specified
     */
    private static long parseMeminfo(String[] memorySplit) {
        if (memorySplit.length < 2) {
            return 0L;
        }
        long memory = ParseUtil.parseLongOrDefault(memorySplit[1], 0L);
        if (memorySplit.length > 2 && "kB".equals(memorySplit[2])) {
            memory *= 1024;
        }
        return memory;
    }
}
