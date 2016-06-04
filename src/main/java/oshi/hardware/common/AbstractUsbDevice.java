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
            sb.append(" (").append(usbDevice.getVendor()).append(")");
        }
        if (usbDevice.getSerialNumber().length() > 0) {
            sb.append(" [s/n: ").append(usbDevice.getSerialNumber()).append("]");
        }
        for (UsbDevice connected : usbDevice.getConnectedDevices()) {
            sb.append("\n").append(indentUsb(connected, indent + 4));
        }
        return sb.toString();
    }

}
