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
package oshi.software.os.linux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.Memory;
import oshi.hardware.PowerSource;
import oshi.hardware.Processor;
import oshi.software.os.OSFileStore;
import oshi.software.os.linux.proc.CentralProcessor;
import oshi.software.os.linux.proc.GlobalMemory;
import oshi.software.os.linux.proc.LinuxFileSystem;
import oshi.software.os.linux.proc.LinuxPowerSource;
import oshi.util.FileUtil;

/**
 * @author alessandro[at]perucchi[dot]org
 */

public class LinuxHardwareAbstractionLayer implements HardwareAbstractionLayer {
    private static final Logger LOG = LoggerFactory.getLogger(LinuxHardwareAbstractionLayer.class);

    private static final String SEPARATOR = "\\s+:\\s";

    private Processor[] _processors;

    private Memory _memory;

    @Override
    public Memory getMemory() {
        if (this._memory == null) {
            this._memory = new GlobalMemory();
        }
        return this._memory;
    }

    @Override
    public Processor[] getProcessors() {

        if (this._processors == null) {
            List<Processor> processors = new ArrayList<>();
            List<String> cpuInfo = null;
            try {
                cpuInfo = FileUtil.readFile("/proc/cpuinfo");
            } catch (IOException e) {
                LOG.error("Problem with /proc/cpuinfo: {}", e.getMessage());
                return null;
            }
            CentralProcessor cpu = null;
            int numCPU = 0;
            for (String toBeAnalyzed : cpuInfo) {
                if (toBeAnalyzed.equals("")) {
                    if (cpu != null) {
                        processors.add(cpu);
                    }
                    cpu = null;
                    continue;
                }
                if (cpu == null) {
                    cpu = new CentralProcessor(numCPU++);
                }
                if (toBeAnalyzed.startsWith("model name\t")) {
                    cpu.setName(toBeAnalyzed.split(SEPARATOR)[1]);
                    continue;
                }
                if (toBeAnalyzed.startsWith("flags\t")) {
                    String[] flags = toBeAnalyzed.split(SEPARATOR)[1].split(" ");
                    boolean found = false;
                    for (String flag : flags) {
                        if (flag.equalsIgnoreCase("LM")) {
                            found = true;
                            break;
                        }
                    }
                    cpu.setCpu64(found);
                    continue;
                }
                if (toBeAnalyzed.startsWith("cpu family\t")) {
                    cpu.setFamily(toBeAnalyzed.split(SEPARATOR)[1]);
                    continue;
                }
                if (toBeAnalyzed.startsWith("model\t")) {
                    cpu.setModel(toBeAnalyzed.split(SEPARATOR)[1]);
                    continue;
                }
                if (toBeAnalyzed.startsWith("stepping\t")) {
                    cpu.setStepping(toBeAnalyzed.split(SEPARATOR)[1]);
                    continue;
                }
                if (toBeAnalyzed.startsWith("vendor_id")) {
                    cpu.setVendor(toBeAnalyzed.split(SEPARATOR)[1]);
                    continue;
                }
            }
            if (cpu != null) {
                processors.add(cpu);
            }
            this._processors = processors.toArray(new Processor[0]);
        }

        return this._processors;
    }

    @Override
    public PowerSource[] getPowerSources() {
        return LinuxPowerSource.getPowerSources();
    }

    @Override
    public OSFileStore[] getFileStores() {
        return LinuxFileSystem.getFileStores();
    }

}
