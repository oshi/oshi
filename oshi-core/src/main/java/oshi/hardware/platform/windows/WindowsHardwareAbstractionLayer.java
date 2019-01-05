/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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

import java.util.List;

public class WindowsHardwareAbstractionLayer extends AbstractHardwareAbstractionLayer {

    private static final long serialVersionUID = 1L;

    private transient final WmiQueryHandler queryHandler;

    private transient WindowsUsbDeviceCache usbDeviceCache;

    private transient WindowsSoundCardCache soundCardCache;

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
        WindowsUsbDeviceCache cache = getUsbDeviceCache();
        List<UsbDevice> ret = WindowsUsbDevice.getUsbDevices(queryHandler, cache, tree);
        return ret.toArray(new UsbDevice[0]);
    }

    /**
     * Get the {@link WindowsUsbDeviceCache} instance which is used for the {@link #getUsbDevices(boolean)} API.
     *
     * @return Return the {@code WindowsUsbDeviceCache} instance which is used for the {@code getUsbDevices(boolean)} API.
     */
    public WindowsUsbDeviceCache getUsbDeviceCache() {
        if (usbDeviceCache == null) {
            usbDeviceCache = createUsbDeviceCache();
        }
        return usbDeviceCache;
    }

    /**
     * Create a new {@link WindowsUsbDeviceCache} instance.
     *
     * @return A new {@code WindowsUsbDeviceCache} instance.
     */
    protected WindowsUsbDeviceCache createUsbDeviceCache() {
        return new WindowsUsbDeviceDefaultCache(queryHandler);
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
            soundCardCache = createSoundCardCache();
        }
        return soundCardCache;
    }

    /**
     * Create a new {@link WindowsSoundCardCache} instance.
     *
     * @return A new {@code WindowsSoundCardCache} instance.
     */
    protected WindowsSoundCardCache createSoundCardCache() {
        return new WindowsSoundCardDefaultCache(queryHandler);
    }
}
