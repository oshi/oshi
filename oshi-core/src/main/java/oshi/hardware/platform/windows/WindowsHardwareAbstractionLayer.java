/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
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
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.hardware.platform.windows;

import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.Display;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;
import oshi.hardware.NetworkIF;
import oshi.hardware.PowerSource;
import oshi.hardware.Sensors;
import oshi.hardware.SoundCard;
import oshi.hardware.UsbDevice;
import oshi.hardware.common.AbstractHardwareAbstractionLayer;
import oshi.util.platform.windows.WmiQueryHandler;
import oshi.util.platform.windows.WmiUtil;

import java.util.List;

public class WindowsHardwareAbstractionLayer extends AbstractHardwareAbstractionLayer {

    private static final long serialVersionUID = 1L;

    private transient final WmiQueryHandler queryHandler;

    private transient WindowsSoundCardCache soundCardCache;

    /**
     * @deprecated TODO: Write javadoc or remove this method.
     */
    @Deprecated
    public WindowsHardwareAbstractionLayer() {
        this(WmiUtil.getShared());
    }

    public WindowsHardwareAbstractionLayer(WmiQueryHandler queryHandler) {
        this.queryHandler = queryHandler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ComputerSystem getComputerSystem() {
        if (this.computerSystem == null) {
            this.computerSystem = new WindowsComputerSystem(queryHandler);
        }
        return this.computerSystem;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GlobalMemory getMemory() {
        if (this.memory == null) {
            this.memory = new WindowsGlobalMemory(queryHandler);
        }
        return this.memory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CentralProcessor getProcessor() {
        if (this.processor == null) {
            this.processor = new WindowsCentralProcessor(queryHandler);
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
    public HWDiskStore[] getDiskStores() {
        return new WindowsDisks(queryHandler).getDisks();
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
            this.sensors = new WindowsSensors(queryHandler);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public UsbDevice[] getUsbDevices(boolean tree) {
        return WindowsUsbDevice.getUsbDevices(tree);
    }

    /**
     * Instantiates an array of {@link SoundCard} objects, representing the
     * Sound cards.
     *
     * @return An array of SoundCard objects or an empty array if none are
     *         present.
     */
    @Override
    public SoundCard[] getSoundCards() {
        WindowsSoundCardCache cache = getSoundCardCache();
        List<WindowsSoundCard> ret = WindowsSoundCard.getSoundCards(queryHandler, cache);
        return ret.toArray(new SoundCard[0]);
    }

    /**
     * Get the {@link WindowsSoundCardCache} instance which is used for the {@link #getSoundCards()} API.
     *
     * @return Return the {@code WindowsSoundCardCache} instance which is used for the {@code getSoundCards()} API.
     */
    public WindowsSoundCardCache getSoundCardCache() {
        if (soundCardCache == null) {
            soundCardCache = createWindowsSoundCardCache();
        }
        return soundCardCache;
    }

    /**
     * Create a new {@link WindowsSoundCardCache} instance.
     *
     * @return A new {@code WindowsSoundCardCache} instance.
     */
    protected WindowsSoundCardCache createWindowsSoundCardCache() {
        return new WindowsSoundCardDefaultCache(queryHandler);
    }
}
