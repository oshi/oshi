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
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.hardware.platform.windows;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.Kernel32;

import oshi.hardware.common.AbstractGlobalMemory;
import oshi.jna.platform.windows.Psapi;
import oshi.jna.platform.windows.Psapi.PERFORMANCE_INFORMATION;
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

    private long lastUpdate = 0;

    /**
     * Update the performance information no more frequently than every 100ms
     */
    protected void updateMeminfo() {
        long now = System.currentTimeMillis();
        if (now - this.lastUpdate > 100) {
            if (!Psapi.INSTANCE.GetPerformanceInfo(perfInfo, perfInfo.size())) {
                LOG.error("Failed to get Performance Info. Error code: {}", Kernel32.INSTANCE.GetLastError());
                return;
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
        Map<String, List<Long>> usage = WmiUtil.selectUint32sFrom(null, "Win32_PerfRawData_PerfOS_PagingFile",
                "PercentUsage,PercentUsage_Base", "WHERE Name=\"_Total\"");
        if (usage.get("PercentUsage").size() > 0) {
            this.swapUsed = this.swapTotal * usage.get("PercentUsage").get(0) / usage.get("PercentUsage_Base").get(0);
        }
    }
}
