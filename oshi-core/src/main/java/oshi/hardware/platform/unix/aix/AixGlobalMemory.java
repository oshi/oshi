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
package oshi.hardware.platform.unix.aix;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.aix.perfstat.PerfstatMemory;
import oshi.hardware.VirtualMemory;
import oshi.hardware.common.AbstractGlobalMemory;
import oshi.jna.platform.unix.aix.Perfstat.perfstat_memory_total_t;

/**
 * Memory obtained by perfstat_memory_total_t
 */
@ThreadSafe
final class AixGlobalMemory extends AbstractGlobalMemory {

    private final Supplier<perfstat_memory_total_t> perfstatMem = memoize(AixGlobalMemory::queryPerfstat,
            defaultExpiration());

    // AIX has multiple page size units, but for purposes of "pages" in perfstat,
    // the docs specify 4KB pages so we hardcode this
    private static final long PAGESIZE = 4096L;

    private final Supplier<VirtualMemory> vm = memoize(this::createVirtualMemory);

    @Override
    public long getAvailable() {
        return perfstatMem.get().real_avail * PAGESIZE;
    }

    @Override
    public long getTotal() {
        return perfstatMem.get().real_total * PAGESIZE;
    }

    @Override
    public long getPageSize() {
        return PAGESIZE;
    }

    @Override
    public VirtualMemory getVirtualMemory() {
        return vm.get();
    }

    private static perfstat_memory_total_t queryPerfstat() {
        return PerfstatMemory.queryMemoryTotal();
    }

    private VirtualMemory createVirtualMemory() {
        return new AixVirtualMemory(perfstatMem);
    }
}
