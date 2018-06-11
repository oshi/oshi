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
import oshi.util.platform.windows.PdhUtil;

/**
 * Memory obtained by GlobalMemoryStatusEx.
 *
 * @author dblock[at]dblock[dot]org
 */
public class WindowsGlobalMemory extends AbstractGlobalMemory {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(WindowsGlobalMemory.class);

    private String pdhPagesInputPerSecCounter = null;
    private String pdhPagesOutputPerSecCounter = null;
    private String pdhPagingPercentUsageCounter = null;

    private transient PERFORMANCE_INFORMATION perfInfo = new PERFORMANCE_INFORMATION();

    private long lastUpdate = 0;

    public WindowsGlobalMemory() {
        // Initialize pdh counters
        initPdhCounters();
    }

    /**
     * Initializes PDH Tick Counters
     */
    private void initPdhCounters() {
        pdhPagesInputPerSecCounter = "\\Memory\\Pages Input/sec";
        pdhPagesOutputPerSecCounter = "\\Memory\\Pages Out/sec";

        PdhUtil.addCounter(pdhPagesInputPerSecCounter);
        PdhUtil.addCounter(pdhPagesOutputPerSecCounter);

        pdhPagingPercentUsageCounter = "\\Paging File(_Total)\\% Usage";
        PdhUtil.addCounter(pdhPagingPercentUsageCounter);
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
            this.swapPagesIn = PdhUtil.queryCounter(pdhPagesInputPerSecCounter);
            this.swapPagesOut = PdhUtil.queryCounter(pdhPagesOutputPerSecCounter);

            this.lastUpdate = now;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateSwap() {
        updateMeminfo();
        this.swapUsed = PdhUtil.queryCounter(pdhPagingPercentUsageCounter) * this.pageSize;
    }
}
