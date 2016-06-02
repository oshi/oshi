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
package oshi.hardware.common;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import oshi.hardware.UsbDevice;
import oshi.json.NullAwareJsonObjectBuilder;

/**
 * A USB device
 * 
 * @author widdis[at]gmail[dot]com
 */
public abstract class AbstractUsbDevice implements UsbDevice {

    private static final long serialVersionUID = 1L;

    protected String name;

    protected String vendor;

    protected String serialNumber;

    protected UsbDevice[] connectedDevices;

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    public AbstractUsbDevice(String name, String vendor, String serialNumber, UsbDevice[] connectedDevices) {
        this.name = name;
        this.vendor = vendor;
        this.serialNumber = serialNumber;
        this.connectedDevices = connectedDevices;
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
    public String getSerialNumber() {
        return this.serialNumber;
    }

    /**
     * {@inheritDoc}
     */
    public UsbDevice[] getConnectedDevices() {
        return this.connectedDevices;
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
                .add("vendor", getVendor()).add("serialNumber", getSerialNumber())
                .add("connectedDevices", usbDeviceArrayBuilder.build()).build();
    }
}
