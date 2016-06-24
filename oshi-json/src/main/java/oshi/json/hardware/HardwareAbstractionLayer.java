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
package oshi.json.hardware;

import oshi.json.json.OshiJsonObject;

/**
 * A hardware abstraction layer. Provides access to hardware items such as
 * processors, memory, battery, and disks.
 * 
 * @author dblock[at]dblock[dot]org
 */
public interface HardwareAbstractionLayer extends oshi.hardware.HardwareAbstractionLayer, OshiJsonObject {

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
     * connected via a usb port (including internal devices). The top level
     * devices returned from this method are the USB Controllers; connected hubs
     * and devices in its device tree share that controller's bandwidth
     * 
     * @return An array of UsbDevice objects representing the USB Controllers
     *         and devices connected to them, or an empty array if none are
     *         present
     */
    UsbDevice[] getUsbDevices();
}
