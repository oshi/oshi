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

import java.util.List;

import oshi.hardware.UsbDevice;
import oshi.hardware.common.AbstractUsbDevice;

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

    void setProductId(String productId) {
        this.productId = productId;
    }

    void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    void setConnectedDevices(UsbDevice[] connectedDevices) {
        this.connectedDevices = connectedDevices;
    }

    /**
     * @return A mutable list of USB Devices.
     */
    public static List<UsbDevice> getUsbDevices(WindowsUsbDeviceCache cache, boolean tree) {
        return WindowsUsbDeviceCollector.collect(cache, tree);
    }
}
