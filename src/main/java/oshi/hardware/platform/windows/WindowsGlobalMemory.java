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
        // TODO: for now, it returns 0
        return 0;
    }

    @Override
    public long getSwapAvailable() {
        // TODO: for now, it returns 0
        return 0;
    }
    
    @Override
    public JsonObject toJSON() {
        return NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder()).add("available", getAvailable()).add("total", getTotal()).build();
    }
}
