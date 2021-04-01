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
package oshi.hardware.platform.linux;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.List;
import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.VirtualMemory;
import oshi.hardware.common.AbstractGlobalMemory;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.platform.linux.ProcPath;
import oshi.util.tuples.Pair;

/**
 * Memory obtained by /proc/meminfo and sysinfo.totalram
 */
@ThreadSafe
public final class LinuxGlobalMemory extends AbstractGlobalMemory {

    public static final long PAGE_SIZE = ParseUtil
            .parseLongOrDefault(ExecutingCommand.getFirstAnswer("getconf PAGE_SIZE"), 4096L);

    private final Supplier<Pair<Long, Long>> availTotal = memoize(LinuxGlobalMemory::readMemInfo, defaultExpiration());

    private final Supplier<VirtualMemory> vm = memoize(this::createVirtualMemory);

    @Override
    public long getAvailable() {
        return availTotal.get().getA();
    }

    @Override
    public long getTotal() {
        return availTotal.get().getB();
    }

    @Override
    public long getPageSize() {
        return PAGE_SIZE;
    }

    @Override
    public VirtualMemory getVirtualMemory() {
        return vm.get();
    }

    /**
     * Updates instance variables from reading /proc/meminfo. While most of the
     * information is available in the sysinfo structure, the most accurate
     * calculation of MemAvailable is only available from reading this pseudo-file.
     * The maintainers of the Linux Kernel have indicated this location will be kept
     * up to date if the calculation changes: see
     * https://git.kernel.org/cgit/linux/kernel/git/torvalds/linux.git/commit/?
     * id=34e431b0ae398fc54ea69ff85ec700722c9da773
     * <p>
     * Internally, reading /proc/meminfo is faster than sysinfo because it only
     * spends time populating the memory components of the sysinfo structure.
     *
     * @return A pair containing available and total memory in bytes
     */
    private static Pair<Long, Long> readMemInfo() {
        long memFree = 0L;
        long activeFile = 0L;
        long inactiveFile = 0L;
        long sReclaimable = 0L;

        long memTotal = 0L;
        long memAvailable;

        List<String> procMemInfo = FileUtil.readFile(ProcPath.MEMINFO);
        for (String checkLine : procMemInfo) {
            String[] memorySplit = ParseUtil.whitespaces.split(checkLine, 2);
            if (memorySplit.length > 1) {
                switch (memorySplit[0]) {
                case "MemTotal:":
                    memTotal = ParseUtil.parseDecimalMemorySizeToBinary(memorySplit[1]);
                    break;
                case "MemAvailable:":
                    memAvailable = ParseUtil.parseDecimalMemorySizeToBinary(memorySplit[1]);
                    // We're done!
                    return new Pair<>(memAvailable, memTotal);
                case "MemFree:":
                    memFree = ParseUtil.parseDecimalMemorySizeToBinary(memorySplit[1]);
                    break;
                case "Active(file):":
                    activeFile = ParseUtil.parseDecimalMemorySizeToBinary(memorySplit[1]);
                    break;
                case "Inactive(file):":
                    inactiveFile = ParseUtil.parseDecimalMemorySizeToBinary(memorySplit[1]);
                    break;
                case "SReclaimable:":
                    sReclaimable = ParseUtil.parseDecimalMemorySizeToBinary(memorySplit[1]);
                    break;
                default:
                    // do nothing with other lines
                    break;
                }
            }
        }
        // We didn't find MemAvailable so we estimate from other fields
        return new Pair<>(memFree + activeFile + inactiveFile + sReclaimable, memTotal);
    }

    private VirtualMemory createVirtualMemory() {
        return new LinuxVirtualMemory(this);
    }
}
