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

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import oshi.json.hardware.UsbDevice;
import oshi.json.json.NullAwareJsonObjectBuilder;

public class UsbDeviceImpl implements UsbDevice {

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

    @Override
    public int compareTo(oshi.hardware.UsbDevice o) {
        return this.usbDevice.compareTo(o);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject toJSON() {
        JsonArrayBuilder usbDeviceArrayBuilder = jsonFactory.createArrayBuilder();
        for (UsbDevice usbDevice : getConnectedDevices()) {
            usbDeviceArrayBuilder.add(usbDevice.toJSON());
        }
        return NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder()).add("name", getName())
                .add("vendor", getVendor()).add("vendorId", getVendorId()).add("productId", getProductId())
                .add("serialNumber", getSerialNumber()).add("connectedDevices", usbDeviceArrayBuilder.build()).build();
    }

    @Override
    public String toString() {
        return this.usbDevice.toString();
    }
}
