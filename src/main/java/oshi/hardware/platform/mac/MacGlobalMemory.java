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
 * Contributors:
 * dblock[at]dblock[dot]org
 * alessandro[at]perucchi[dot]org
 * widdis[at]gmail[dot]com
 * enrico[dot]bianchi[at]gmail[dot]com
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.hardware.platform.mac;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native;
import com.sun.jna.platform.mac.SystemB;
import com.sun.jna.platform.mac.SystemB.VMStatistics;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;

import oshi.hardware.GlobalMemory;
import oshi.jna.platform.mac.SystemB.XswUsage;
import oshi.json.NullAwareJsonObjectBuilder;
import oshi.util.platform.mac.SysctlUtil;

/**
 * Memory obtained by host_statistics (vm_stat) and sysctl
 * 
 * @author widdis[at]gmail[dot]com
 */
public class MacGlobalMemory implements GlobalMemory {
    private static final Logger LOG = LoggerFactory.getLogger(MacGlobalMemory.class);

    private XswUsage xswUsage = new XswUsage();

    private long lastUpdateSwap = 0;

    private VMStatistics vmStats = new VMStatistics();

    private long lastUpdateAvail = 0;

    private long totalMemory = 0;

    private long pageSize = 4096;

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    public MacGlobalMemory() {
        updateTotal();

        LongByReference pPageSize = new LongByReference();
        if (0 != SystemB.INSTANCE.host_page_size(SystemB.INSTANCE.mach_host_self(), pPageSize)) {
            LOG.error("Failed to get host page size. Error code: " + Native.getLastError());
            return;
        }
        pageSize = pPageSize.getValue();
    }

    /**
     * Updates total memory
     */
    private void updateTotal() {
        long memory = SysctlUtil.sysctl("hw.memsize", -1L);
        if (memory < 0) {
            return;
        }
        this.totalMemory = memory;
    }

    /**
     * Updates available memory no more often than every 100ms
     */
    private void updateAvailable() {
        long now = System.currentTimeMillis();
        if (now - lastUpdateAvail > 100) {
            if (0 != SystemB.INSTANCE.host_statistics(SystemB.INSTANCE.mach_host_self(), SystemB.HOST_VM_INFO, vmStats,
                    new IntByReference(vmStats.size() / SystemB.INT_SIZE))) {
                LOG.error("Failed to get host VM info. Error code: " + Native.getLastError());
                return;
            }
            lastUpdateAvail = now;
        }
    }

    /**
     * Updates swap file stats no more often than every 100ms
     */
    private void updateSwap() {
        long now = System.currentTimeMillis();
        if (now - lastUpdateSwap > 100) {
            if (!SysctlUtil.sysctl("vm.swapusage", xswUsage)) {
                return;
            }
            lastUpdateSwap = now;
        }
    }

    @Override
    public long getAvailable() {
        updateAvailable();
        return (vmStats.free_count + vmStats.inactive_count) * pageSize;
    }

    @Override
    public long getTotal() {
        if (this.totalMemory == 0) {
            updateTotal();
        }
        return this.totalMemory;
    }

    @Override
    public long getSwapUsed() {
        updateSwap();
        return xswUsage.xsu_used;
    }

    @Override
    public long getSwapTotal() {
        updateSwap();
        return xswUsage.xsu_total;
    }

    @Override
    public JsonObject toJSON() {
        return NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder()).add("available", getAvailable())
                .add("total", getTotal()).add("swapTotal", getSwapTotal()).add("swapused", getSwapUsed()).build();
    }
}
