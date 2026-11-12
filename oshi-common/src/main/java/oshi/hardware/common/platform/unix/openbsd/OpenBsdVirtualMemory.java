/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.openbsd;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.List;
import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.AbstractVirtualMemory;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.tuples.Triplet;

/**
 * Memory info on OpenBSD
 */
@ThreadSafe
public class OpenBsdVirtualMemory extends AbstractVirtualMemory {

    private final OpenBsdGlobalMemory global;

    private final Supplier<Triplet<Long, Long, Long>> usedTotalPgin = memoize(OpenBsdVirtualMemory::queryVmstat,
            defaultExpiration());
    private final Supplier<Long> pgout = memoize(OpenBsdVirtualMemory::queryUvm, defaultExpiration());

    /**
     * Constructs a new {@code OpenBsdVirtualMemory}.
     *
     * @param global the backing global memory
     */
    public OpenBsdVirtualMemory(OpenBsdGlobalMemory global) {
        this.global = global;
    }

    @Override
    public long getSwapUsed() {
        return usedTotalPgin.get().getA() * global.getPageSize();
    }

    @Override
    public long getSwapTotal() {
        return usedTotalPgin.get().getB() * global.getPageSize();
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
        return usedTotalPgin.get().getC();
    }

    @Override
    public long getSwapPagesOut() {
        return pgout.get();
    }

    private static Triplet<Long, Long, Long> queryVmstat() {
        return parseVmstat(ExecutingCommand.runNative("vmstat -s"));
    }

    /**
     * Parses the swap counters from {@code vmstat -s}, each line of which is a count followed by its label.
     *
     * @param vmstat the lines of {@code vmstat -s} output
     * @return swap pages in use, total swap pages, and pagein operations
     */
    static Triplet<Long, Long, Long> parseVmstat(List<String> vmstat) {
        long used = 0L;
        long total = 0L;
        long swapIn = 0L;
        for (String line : vmstat) {
            if (line.contains("swap pages in use")) {
                used = parseLeadingLong(line);
            } else if (line.contains("swap pages")) {
                total = parseLeadingLong(line);
            } else if (line.contains("pagein operations")) {
                swapIn = parseLeadingLong(line);
            }
        }
        return new Triplet<>(used, total, swapIn);
    }

    private static long queryUvm() {
        // Widen the batch output so no counter is abbreviated with a K/M/G suffix
        return parseUvmCounter(ExecutingCommand.runNative("systat -ab -w 200 uvm"), "pdpageouts");
    }

    /**
     * Parses one counter from {@code systat -b uvm}. Each row holds up to three value and name pairs. The value
     * precedes its name and is left blank when zero, and the name is printed in upper case when the value changed since
     * the previous sample, which on the single sample of batch mode is any nonzero value.
     *
     * @param uvm  the lines of {@code systat -b uvm} output
     * @param name the counter name
     * @return the counter value, or 0 if it is absent or blank
     */
    static long parseUvmCounter(List<String> uvm, String name) {
        for (String line : uvm) {
            String[] tokens = ParseUtil.whitespaces.split(line.trim(), -1);
            for (int i = 0; i < tokens.length; i++) {
                if (tokens[i].equalsIgnoreCase(name)) {
                    return i > 0 ? ParseUtil.parseLongOrDefault(tokens[i - 1], 0L) : 0L;
                }
            }
        }
        return 0L;
    }

    private static long parseLeadingLong(String line) {
        return ParseUtil.parseLongOrDefault(ParseUtil.whitespaces.split(line.trim(), 2)[0], 0L);
    }
}
