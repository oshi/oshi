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

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;

import oshi.hardware.CentralProcessor;
import oshi.hardware.Display;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.PowerSource;
import oshi.software.os.OSFileStore;
import oshi.software.os.windows.WindowsFileSystem;

public class WindowsHardwareAbstractionLayer implements HardwareAbstractionLayer {

    private CentralProcessor processor;

    private GlobalMemory _memory;

    @Override
    public GlobalMemory getMemory() {
        if (this._memory == null) {
            this._memory = new WindowsGlobalMemory();
        }
        return this._memory;
    }

    @Override
    public CentralProcessor getProcessor() {
        if (this.processor == null) {
            processor = new WindowsCentralProcessor();
            final String cpuRegistryRoot = "HARDWARE\\DESCRIPTION\\System\\CentralProcessor";
            String[] processorIds = Advapi32Util.registryGetKeys(WinReg.HKEY_LOCAL_MACHINE, cpuRegistryRoot);
            if (processorIds.length > 0) {
                String cpuRegistryPath = cpuRegistryRoot + "\\" + processorIds[0];
                processor.setIdentifier(
                        Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, cpuRegistryPath, "Identifier"));
                processor.setName(Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, cpuRegistryPath,
                        "ProcessorNameString"));
                processor.setVendor(Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, cpuRegistryPath,
                        "VendorIdentifier"));
            }
        }
        return this.processor;
    }

    @Override
    public PowerSource[] getPowerSources() {
        return WindowsPowerSource.getPowerSources();
    }

    @Override
    public OSFileStore[] getFileStores() {
        return WindowsFileSystem.getFileStores();
    }

    @Override
    public Display[] getDisplays() {
        return WindowsDisplay.getDisplays();
    }
}
