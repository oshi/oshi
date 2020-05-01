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
package oshi.hardware;

import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * A hardware abstraction layer. Provides access to hardware items such as
 * processors, memory, battery, and disks.
 */
@ThreadSafe
public interface HardwareAbstractionLayer {

    /**
     * Instantiates a {@link oshi.hardware.ComputerSystem} object. This represents
     * the physical hardware, including components such as BIOS/Firmware and a
     * motherboard, logic board, etc.
     *
     * @return a {@link oshi.hardware.ComputerSystem} object.
     */
    ComputerSystem getComputerSystem();

    /**
     * Instantiates a {@link oshi.hardware.CentralProcessor} object. This represents
     * one or more Logical CPUs.
     *
     * @return A {@link oshi.hardware.CentralProcessor} object.
     */
    CentralProcessor getProcessor();

    /**
     * Instantiates a {@link oshi.hardware.GlobalMemory} object.
     *
     * @return A memory object.
     */
    GlobalMemory getMemory();

    /**
     * Instantiates an {@code UnmodifiableList} of {@link oshi.hardware.PowerSource}
     * objects, representing batteries, etc.
     *
     * @return An {@code UnmodifiableList} of PowerSource objects or an empty array
     *         if none are present.
     */
    List<PowerSource> getPowerSources();

    /**
     * Instantiates an {@code UnmodifiableList} of {@link oshi.hardware.HWDiskStore}
     * objects, representing physical hard disks or other similar storage devices
     *
     * @return An {@code UnmodifiableList} of HWDiskStore objects or an empty list
     *         if none are present.
     */
    List<HWDiskStore> getDiskStores();

    /**
     * Gets a list of {@link NetworkIF} objects, representing a network interface
     *
     * @return An {@code UnmodifiableList} of {@link NetworkIF} objects representing
     *         the interfaces
     */
    List<NetworkIF> getNetworkIFs();

    /**
     * Instantiates an {@code UnmodifiableList} of {@link oshi.hardware.Display}
     * objects, representing monitors or other video output devices.
     *
     * @return An {@code UnmodifiableList} of Display objects or an empty array if
     *         none are present.
     */
    List<Display> getDisplays();

    /**
     * Instantiates a {@link oshi.hardware.Sensors} object, representing CPU
     * temperature and fan speed
     *
     * @return A Sensors object
     */
    Sensors getSensors();

    /**
     * Instantiates an {@code UnmodifiableList} of {@link oshi.hardware.UsbDevice}
     * objects, representing devices connected via a usb port (including internal
     * devices).
     * <p>
     * If the value of {@code tree} is true, the top level devices returned from
     * this method are the USB Controllers; connected hubs and devices in its device
     * tree share that controller's bandwidth. If the value of {@code tree} is
     * false, USB devices (not controllers) are listed in a single flat list.
     *
     * @param tree
     *            If {@code true}, returns devices connected to the existing device,
     *            accessible via {@link UsbDevice#getConnectedDevices()}. If
     *            {@code false} returns devices as a flat list with no connected
     *            device information.
     * @return An {@code UnmodifiableList} of UsbDevice objects representing
     *         (optionally) the USB Controllers and devices connected to them, or an
     *         empty array if none are present
     */
    List<UsbDevice> getUsbDevices(boolean tree);

    /**
     * Instantiates an {@code UnmodifiableList} of {@link oshi.hardware.SoundCard}
     * objects, representing the Sound cards.
     *
     * @return An {@code UnmodifiableList} of SoundCard objects or an empty array if
     *         none are present.
     */
    List<SoundCard> getSoundCards();

    /**
     * Instantiates an {@code UnmodifiableList} of
     * {@link oshi.hardware.GraphicsCard} objects, representing the Graphics cards.
     *
     * @return An {@code UnmodifiableList} of objects or an empty array if none are
     *         present.
     */
    List<GraphicsCard> getGraphicsCards();
}
