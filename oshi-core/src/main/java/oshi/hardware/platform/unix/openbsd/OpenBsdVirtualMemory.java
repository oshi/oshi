/*
 * MIT License
 *
 * Copyright (c) 2010 - 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.hardware.platform.unix.openbsd;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

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
final class OpenBsdVirtualMemory extends AbstractVirtualMemory {

    OpenBsdGlobalMemory global;

    private final Supplier<Triplet<Integer, Integer, Integer>> usedTotalPgin = memoize(
            OpenBsdVirtualMemory::queryVmstat, defaultExpiration());
    private final Supplier<Integer> pgout = memoize(OpenBsdVirtualMemory::queryUvm, defaultExpiration());

    OpenBsdVirtualMemory(OpenBsdGlobalMemory freeBsdGlobalMemory) {
        this.global = freeBsdGlobalMemory;
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
        return usedTotalPgin.get().getC() * global.getPageSize();
    }

    @Override
    public long getSwapPagesOut() {
        return pgout.get() * global.getPageSize();
    }

    private static Triplet<Integer, Integer, Integer> queryVmstat() {
        int used = 0;
        int total = 0;
        int swapIn = 0;
        for (String line : ExecutingCommand.runNative("vmstat -s")) {
            if (line.contains("swap pages in use")) {
                used = ParseUtil.getFirstIntValue(line);
            } else if (line.contains("swap pages")) {
                total = ParseUtil.getFirstIntValue(line);
            } else if (line.contains("pagein operations")) {
                swapIn = ParseUtil.getFirstIntValue(line);
            }
        }
        return new Triplet<>(used, total, swapIn);
    }

    private static int queryUvm() {
        for (String line : ExecutingCommand.runNative("systat -ab uvm")) {
            if (line.contains("pdpageouts")) {
                // First column is non-numeric "Constants" header
                return ParseUtil.getFirstIntValue(line);
            }
        }
        return 0;
    }
}
