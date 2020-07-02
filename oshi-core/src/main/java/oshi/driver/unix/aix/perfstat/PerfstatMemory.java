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
package oshi.driver.unix.aix.perfstat;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.platform.unix.aix.Perfstat;
import oshi.jna.platform.unix.aix.Perfstat.perfstat_memory_total_t;

/**
 * Utility to query performance stats for memory
 */
@ThreadSafe
public final class PerfstatMemory {

    private static final Perfstat PERF = Perfstat.INSTANCE;

    private PerfstatMemory() {
    }

    /**
     * Queries perfstat_memory_total for total memory usage statistics
     *
     * @return usage statistics
     */
    public static perfstat_memory_total_t queryMemoryTotal() {
        perfstat_memory_total_t memory = new perfstat_memory_total_t();
        int ret = PERF.perfstat_memory_total(null, memory, memory.size(), 1);
        if (ret > 0) {
            return memory;
        }
        return new perfstat_memory_total_t();
    }
}
