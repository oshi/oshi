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

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.SystemB;
import com.sun.jna.platform.mac.SystemB.VMStatistics;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import java.math.BigDecimal;

import oshi.hardware.GlobalMemory;
import oshi.json.NullAwareJsonObjectBuilder;

/**
 * Memory obtained by host_statistics (vm_stat) and sysctl
 * 
 * @author widdis[at]gmail[dot]com
 */
public class MacGlobalMemory implements GlobalMemory {
    private static final Logger LOG = LoggerFactory.getLogger(MacGlobalMemory.class);

    long totalMemory = 0;

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    @Override
    public long getAvailable() {
        long availableMemory = 0;
        long pageSize = 4096;

        int machPort = SystemB.INSTANCE.mach_host_self();

        LongByReference pPageSize = new LongByReference();
        if (0 != SystemB.INSTANCE.host_page_size(machPort, pPageSize)) {
            LOG.error("Failed to get host page size. Error code: " + Native.getLastError());
            return 0L;
        }
        pageSize = pPageSize.getValue();

        VMStatistics vmStats = new VMStatistics();
        if (0 != SystemB.INSTANCE.host_statistics(machPort, SystemB.HOST_VM_INFO, vmStats,
                new IntByReference(vmStats.size() / SystemB.INT_SIZE))) {
            LOG.error("Failed to get host VM info. Error code: " + Native.getLastError());
            return 0L;
        }
        availableMemory = (vmStats.free_count + vmStats.inactive_count) * pageSize;

        return availableMemory;
    }

    @Override
    public long getTotal() {
        if (this.totalMemory == 0) {
            Pointer pMemSize = new Memory(SystemB.UINT64_SIZE);
            if (0 != SystemB.INSTANCE.sysctlbyname("hw.memsize", pMemSize, new IntByReference(SystemB.UINT64_SIZE),
                    null, 0)) {
                LOG.error("Failed to get memory size. Error code: " + Native.getLastError());
                return 0L;
            }
            this.totalMemory = pMemSize.getLong(0);
        }
        return this.totalMemory;
    }

    @Override
    public long getSwapUsed() {
        Pointer p = new Memory(SystemB.UINT64_SIZE * 4);
        if (0 != SystemB.INSTANCE.sysctlbyname("vm.swapusage", p, new IntByReference(SystemB.UINT64_SIZE * 4), null,
                0)) {
            LOG.error("Failed to get Swap Usage. Error code: " + Native.getLastError());
            return 0L;
        }
        // p points to an array of four longs
        // first 3 elements total, free, used
        return p.getLong(SystemB.UINT64_SIZE * 2);
    }

    @Override
    public long getSwapTotal() {
        Pointer p = new Memory(SystemB.UINT64_SIZE * 4);
        if (0 != SystemB.INSTANCE.sysctlbyname("vm.swapusage", p, new IntByReference(SystemB.UINT64_SIZE * 4), null,
                0)) {
            LOG.error("Failed to get Swap Usage. Error code: " + Native.getLastError());
            return 0L;
        }
        // p points to an array of four longs
        // first 3 elements total, free, used
        return p.getLong(0);
    }

    @Override
    public JsonObject toJSON() {
        return NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder()).add("available", getAvailable()).add("total", getTotal()).add("swapTotal", getSwapTotal()).add("swapused", getSwapUsed()).build();
    }
}
