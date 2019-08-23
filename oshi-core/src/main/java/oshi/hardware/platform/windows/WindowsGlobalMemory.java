/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.hardware.platform.windows;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.Kernel32; // NOSONAR squid:S1191
import com.sun.jna.platform.win32.Psapi;
import com.sun.jna.platform.win32.Psapi.PERFORMANCE_INFORMATION;

import oshi.hardware.VirtualMemory;
import oshi.hardware.common.AbstractGlobalMemory;

/**
 * Memory obtained by Performance Info.
 */
public class WindowsGlobalMemory extends AbstractGlobalMemory {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsGlobalMemory.class);

    private final Supplier<PerfInfo> perfInfo = memoize(this::readPerfInfo, defaultExpiration());

    private final Supplier<VirtualMemory> vm = memoize(this::createVirtualMemory);

    @Override
    public long getAvailable() {
        return perfInfo.get().available;
    }

    @Override
    public long getTotal() {
        return perfInfo.get().total;
    }

    @Override
    public long getPageSize() {
        return perfInfo.get().pageSize;
    }

    @Override
    public VirtualMemory getVirtualMemory() {
        return vm.get();
    }

    private VirtualMemory createVirtualMemory() {
        return new WindowsVirtualMemory(getPageSize());
    }

    private PerfInfo readPerfInfo() {
        long pageSize;
        long memAvailable;
        long memTotal;
        PERFORMANCE_INFORMATION performanceInfo = new PERFORMANCE_INFORMATION();
        if (!Psapi.INSTANCE.GetPerformanceInfo(performanceInfo, performanceInfo.size())) {
            LOG.error("Failed to get Performance Info. Error code: {}", Kernel32.INSTANCE.GetLastError());
            return new PerfInfo(0, 0, 4098);
        }
        pageSize = performanceInfo.PageSize.longValue();
        memAvailable = pageSize * performanceInfo.PhysicalAvailable.longValue();
        memTotal = pageSize * performanceInfo.PhysicalTotal.longValue();
        return new PerfInfo(memTotal, memAvailable, pageSize);
    }

    private static final class PerfInfo {
        private final long total;
        private final long available;
        private final long pageSize;

        private PerfInfo(long total, long available, long pageSize) {
            this.total = total;
            this.available = available;
            this.pageSize = pageSize;
        }
    }
}
