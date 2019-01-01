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
package oshi.hardware;

import java.io.Serializable;

/**
 * A hardware abstraction layer. Provides access to hardware items such as
 * processors, memory, battery, and disks.
 *
 * @author dblock[at]dblock[dot]org
 */
public interface HardwareAbstractionLayer extends Serializable {

    /**
     * Instantiates a {@link ComputerSystem} object. This represents the
     * physical hardware, including components such as BIOS/Firmware and a
     * motherboard, logic board, etc.
     *
     * @return a {@link ComputerSystem} object.
     */
    ComputerSystem getComputerSystem();

    /**
     * Instantiates a {@link CentralProcessor} object. This represents one or
     * more Logical CPUs.
     *
     * @return A {@link CentralProcessor} object.
     */
    CentralProcessor getProcessor();

    /**
     * Instantiates a {@link GlobalMemory} object.
     *
     * @return A memory object.
     */
    GlobalMemory getMemory();

    /**
     * Instantiates an array of {@link PowerSource} objects, representing
     * batteries, etc.
     *
     * @return An array of PowerSource objects or an empty array if none are
     *         present.
     */
    PowerSource[] getPowerSources();

    /**
     * Instantiates an array of {@link HWDiskStore} objects, representing a
     * physical hard disk or other similar storage device
     *
     * @return An array of HWDiskStore objects or an empty array if none are
     *         present.
     */
    HWDiskStore[] getDiskStores();

    /**
     * Instantiates an array of {@link NetworkIF} objects, representing a
     * network interface
     *
     * @return An array of HWNetworkStore objects or an empty array if none are
     *         present.
     */
    NetworkIF[] getNetworkIFs();

    /**
     * Instantiates an array of {@link Display} objects, representing monitors
     * or other video output devices.
     *
     * @return An array of Display objects or an empty array if none are
     *         present.
     */
    Display[] getDisplays();

    /**
     * Instantiates a {@link Sensors} object, representing CPU temperature and
     * fan speed
     *
     * @return A Sensors object
     */
    Sensors getSensors();

    /**
     * Instantiates an array of {@link UsbDevice} objects, representing devices
     * connected via a usb port (including internal devices).
     *
     * If the value of tree is true, the top level devices returned from this
     * method are the USB Controllers; connected hubs and devices in its device
     * tree share that controller's bandwidth. If the value of tree is false,
     * USB devices (not controllers) are listed in a single flat array.
     *
     * @param tree
     *            WHether to display devices in a nested tree format from their
     *            controllers
     * @return An array of UsbDevice objects representing (optionally) the USB
     *         Controllers and devices connected to them, or an empty array if
     *         none are present
     */
    UsbDevice[] getUsbDevices(boolean tree);

    /**
     * Instantiates an array of {@link SoundCard} objects, representing the
     * Sound cards.
     *
     * @return An array of SoundCard objects or an empty array if none are
     *         present.
     */
    SoundCard[] getSoundCards();
}
