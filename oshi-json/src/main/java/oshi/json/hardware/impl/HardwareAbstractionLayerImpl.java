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

import oshi.json.hardware.CentralProcessor;
import oshi.json.hardware.Display;
import oshi.json.hardware.GlobalMemory;
import oshi.json.hardware.HWDiskStore;
import oshi.json.hardware.HardwareAbstractionLayer;
import oshi.json.hardware.NetworkIF;
import oshi.json.hardware.PowerSource;
import oshi.json.hardware.Sensors;
import oshi.json.hardware.UsbDevice;
import oshi.json.json.NullAwareJsonObjectBuilder;

public class HardwareAbstractionLayerImpl implements HardwareAbstractionLayer {

    private static final long serialVersionUID = 1L;

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    private oshi.hardware.HardwareAbstractionLayer hal;

    private CentralProcessor processor;
    private GlobalMemory memory;
    private Sensors sensors;

    public HardwareAbstractionLayerImpl(oshi.hardware.HardwareAbstractionLayer hardware) {
        this.hal = hardware;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CentralProcessor getProcessor() {
        if (this.processor == null) {
            this.processor = new CentralProcessorImpl(this.hal.getProcessor());
        }
        return this.processor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GlobalMemory getMemory() {
        if (this.memory == null) {
            this.memory = new GlobalMemoryImpl(this.hal.getMemory());
        }
        return this.memory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PowerSource[] getPowerSources() {
        oshi.hardware.PowerSource[] ps = this.hal.getPowerSources();
        PowerSource[] powerSources = new PowerSource[ps.length];
        for (int i = 0; i < ps.length; i++) {
            powerSources[i] = new PowerSourceImpl(ps[i]);
        }
        return powerSources;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HWDiskStore[] getDiskStores() {
        oshi.hardware.HWDiskStore[] ds = this.hal.getDiskStores();
        HWDiskStore[] diskStores = new HWDiskStore[ds.length];
        for (int i = 0; i < ds.length; i++) {
            diskStores[i] = new HWDiskStore(ds[i].getName(), ds[i].getModel(), ds[i].getSerial(), ds[i].getSize(),
                    ds[i].getReads(), ds[i].getWrites());
        }
        return diskStores;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NetworkIF[] getNetworkIFs() {
        oshi.hardware.NetworkIF[] ifs = this.hal.getNetworkIFs();
        NetworkIF[] networkIFs = new NetworkIF[ifs.length];
        for (int i = 0; i < ifs.length; i++) {
            networkIFs[i] = new NetworkIF();
            networkIFs[i].setNetworkInterface(ifs[i].getNetworkInterface());
            networkIFs[i].setBytesRecv(ifs[i].getBytesRecv());
            networkIFs[i].setBytesSent(ifs[i].getBytesSent());
            networkIFs[i].setPacketsRecv(ifs[i].getPacketsRecv());
            networkIFs[i].setPacketsSent(ifs[i].getPacketsSent());
            networkIFs[i].setSpeed(ifs[i].getSpeed());
        }
        return networkIFs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Display[] getDisplays() {
        oshi.hardware.Display[] ds = this.hal.getDisplays();
        Display[] displays = new Display[ds.length];
        for (int i = 0; i < ds.length; i++) {
            displays[i] = new DisplayImpl(ds[i]);
        }
        return displays;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Sensors getSensors() {
        if (this.sensors == null) {
            this.sensors = new SensorsImpl(this.hal.getSensors());
        }
        return this.sensors;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UsbDevice[] getUsbDevices(boolean tree) {
        oshi.hardware.UsbDevice[] usbs = this.hal.getUsbDevices(tree);
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
    public JsonObject toJSON() {
        JsonArrayBuilder powerSourceArrayBuilder = jsonFactory.createArrayBuilder();
        for (PowerSource powerSource : getPowerSources()) {
            powerSourceArrayBuilder.add(powerSource.toJSON());
        }
        JsonArrayBuilder diskStoreArrayBuilder = jsonFactory.createArrayBuilder();
        for (HWDiskStore diskStore : getDiskStores()) {
            diskStoreArrayBuilder.add(diskStore.toJSON());
        }
        JsonArrayBuilder networkIFArrayBuilder = jsonFactory.createArrayBuilder();
        for (NetworkIF netStore : getNetworkIFs()) {
            networkIFArrayBuilder.add(netStore.toJSON());
        }
        JsonArrayBuilder displayArrayBuilder = jsonFactory.createArrayBuilder();
        for (Display display : getDisplays()) {
            displayArrayBuilder.add(display.toJSON());
        }
        JsonArrayBuilder usbDeviceArrayBuilder = jsonFactory.createArrayBuilder();
        for (UsbDevice usbDevice : getUsbDevices(true)) {
            usbDeviceArrayBuilder.add(usbDevice.toJSON());
        }
        return NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder())
                .add("processor", getProcessor().toJSON()).add("memory", getMemory().toJSON())
                .add("powerSources", powerSourceArrayBuilder.build()).add("disks", diskStoreArrayBuilder.build())
                .add("networks", networkIFArrayBuilder.build()).add("displays", displayArrayBuilder.build())
                .add("sensors", getSensors().toJSON()).add("usbDevices", usbDeviceArrayBuilder.build()).build();
    }

}
