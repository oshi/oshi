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
package oshi.hardware.common;

import oshi.hardware.CentralProcessor;
import oshi.hardware.Display;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.PowerSource;
import oshi.hardware.Sensors;
import oshi.hardware.UsbDevice;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;

public abstract class AbstractHardwareAbstractionLayer implements HardwareAbstractionLayer {

    private static final long serialVersionUID = 1L;

    protected CentralProcessor processor;

    protected GlobalMemory memory;

    protected Sensors sensors;

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract GlobalMemory getMemory();

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract CentralProcessor getProcessor();

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract PowerSource[] getPowerSources();

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract FileSystem getFileSystem();

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract OSFileStore[] getFileStores();

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract HWDiskStore[] getDiskStores();

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract Display[] getDisplays();

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract Sensors getSensors();

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract UsbDevice[] getUsbDevices();
}
