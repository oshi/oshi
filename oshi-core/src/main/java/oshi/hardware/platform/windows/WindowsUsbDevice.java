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

import oshi.hardware.UsbDevice;
import oshi.hardware.common.AbstractUsbDevice;
import oshi.util.platform.windows.WmiQueryHandler;

import java.util.List;

public class WindowsUsbDevice extends AbstractUsbDevice {

    private static final long serialVersionUID = 2L;

    enum USBControllerProperty {
        PNPDEVICEID;
    }

    enum PnPEntityProperty {
        NAME, MANUFACTURER, PNPDEVICEID;
    }

    enum DiskDriveProperty {
        PNPDEVICEID, SERIALNUMBER;
    }

    public WindowsUsbDevice(String name, String vendor, String vendorId, String productId, String serialNumber,
            UsbDevice[] connectedDevices) {
        super(name, vendor, vendorId, productId, serialNumber, connectedDevices);
    }

    void setName(String name) {
        this.name = name;
    }

    void setVendorId(String vendorId) {
        this.vendorId = vendorId;
    }

    void setProductId(String productId ) {
        this.productId = productId;
    }

    void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    void setConnectedDevices(UsbDevice[] connectedDevices) {
        this.connectedDevices = connectedDevices;
    }

    /**
     * @return An mutable list.
     */
    public static List<UsbDevice> getUsbDevices(WmiQueryHandler queryHandler, WindowsUsbDeviceCache cache, boolean tree) {
        return WindowsUsbDeviceCollector.collect(queryHandler, cache, tree);
    }
}
