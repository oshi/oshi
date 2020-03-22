/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.hardware.platform.linux;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.List;
import java.util.function.Supplier;

import oshi.hardware.common.AbstractVirtualMemory;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.platform.linux.ProcPath;
import oshi.util.tuples.Pair;

/**
 * Memory obtained by /proc/meminfo and /proc/vmstat
 */
public class LinuxVirtualMemory extends AbstractVirtualMemory {

    private final Supplier<Pair<Long, Long>> usedTotal = memoize(LinuxVirtualMemory::queryMemInfo, defaultExpiration());

    private final Supplier<Pair<Long, Long>> inOut = memoize(LinuxVirtualMemory::queryVmStat, defaultExpiration());

    @Override
    public long getSwapUsed() {
        return usedTotal.get().getA();
    }

    @Override
    public long getSwapTotal() {
        return usedTotal.get().getB();
    }

    @Override
    public long getSwapPagesIn() {
        return inOut.get().getA();
    }

    @Override
    public long getSwapPagesOut() {
        return inOut.get().getB();
    }

    private static Pair<Long, Long> queryMemInfo() {
        long swapFree = 0L;
        long swapTotal = 0L;

        List<String> procMemInfo = FileUtil.readFile(ProcPath.MEMINFO);
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
                default:
                    // do nothing with other lines
                    break;
                }
            }
        }
        return new Pair<>(swapTotal - swapFree, swapTotal);
    }

    private static Pair<Long, Long> queryVmStat() {
        long swapPagesIn = 0L;
        long swapPagesOut = 0L;
        List<String> procVmStat = FileUtil.readFile(ProcPath.VMSTAT);
        for (String checkLine : procVmStat) {
            String[] memorySplit = ParseUtil.whitespaces.split(checkLine);
            if (memorySplit.length > 1) {
                switch (memorySplit[0]) {
                case "pgpgin":
                    swapPagesIn = ParseUtil.parseLongOrDefault(memorySplit[1], 0L);
                    break;
                case "pgpgout":
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
     * @param memorySplit
     *            Array of Strings representing the 3 columns of /proc/meminfo
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
