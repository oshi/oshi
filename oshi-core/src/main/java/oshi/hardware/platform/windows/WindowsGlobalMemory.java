/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.hardware.platform.windows;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.Kernel32; // NOSONAR
import com.sun.jna.platform.win32.Psapi;
import com.sun.jna.platform.win32.Psapi.PERFORMANCE_INFORMATION;

import oshi.hardware.common.AbstractGlobalMemory;
import oshi.jna.platform.windows.WbemcliUtil.WmiQuery;
import oshi.jna.platform.windows.WbemcliUtil.WmiResult;
import oshi.util.platform.windows.PerfDataUtil;
import oshi.util.platform.windows.PerfDataUtil.PerfCounter;
import oshi.util.platform.windows.WmiUtil;

/**
 * Memory obtained by GlobalMemoryStatusEx.
 *
 * @author dblock[at]dblock[dot]org
 */
public class WindowsGlobalMemory extends AbstractGlobalMemory {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(WindowsGlobalMemory.class);

    private transient PERFORMANCE_INFORMATION perfInfo = new PERFORMANCE_INFORMATION();

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
        // Initialize pdh counters
        initPdhCounters();
    }

    /**
     * Initializes PDH Tick Counters
     */
    private void initPdhCounters() {
        this.pagesInputPerSecCounter = PerfDataUtil.createCounter("Memory", null, "Pages Input/sec");
        this.pagesOutputPerSecCounter = PerfDataUtil.createCounter("Memory", null, "Pages Output/sec");
        if (!PerfDataUtil.addCounterToQuery(pagesInputPerSecCounter)
                || !PerfDataUtil.addCounterToQuery(pagesOutputPerSecCounter)) {
            this.pagesInputPerSecCounter = null;
            this.pagesOutputPerSecCounter = null;
            this.pageSwapsQuery = new WmiQuery<>("Win32_PerfRawData_PerfOS_Memory", PageSwapProperty.class);
        }

        this.pagingPercentUsageCounter = PerfDataUtil.createCounter("Paging File", "_Total", "% Usage");
        if (!PerfDataUtil.addCounterToQuery(pagingPercentUsageCounter)) {
            this.pagingPercentUsageCounter = null;
            this.pagingPercentQuery = new WmiQuery<>("Win32_PerfRawData_PerfOS_PagingFile",
                    PagingPercentProperty.class);
        }
    }

    /**
     * Update the performance information no more frequently than every 100ms
     */
    @Override
    protected void updateMeminfo() {
        long now = System.currentTimeMillis();
        if (now - this.lastUpdate > 100) {
            if (!Psapi.INSTANCE.GetPerformanceInfo(this.perfInfo, this.perfInfo.size())) {
                LOG.error("Failed to get Performance Info. Error code: {}", Kernel32.INSTANCE.GetLastError());
                return;
            }
            this.pageSize = this.perfInfo.PageSize.longValue();
            this.memAvailable = this.pageSize * this.perfInfo.PhysicalAvailable.longValue();
            this.memTotal = this.pageSize * this.perfInfo.PhysicalTotal.longValue();
            this.swapTotal = this.pageSize
                    * (this.perfInfo.CommitLimit.longValue() - this.perfInfo.PhysicalTotal.longValue());
            if (this.swapTotal > 0) {
                if (this.pageSwapsQuery == null) {
                    PerfDataUtil.updateQuery(pagesInputPerSecCounter);
                    this.swapPagesIn = PerfDataUtil.queryCounter(pagesInputPerSecCounter);
                    this.swapPagesOut = PerfDataUtil.queryCounter(pagesOutputPerSecCounter);
                } else {
                    WmiResult<PageSwapProperty> result = WmiUtil.queryWMI(this.pageSwapsQuery);
                    if (result.getResultCount() > 0) {
                        this.swapPagesIn = WmiUtil.getUint32(result, PageSwapProperty.PAGESINPUTPERSEC, 0);
                        this.swapPagesOut = WmiUtil.getUint32(result, PageSwapProperty.PAGESOUTPUTPERSEC, 0);
                    }
                }
            }
            this.lastUpdate = now;
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
                PerfDataUtil.updateQuery(pagingPercentUsageCounter);
                this.swapUsed = PerfDataUtil.queryCounter(pagingPercentUsageCounter) * this.pageSize;
            } else {
                WmiResult<PagingPercentProperty> result = WmiUtil.queryWMI(this.pagingPercentQuery);
                if (result.getResultCount() > 0) {
                    this.swapUsed = WmiUtil.getUint32(result, PagingPercentProperty.PERCENTUSAGE, 0) * this.pageSize;
                }
            }
        }
    }
}
