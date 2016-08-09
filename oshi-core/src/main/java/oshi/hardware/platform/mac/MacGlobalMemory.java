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
package oshi.hardware.platform.mac;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native; // NOSONAR squid:S1191
import com.sun.jna.platform.mac.SystemB; // NOSONAR squid:S1191
import com.sun.jna.platform.mac.SystemB.VMStatistics; // NOSONAR squid:S1191
import com.sun.jna.ptr.IntByReference; // NOSONAR squid:S1191
import com.sun.jna.ptr.LongByReference; // NOSONAR squid:S1191

import oshi.hardware.common.AbstractGlobalMemory;
import oshi.jna.platform.mac.SystemB.XswUsage;
import oshi.util.platform.mac.SysctlUtil;

/**
 * Memory obtained by host_statistics (vm_stat) and sysctl
 * 
 * @author widdis[at]gmail[dot]com
 */
public class MacGlobalMemory extends AbstractGlobalMemory {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(MacGlobalMemory.class);

    private transient XswUsage xswUsage = new XswUsage();
    private long lastUpdateSwap = 0;

    private transient VMStatistics vmStats = new VMStatistics();
    private long lastUpdateAvail = 0;

    private long pageSize = 4096;

    public MacGlobalMemory() {
        long memory = SysctlUtil.sysctl("hw.memsize", -1L);
        if (memory >= 0) {
            this.memTotal = memory;
        }

        LongByReference pPageSize = new LongByReference();
        if (0 != SystemB.INSTANCE.host_page_size(SystemB.INSTANCE.mach_host_self(), pPageSize)) {
            LOG.error("Failed to get host page size. Error code: " + Native.getLastError());
            return;
        }
        this.pageSize = pPageSize.getValue();
    }

    /**
     * Updates available memory no more often than every 100ms
     */
    @Override
    protected void updateMeminfo() {
        long now = System.currentTimeMillis();
        if (now - lastUpdateAvail > 100) {
            if (0 != SystemB.INSTANCE.host_statistics(SystemB.INSTANCE.mach_host_self(), SystemB.HOST_VM_INFO, vmStats,
                    new IntByReference(vmStats.size() / SystemB.INT_SIZE))) {
                LOG.error("Failed to get host VM info. Error code: " + Native.getLastError());
                return;
            }
            this.memAvailable = (vmStats.free_count + vmStats.inactive_count) * pageSize;
            lastUpdateAvail = now;
        }
    }

    /**
     * Updates swap file stats no more often than every 100ms
     */
    @Override
    protected void updateSwap() {
        long now = System.currentTimeMillis();
        if (now - lastUpdateSwap > 100) {
            if (!SysctlUtil.sysctl("vm.swapusage", xswUsage)) {
                return;
            }
            this.swapUsed = xswUsage.xsu_used;
            this.swapTotal = xswUsage.xsu_total;
            lastUpdateSwap = now;
        }
    }
}
