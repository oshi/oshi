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
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.hardware.platform.windows;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase.MEMORYSTATUSEX;

import oshi.hardware.GlobalMemory;

/**
 * Memory obtained by GlobalMemoryStatusEx.
 * 
 * @author dblock[at]dblock[dot]org
 */
public class WindowsGlobalMemory implements GlobalMemory {
    private static final Logger LOG = LoggerFactory.getLogger(WindowsGlobalMemory.class);

    private MEMORYSTATUSEX _memory = new MEMORYSTATUSEX();

    public WindowsGlobalMemory() {
        if (!Kernel32.INSTANCE.GlobalMemoryStatusEx(this._memory)) {
            LOG.error("Failed to Initialize MemoryStatusEx. Error code: {}", Kernel32.INSTANCE.GetLastError());
            this._memory = null;
        }
    }

    @Override
    public long getAvailable() {
        if (this._memory == null) {
            LOG.warn("MemoryStatusEx not initialized. No available memoroy data available");
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
}
