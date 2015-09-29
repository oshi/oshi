/**
 * Oshi (https://github.com/dblock/oshi)
 * 
 * Copyright (c) 2010 - 2015 The Oshi Project Team
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
package oshi.software.os.mac;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native;
import com.sun.jna.platform.mac.SystemB;
import com.sun.jna.ptr.IntByReference;

import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.Memory;
import oshi.hardware.PowerSource;
import oshi.hardware.Processor;
import oshi.software.os.OSFileStore;
import oshi.software.os.mac.local.CentralProcessor;
import oshi.software.os.mac.local.GlobalMemory;
import oshi.software.os.mac.local.MacFileSystem;
import oshi.software.os.mac.local.MacPowerSource;

/**
 * @author alessandro[at]perucchi[dot]org
 * @author widdis[at]gmail[dot]com
 */
public class MacHardwareAbstractionLayer implements HardwareAbstractionLayer {
    private static final Logger LOG = LoggerFactory.getLogger(MacHardwareAbstractionLayer.class);

    private Processor[] _processors;

    private Memory _memory;

    private PowerSource[] _powerSources;

    /*
     * (non-Javadoc)
     * 
     * @see oshi.hardware.HardwareAbstractionLayer#getProcessors()
     */
    @Override
    public Processor[] getProcessors() {
        if (this._processors == null) {
            int nbCPU = 1;
            List<Processor> processors = new ArrayList<>();
            com.sun.jna.Memory pNbCPU = new com.sun.jna.Memory(SystemB.INT_SIZE);
            if (0 != SystemB.INSTANCE.sysctlbyname("hw.logicalcpu", pNbCPU, new IntByReference(SystemB.INT_SIZE), null,
                    0)) {
                LOG.error("Failed to get number of CPUs. Error code: " + Native.getLastError());
                nbCPU = 1;
            } else
                nbCPU = pNbCPU.getInt(0);
            for (int i = 0; i < nbCPU; i++)
                processors.add(new CentralProcessor(i));

            this._processors = processors.toArray(new Processor[0]);
        }
        return this._processors;
    }

    /*
     * (non-Javadoc)
     * 
     * @see oshi.hardware.HardwareAbstractionLayer#getMemory()
     */
    @Override
    public Memory getMemory() {
        if (this._memory == null) {
            this._memory = new GlobalMemory();
        }
        return this._memory;
    }

    @Override
    public PowerSource[] getPowerSources() {
        if (this._powerSources == null) {
            this._powerSources = MacPowerSource.getPowerSources();
        }
        return this._powerSources;
    }

    @Override
    public OSFileStore[] getFileStores() {
        return MacFileSystem.getFileStores();
    }

}
