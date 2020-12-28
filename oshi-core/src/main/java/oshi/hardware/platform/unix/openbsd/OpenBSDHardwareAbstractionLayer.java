/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.hardware.platform.unix.openbsd;

import oshi.hardware.*;
import oshi.hardware.common.AbstractHardwareAbstractionLayer;

import java.util.List;

public class OpenBSDHardwareAbstractionLayer extends AbstractHardwareAbstractionLayer {

    @Override
    protected ComputerSystem createComputerSystem() {
        return new OpenBSDComputerSystem();
    }

    /**
     * Instantiates the platform-specific {@link CentralProcessor} object
     *
     * @return platform-specific {@link CentralProcessor} object
     */
    @Override
    protected CentralProcessor createProcessor() {
        return new OpenBSDCentralProcessor();
    }

    /**
     * Instantiates the platform-specific {@link GlobalMemory} object
     *
     * @return platform-specific {@link GlobalMemory} object
     */
    @Override
    protected GlobalMemory createMemory() {
        return null;
    }

    /**
     * Instantiates the platform-specific {@link Sensors} object
     *
     * @return platform-specific {@link Sensors} object
     */
    @Override
    protected Sensors createSensors() {
        return null;
    }

    /**
     * Instantiates an {@code UnmodifiableList} of {@link PowerSource}
     * objects, representing batteries, etc.
     *
     * @return An {@code UnmodifiableList} of PowerSource objects or an empty array
     * if none are present.
     */
    @Override
    public List<PowerSource> getPowerSources() {
        return null;
    }

    /**
     * Instantiates an {@code UnmodifiableList} of {@link HWDiskStore}
     * objects, representing physical hard disks or other similar storage devices.
     *
     * @return An {@code UnmodifiableList} of HWDiskStore objects or an empty list
     * if none are present.
     */
    @Override
    public List<HWDiskStore> getDiskStores() {
        return null;
    }

    /**
     * Gets a list {@link NetworkIF} objects, representing a network interface.
     *
     * @param includeLocalInterfaces whether to include local interfaces (loopback or no hardware
     *                               address) in the result
     * @return An {@code UnmodifiableList} of {@link NetworkIF} objects representing
     * the interfaces
     */
    @Override
    public List<NetworkIF> getNetworkIFs(boolean includeLocalInterfaces) {
        return null;
    }

    /**
     * Instantiates an {@code UnmodifiableList} of {@link Display}
     * objects, representing monitors or other video output devices.
     *
     * @return An {@code UnmodifiableList} of Display objects or an empty array if
     * none are present.
     */
    @Override
    public List<Display> getDisplays() {
        return null;
    }

    /**
     * Instantiates an {@code UnmodifiableList} of {@link UsbDevice}
     * objects, representing devices connected via a usb port (including internal
     * devices).
     * <p>
     * If the value of {@code tree} is true, the top level devices returned from
     * this method are the USB Controllers; connected hubs and devices in its device
     * tree share that controller's bandwidth. If the value of {@code tree} is
     * false, USB devices (not controllers) are listed in a single flat list.
     *
     * @param tree If {@code true}, returns devices connected to the existing device,
     *             accessible via {@link UsbDevice#getConnectedDevices()}. If
     *             {@code false} returns devices as a flat list with no connected
     *             device information.
     * @return An {@code UnmodifiableList} of UsbDevice objects representing
     * (optionally) the USB Controllers and devices connected to them, or an
     * empty array if none are present
     */
    @Override
    public List<UsbDevice> getUsbDevices(boolean tree) {
        return null;
    }

    /**
     * Instantiates an {@code UnmodifiableList} of {@link SoundCard}
     * objects, representing the Sound cards.
     *
     * @return An {@code UnmodifiableList} of SoundCard objects or an empty array if
     * none are present.
     */
    @Override
    public List<SoundCard> getSoundCards() {
        return null;
    }

    /**
     * Instantiates an {@code UnmodifiableList} of
     * {@link GraphicsCard} objects, representing the Graphics cards.
     *
     * @return An {@code UnmodifiableList} of objects or an empty array if none are
     * present.
     */
    @Override
    public List<GraphicsCard> getGraphicsCards() {
        return null;
    }
}
