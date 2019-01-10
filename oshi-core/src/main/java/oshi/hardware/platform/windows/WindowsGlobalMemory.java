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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.Kernel32; // NOSONAR squid:S1191
import com.sun.jna.platform.win32.Psapi;
import com.sun.jna.platform.win32.Psapi.PERFORMANCE_INFORMATION;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.hardware.common.AbstractGlobalMemory;
import oshi.util.platform.windows.PerfDataUtil;
import oshi.util.platform.windows.PerfDataUtil.PerfCounter;
import oshi.util.platform.windows.WmiQueryHandler;
import oshi.util.platform.windows.WmiUtil;

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
    enum PageSwapProperty {
        PAGESINPUTPERSEC, PAGESOUTPUTPERSEC;
    }

    // Only one of these will be used
    private transient PerfCounter pagesInputPerSecCounter = null;
    private transient PerfCounter pagesOutputPerSecCounter = null;
    private transient WmiQuery<PageSwapProperty> pageSwapsQuery = null;

    /*
     * For swap file usage
     */
    enum PagingPercentProperty {
        PERCENTUSAGE;
    }

    // Only one of these will be used
    private transient PerfCounter pagingPercentUsageCounter = null;
    private transient WmiQuery<PagingPercentProperty> pagingPercentQuery = null;

    public WindowsGlobalMemory() {
        initPdhCounters();
    }

    /**
     * Initializes PDH Tick Counters
     */
    private void initPdhCounters() {
        this.pagesInputPerSecCounter = PerfDataUtil.createCounter("Memory", null, "Pages Input/sec");
        this.pagesOutputPerSecCounter = PerfDataUtil.createCounter("Memory", null, "Pages Output/sec");
        if (!PerfDataUtil.addCounterToQuery(this.pagesInputPerSecCounter)
                || !PerfDataUtil.addCounterToQuery(this.pagesOutputPerSecCounter)) {
            initWmiSwapIoQuery();
        }

        this.pagingPercentUsageCounter = PerfDataUtil.createCounter("Paging File", "_Total", "% Usage");
        if (!PerfDataUtil.addCounterToQuery(this.pagingPercentUsageCounter)) {
            initWmiSwapUsageQuery();
        }
    }

    /**
     * Nulls PDH counters and sets up WMI query for page swap counters.
     */
    private void initWmiSwapIoQuery() {
        PerfDataUtil.removeCounterFromQuery(this.pagesInputPerSecCounter);
        this.pagesInputPerSecCounter = null;
        PerfDataUtil.removeCounterFromQuery(this.pagesOutputPerSecCounter);
        this.pagesOutputPerSecCounter = null;
        this.pageSwapsQuery = new WmiQuery<>("Win32_PerfRawData_PerfOS_Memory", PageSwapProperty.class);
    }

    /**
     * Nulls PDH counter and sets up WMI query for pagefile usage.
     */
    private void initWmiSwapUsageQuery() {
        PerfDataUtil.removeCounterFromQuery(this.pagingPercentUsageCounter);
        this.pagingPercentUsageCounter = null;
        this.pagingPercentQuery = new WmiQuery<>("Win32_PerfRawData_PerfOS_PagingFile", PagingPercentProperty.class);
    }

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
        if (this.pageSwapsQuery == null) {
            long timeStamp = PerfDataUtil.updateQuery(this.pagesInputPerSecCounter);
            if (timeStamp > 0) {
                this.swapPagesIn = PerfDataUtil.queryCounter(this.pagesInputPerSecCounter);
                this.swapPagesOut = PerfDataUtil.queryCounter(this.pagesOutputPerSecCounter);
            } else {
                // Zero timestamp means update failed after muliple
                // attempts; fallback to WMI
                initWmiSwapIoQuery();
            }
        }
        if (this.pageSwapsQuery != null) {
            WmiResult<PageSwapProperty> result = WmiQueryHandler.getInstance().queryWMI(this.pageSwapsQuery);
            if (result.getResultCount() > 0) {
                this.swapPagesIn = WmiUtil.getUint32(result, PageSwapProperty.PAGESINPUTPERSEC, 0);
                this.swapPagesOut = WmiUtil.getUint32(result, PageSwapProperty.PAGESOUTPUTPERSEC, 0);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateSwap() {
        updateMeminfo();
        if (this.swapTotal > 0) {
            if (this.pagingPercentQuery == null) {
                long timeStamp = PerfDataUtil.updateQuery(this.pagingPercentUsageCounter);
                if (timeStamp > 0) {
                    this.swapUsed = PerfDataUtil.queryCounter(this.pagingPercentUsageCounter) * this.pageSize;
                } else {
                    // Zero timestamp means update failed after muliple
                    // attempts; fallback to WMI
                    initWmiSwapUsageQuery();
                }
            }
            if (this.pagingPercentQuery != null) {
                WmiResult<PagingPercentProperty> result = WmiQueryHandler.getInstance()
                        .queryWMI(this.pagingPercentQuery);
                if (result.getResultCount() > 0) {
                    this.swapUsed = WmiUtil.getUint32(result, PagingPercentProperty.PERCENTUSAGE, 0) * this.pageSize;
                }
            }
        }
    }
}
