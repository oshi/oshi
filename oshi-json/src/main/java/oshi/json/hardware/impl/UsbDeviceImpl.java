/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors:
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
package oshi.json.hardware.impl;

import java.util.Properties;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import oshi.json.hardware.UsbDevice;
import oshi.json.json.AbstractOshiJsonObject;
import oshi.json.json.NullAwareJsonObjectBuilder;
import oshi.json.util.PropertiesUtil;

/**
 * Wrapper class to implement USBDevice interface with platform-specific objects
 */
public class UsbDeviceImpl extends AbstractOshiJsonObject implements UsbDevice {

    private static final long serialVersionUID = 1L;

    private transient JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    private oshi.hardware.UsbDevice usbDevice;

    /**
     * Creates a new platform-specific USBDevice object wrapping the provided
     * argument
     *
     * @param usbDevice
     *            a platform-specific USBDevice object
     */
    public UsbDeviceImpl(oshi.hardware.UsbDevice usbDevice) {
        this.usbDevice = usbDevice;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return this.usbDevice.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getVendor() {
        return this.usbDevice.getVendor();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getVendorId() {
        return this.usbDevice.getVendorId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getProductId() {
        return this.usbDevice.getProductId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSerialNumber() {
        return this.usbDevice.getSerialNumber();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UsbDevice[] getConnectedDevices() {
        oshi.hardware.UsbDevice[] usbs = this.usbDevice.getConnectedDevices();
        UsbDevice[] usbDevices = new UsbDevice[usbs.length];
        for (int i = 0; i < usbs.length; i++) {
            usbDevices[i] = new UsbDeviceImpl(usbs[i]);
        }
        return usbDevices;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject toJSON(Properties properties) {
        JsonObjectBuilder json = NullAwareJsonObjectBuilder.wrap(this.jsonFactory.createObjectBuilder());
        if (PropertiesUtil.getBoolean(properties, "hardware.usbDevices.name")) {
            json.add("name", getName());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.usbDevices.vendor")) {
            json.add("vendor", getVendor());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.usbDevices.vendorId")) {
            json.add("vendorId", getVendorId());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.usbDevices.productId")) {
            json.add("productId", getProductId());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.usbDevices.serialNumber")) {
            json.add("serialNumber", getSerialNumber());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.usbDevices.connectedDevices")
                && PropertiesUtil.getBoolean(properties, "hardware.usbDevices.tree")) {
            JsonArrayBuilder usbDeviceArrayBuilder = this.jsonFactory.createArrayBuilder();
            for (UsbDevice usbDevice : getConnectedDevices()) {
                usbDeviceArrayBuilder.add(usbDevice.toJSON());
            }
            json.add("connectedDevices", usbDeviceArrayBuilder.build());
        }
        return json.build();
    }

    @Override
    public String toString() {
        return this.usbDevice.toString();
    }
}
