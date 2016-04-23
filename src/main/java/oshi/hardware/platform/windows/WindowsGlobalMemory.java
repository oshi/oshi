/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2016 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.hardware.platform.windows;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.ptr.PointerByReference;

import oshi.hardware.common.AbstractGlobalMemory;
import oshi.jna.platform.windows.Pdh;
import oshi.jna.platform.windows.Psapi;
import oshi.jna.platform.windows.Psapi.PERFORMANCE_INFORMATION;
import oshi.util.platform.windows.PdhUtil;

/**
 * Memory obtained by GlobalMemoryStatusEx.
 *
 * @author dblock[at]dblock[dot]org
 */
public class WindowsGlobalMemory extends AbstractGlobalMemory {
    private static final Logger LOG = LoggerFactory.getLogger(WindowsGlobalMemory.class);

    private PERFORMANCE_INFORMATION perfInfo = new PERFORMANCE_INFORMATION();

    private long lastUpdate = 0;

    // Set up Performance Data Helper thread for % pagefile usage
    private PointerByReference pagefileQuery = new PointerByReference();
    private PointerByReference pPagefile = new PointerByReference();;

    public WindowsGlobalMemory() {
        // Open Pagefile query
        if (PdhUtil.openQuery(pagefileQuery)) {
            // \Paging File(_Total)\% Usage
            PdhUtil.addCounter(pagefileQuery, "\\Paging File(_Total)\\% Usage", pPagefile);
            // Initialize by collecting data the first time
            Pdh.INSTANCE.PdhCollectQueryData(pagefileQuery.getValue());
        }

        // Set up hook to close the query on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                Pdh.INSTANCE.PdhCloseQuery(pagefileQuery.getValue());
            }
        });
    }

    /**
     * Update the performance information no more frequently than every 100ms
     */
    protected void updateMeminfo() {
        long now = System.currentTimeMillis();
        if (now - this.lastUpdate > 100) {
            if (!Psapi.INSTANCE.GetPerformanceInfo(perfInfo, perfInfo.size())) {
                LOG.error("Failed to get Performance Info. Error code: {}", Kernel32.INSTANCE.GetLastError());
                this.perfInfo = null;
            }
            this.memAvailable = perfInfo.PageSize.longValue() * perfInfo.PhysicalAvailable.longValue();
            this.memTotal = perfInfo.PageSize.longValue() * perfInfo.PhysicalTotal.longValue();
            this.swapTotal = perfInfo.PageSize.longValue()
                    * (perfInfo.CommitLimit.longValue() - perfInfo.PhysicalTotal.longValue());
            this.lastUpdate = now;
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void updateSwap() {
        updateMeminfo();
        if (!PdhUtil.updateCounters(pagefileQuery)) {
            return;
        }
        // Returns results in 1000's of percent, e.g. 5% is 5000
        // Multiply by page file size and Divide by 100 * 1000
        // Putting division at end avoids need to cast division to double
        this.swapUsed = this.swapTotal * PdhUtil.queryCounter(pPagefile) / 100000;
    }
}
