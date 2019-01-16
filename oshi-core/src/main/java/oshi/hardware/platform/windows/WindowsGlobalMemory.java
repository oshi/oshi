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

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.Kernel32; // NOSONAR squid:S1191
import com.sun.jna.platform.win32.Psapi;
import com.sun.jna.platform.win32.Psapi.PERFORMANCE_INFORMATION;

import oshi.data.windows.PerfCounterQuery;
import oshi.data.windows.PerfCounterQuery.PdhCounterProperty;
import oshi.hardware.common.AbstractGlobalMemory;

/**
 * Memory obtained by GlobalMemoryStatusEx.
 *
 * @author dblock[at]dblock[dot]org
 */
public class WindowsGlobalMemory extends AbstractGlobalMemory {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(WindowsGlobalMemory.class);

    private long lastUpdate = 0L;

    /*
     * For pages in/out
     */
    public enum PageSwapProperty implements PdhCounterProperty {
        PAGESINPUTPERSEC(null, "Pages Input/sec"), //
        PAGESOUTPUTPERSEC(null, "Pages Output/sec");

        private final String instance;
        private final String counter;

        PageSwapProperty(String instance, String counter) {
            this.instance = instance;
            this.counter = counter;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getInstance() {
            return instance;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getCounter() {
            return counter;
        }
    }

    private transient PerfCounterQuery<PageSwapProperty> memoryPerfCounters = new PerfCounterQuery<>(PageSwapProperty.class,
            "Memory", "Win32_PerfRawData_PerfOS_Memory");

    /*
     * For swap file usage
     */
    enum PagingPercentProperty implements PdhCounterProperty {
        PERCENTUSAGE(PerfCounterQuery.TOTAL_INSTANCE, "% Usage");

        private final String instance;
        private final String counter;

        PagingPercentProperty(String instance, String counter) {
            this.instance = instance;
            this.counter = counter;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getInstance() {
            return instance;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getCounter() {
            return counter;
        }
    }

    private transient PerfCounterQuery<PagingPercentProperty> pagingPerfCounters = new PerfCounterQuery<>(
            PagingPercentProperty.class, "Paging File", "Win32_PerfRawData_PerfOS_PagingFile");

    /**
     * Update the performance information no more frequently than every 100ms
     */
    @Override
    protected void updateMeminfo() {
        PERFORMANCE_INFORMATION perfInfo = new PERFORMANCE_INFORMATION();
        long now = System.currentTimeMillis();
        if (now - this.lastUpdate > 100) {
            if (!Psapi.INSTANCE.GetPerformanceInfo(perfInfo, perfInfo.size())) {
                LOG.error("Failed to get Performance Info. Error code: {}", Kernel32.INSTANCE.GetLastError());
                return;
            }
            this.pageSize = perfInfo.PageSize.longValue();
            this.memAvailable = this.pageSize * perfInfo.PhysicalAvailable.longValue();
            this.memTotal = this.pageSize * perfInfo.PhysicalTotal.longValue();
            this.swapTotal = this.pageSize * (perfInfo.CommitLimit.longValue() - perfInfo.PhysicalTotal.longValue());
            if (this.swapTotal > 0) {
                updateSwapCounters();
            }
            this.lastUpdate = now;
        }
    }

    private void updateSwapCounters() {
        Map<PageSwapProperty, Long> valueMap = this.memoryPerfCounters.queryValues();
        this.swapPagesIn = valueMap.getOrDefault(PageSwapProperty.PAGESINPUTPERSEC, 0L);
        this.swapPagesOut = valueMap.getOrDefault(PageSwapProperty.PAGESOUTPUTPERSEC, 0L);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateSwap() {
        updateMeminfo();
        Map<PagingPercentProperty, Long> valueMap = this.pagingPerfCounters.queryValues();
        this.swapUsed = valueMap.getOrDefault(PagingPercentProperty.PERCENTUSAGE, 0L);
    }
}
