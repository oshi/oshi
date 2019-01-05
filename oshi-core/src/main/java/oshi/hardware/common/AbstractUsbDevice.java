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
package oshi.hardware.common;

import java.util.Arrays;

import oshi.hardware.UsbDevice;

/**
 * A USB device
 *
 * @author widdis[at]gmail[dot]com
 */
public abstract class AbstractUsbDevice implements UsbDevice {

    private static final long serialVersionUID = 2L;

    protected String name;

    protected String vendor;

    protected String vendorId;

    protected String productId;

    protected String serialNumber;

    protected UsbDevice[] connectedDevices;

    public AbstractUsbDevice(String name, String vendor, String vendorId, String productId, String serialNumber,
            UsbDevice[] connectedDevices) {
        this.name = name;
        this.vendor = vendor;
        this.vendorId = vendorId;
        this.productId = productId;
        this.serialNumber = serialNumber;
        this.connectedDevices = Arrays.copyOf(connectedDevices, connectedDevices.length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getVendor() {
        return this.vendor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getVendorId() {
        return this.vendorId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getProductId() {
        return this.productId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSerialNumber() {
        return this.serialNumber;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UsbDevice[] getConnectedDevices() {
        return Arrays.copyOf(this.connectedDevices, this.connectedDevices.length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(UsbDevice usb) {
        // Naturally sort by device name
        return getName().compareTo(usb.getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return indentUsb(this, 1);
    }

    /**
     * Helper method for indenting chained USB devices
     *
     * @param usbDevice
     *            A USB device to print
     * @param indent
     *            number of spaces to indent
     */
    private static String indentUsb(UsbDevice usbDevice, int indent) {
        String indentFmt = indent > 2 ? String.format("%%%ds|-- ", indent - 4) : String.format("%%%ds", indent);
        StringBuilder sb = new StringBuilder(String.format(indentFmt, ""));
        sb.append(usbDevice.getName());
        if (usbDevice.getVendor().length() > 0) {
            sb.append(" (").append(usbDevice.getVendor()).append(')');
        }
        if (usbDevice.getSerialNumber().length() > 0) {
            sb.append(" [s/n: ").append(usbDevice.getSerialNumber()).append(']');
        }
        for (UsbDevice connected : usbDevice.getConnectedDevices()) {
            sb.append('\n').append(indentUsb(connected, indent + 4));
        }
        return sb.toString();
    }

}
