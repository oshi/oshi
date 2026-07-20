/*
 * Copyright 2019-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.solaris;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.AbstractVirtualMemory;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;

/**
 * Memory obtained by kstat and swap
 */
@ThreadSafe
public class SolarisVirtualMemory extends AbstractVirtualMemory {

    private static final Pattern SWAP_INFO = Pattern.compile(".+\\s(\\d+)K\\s+(\\d+)K$");

    private final SolarisGlobalMemory global;

    // Swap
    private final Supplier<Pair<Long, Long>> usedTotal = memoize(SolarisVirtualMemory::querySwapInfo,
            defaultExpiration());

    private final Supplier<Long> pagesIn = memoize(SolarisVirtualMemory::queryPagesIn, defaultExpiration());

    private final Supplier<Long> pagesOut = memoize(SolarisVirtualMemory::queryPagesOut, defaultExpiration());

    /**
     * Constructor for SolarisVirtualMemory.
     *
     * @param solarisGlobalMemory The parent global memory class instantiating this
     */
    public SolarisVirtualMemory(SolarisGlobalMemory solarisGlobalMemory) {
        this.global = solarisGlobalMemory;
    }

    @Override
    public long getSwapUsed() {
        return usedTotal.get().getA();
    }

    @Override
    public long getSwapTotal() {
        return usedTotal.get().getB();
    }

    @Override
    public long getVirtualMax() {
        return this.global.getTotal() + getSwapTotal();
    }

    @Override
    public long getVirtualInUse() {
        return this.global.getTotal() - this.global.getAvailable() + getSwapUsed();
    }

    @Override
    public long getSwapPagesIn() {
        return pagesIn.get();
    }

    @Override
    public long getSwapPagesOut() {
        return pagesOut.get();
    }

    private static long queryPagesIn() {
        return sumKstatLong(ExecutingCommand.runNative("kstat -p cpu_stat:::pgswapin"));
    }

    private static long queryPagesOut() {
        return sumKstatLong(ExecutingCommand.runNative("kstat -p cpu_stat:::pgswapout"));
    }

    private static Pair<Long, Long> querySwapInfo() {
        return parseSwapInfo(ExecutingCommand.getAnswerAt("swap -lk", 1));
    }

    /**
     * Sums the last long value from each line of {@code kstat -p} output.
     *
     * @param kstat the lines emitted by a {@code kstat -p} command
     * @return the sum of the trailing long values across all lines
     */
    static long sumKstatLong(List<String> kstat) {
        long total = 0L;
        for (String s : kstat) {
            total += ParseUtil.parseLastLong(s, 0L);
        }
        return total;
    }

    /**
     * Parses a single line of {@code swap -lk} output into swap used and swap total values.
     *
     * @param swapLine the line from {@code swap -lk} (typically the second line of output)
     * @return a {@link Pair} of (swap used, swap total) in bytes
     */
    static Pair<Long, Long> parseSwapInfo(String swapLine) {
        long swapTotal = 0L;
        long swapUsed = 0L;
        Matcher m = SWAP_INFO.matcher(swapLine);
        if (m.matches()) {
            swapTotal = ParseUtil.parseLongOrDefault(m.group(1), 0L) << 10;
            swapUsed = swapTotal - (ParseUtil.parseLongOrDefault(m.group(2), 0L) << 10);
        }
        return new Pair<>(swapUsed, swapTotal);
    }
}
