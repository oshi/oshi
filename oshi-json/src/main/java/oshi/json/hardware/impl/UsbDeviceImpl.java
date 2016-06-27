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

public class UsbDeviceImpl extends AbstractOshiJsonObject implements UsbDevice {

    private static final long serialVersionUID = 1L;

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    private oshi.hardware.UsbDevice usbDevice;

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
        JsonObjectBuilder json = NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder());
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
            JsonArrayBuilder usbDeviceArrayBuilder = jsonFactory.createArrayBuilder();
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
