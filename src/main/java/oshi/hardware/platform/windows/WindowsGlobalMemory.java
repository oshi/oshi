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
package oshi.hardware.platform.windows;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase.MEMORYSTATUSEX;
import java.math.BigDecimal;

import oshi.hardware.GlobalMemory;
import oshi.json.NullAwareJsonObjectBuilder;

/**
 * Memory obtained by GlobalMemoryStatusEx.
 *
 * @author dblock[at]dblock[dot]org
 */
public class WindowsGlobalMemory implements GlobalMemory {
    private static final Logger LOG = LoggerFactory.getLogger(WindowsGlobalMemory.class);

    private MEMORYSTATUSEX _memory = new MEMORYSTATUSEX();

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    public WindowsGlobalMemory() {
        if (!Kernel32.INSTANCE.GlobalMemoryStatusEx(this._memory)) {
            LOG.error("Failed to Initialize MemoryStatusEx. Error code: {}", Kernel32.INSTANCE.GetLastError());
            this._memory = null;
        }
    }

    @Override
    public long getAvailable() {
        if (!Kernel32.INSTANCE.GlobalMemoryStatusEx(this._memory)) {
            LOG.error("Failed to Initialize MemoryStatusEx. Error code: {}", Kernel32.INSTANCE.GetLastError());
            this._memory = null;
            return 0L;
        }
        return this._memory.ullAvailPhys.longValue();
    }

    @Override
    public long getTotal() {
        if (this._memory == null) {
            LOG.warn("MemoryStatusEx not initialized. No total memory data available");
            return 0L;
        }
        return this._memory.ullTotalPhys.longValue();
    }

    @Override
    public long getSwapTotal() {
        // NOTE: other ways to get the Windows page file size are:
        // 1 - The Win32_PageFile WMI class via OLE32 call;
        // 2 - The EnumPageFiles via Psapi call. 

        if (!Kernel32.INSTANCE.GlobalMemoryStatusEx(this._memory)) {
            LOG.error("Failed to Initialize MemoryStatusEx. Error code: {}", Kernel32.INSTANCE.GetLastError());
            this._memory = null;
            return 0L;
        }

        return this._memory.ullTotalPageFile.longValue() - this._memory.ullTotalPhys.longValue();
    }

    @Override
    public long getSwapUsed() {
        long total;
        long available;
        
        if (!Kernel32.INSTANCE.GlobalMemoryStatusEx(this._memory)) {
            LOG.error("Failed to Initialize MemoryStatusEx. Error code: {}", Kernel32.INSTANCE.GetLastError());
            this._memory = null;
            return 0L;
        }

        total = this.getSwapTotal();
        available = this._memory.ullAvailPageFile.longValue() - this._memory.ullAvailPhys.longValue();
        
        return total - available;
    }

    @Override
    public JsonObject toJSON() {
        return NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder()).add("available", getAvailable()).add("total", getTotal()).add("swapTotal", getSwapTotal()).add("swapUsed", getSwapUsed()).build();
    }
}
