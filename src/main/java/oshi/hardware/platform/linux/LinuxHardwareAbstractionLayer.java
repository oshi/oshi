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
package oshi.hardware.platform.linux;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.hardware.CentralProcessor;
import oshi.hardware.Display;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.PowerSource;
import oshi.software.os.OSFileStore;
import oshi.software.os.linux.LinuxFileSystem;
import oshi.util.FileUtil;

/**
 * @author alessandro[at]perucchi[dot]org
 */

public class LinuxHardwareAbstractionLayer implements HardwareAbstractionLayer {
    private static final Logger LOG = LoggerFactory.getLogger(LinuxHardwareAbstractionLayer.class);

    private static final String SEPARATOR = "\\s+:\\s";

    private CentralProcessor processor;

    private GlobalMemory _memory;

    @Override
    public GlobalMemory getMemory() {
        if (this._memory == null) {
            this._memory = new LinuxGlobalMemory();
        }
        return this._memory;
    }

    @Override
    public CentralProcessor getProcessor() {

        if (this.processor == null) {
            List<String> cpuInfo = null;
            try {
                cpuInfo = FileUtil.readFile("/proc/cpuinfo");
            } catch (IOException e) {
                LOG.error("Problem with /proc/cpuinfo: {}", e.getMessage());
                return null;
            }
            for (String toBeAnalyzed : cpuInfo) {
                if (toBeAnalyzed.equals("")) {
                    break;
                }
                if (processor == null) {
                    processor = new LinuxCentralProcessor();
                }
                if (toBeAnalyzed.startsWith("model name\t")) {
                    processor.setName(toBeAnalyzed.split(SEPARATOR)[1]);
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
                    processor.setCpu64(found);
                    continue;
                }
                if (toBeAnalyzed.startsWith("cpu family\t")) {
                    processor.setFamily(toBeAnalyzed.split(SEPARATOR)[1]);
                    continue;
                }
                if (toBeAnalyzed.startsWith("model\t")) {
                    processor.setModel(toBeAnalyzed.split(SEPARATOR)[1]);
                    continue;
                }
                if (toBeAnalyzed.startsWith("stepping\t")) {
                    processor.setStepping(toBeAnalyzed.split(SEPARATOR)[1]);
                    continue;
                }
                if (toBeAnalyzed.startsWith("vendor_id")) {
                    processor.setVendor(toBeAnalyzed.split(SEPARATOR)[1]);
                    continue;
                }
            }
        }
        return this.processor;
    }

    @Override
    public PowerSource[] getPowerSources() {
        return LinuxPowerSource.getPowerSources();
    }

    @Override
    public OSFileStore[] getFileStores() {
        return LinuxFileSystem.getFileStores();
    }

    @Override
    public Display[] getDisplays() {
        return LinuxDisplay.getDisplays();
    }

}
