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
package oshi.hardware.platform.windows;

import oshi.hardware.CentralProcessor;
import oshi.hardware.Display;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;
import oshi.hardware.NetworkIF;
import oshi.hardware.PowerSource;
import oshi.hardware.Sensors;
import oshi.hardware.common.AbstractHardwareAbstractionLayer;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.windows.WindowsFileSystem;

public class WindowsHardwareAbstractionLayer extends AbstractHardwareAbstractionLayer {

    private static final long serialVersionUID = 1L;

    /**
     * {@inheritDoc}
     */
    @Override
    public GlobalMemory getMemory() {
        if (this.memory == null) {
            this.memory = new WindowsGlobalMemory();
        }
        return this.memory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CentralProcessor getProcessor() {
        if (this.processor == null) {
            processor = new WindowsCentralProcessor();
        }
        return this.processor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PowerSource[] getPowerSources() {
        return WindowsPowerSource.getPowerSources();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileSystem getFileSystem() {
        return new WindowsFileSystem();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OSFileStore[] getFileStores() {
        return new WindowsFileSystem().getFileStores();
    }

    @Override
    public HWDiskStore[] getDiskStores() {
        return new WindowsDisks().getDisks();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Display[] getDisplays() {
        return WindowsDisplay.getDisplays();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Sensors getSensors() {
        if (this.sensors == null) {
            this.sensors = new WindowsSensors();
        }
        return this.sensors;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NetworkIF[] getNetworkIFs() {
        return new WindowsNetworks().getNetworks();
    }
}
