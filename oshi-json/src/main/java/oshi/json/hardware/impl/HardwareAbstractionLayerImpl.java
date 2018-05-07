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
package oshi.json.hardware.impl;

import java.util.Properties;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import oshi.json.hardware.CentralProcessor;
import oshi.json.hardware.ComputerSystem;
import oshi.json.hardware.Display;
import oshi.json.hardware.GlobalMemory;
import oshi.json.hardware.HWDiskStore;
import oshi.json.hardware.HWPartition;
import oshi.json.hardware.HardwareAbstractionLayer;
import oshi.json.hardware.NetworkIF;
import oshi.json.hardware.PowerSource;
import oshi.json.hardware.Sensors;
import oshi.json.hardware.UsbDevice;
import oshi.json.json.AbstractOshiJsonObject;
import oshi.json.json.NullAwareJsonObjectBuilder;
import oshi.json.util.PropertiesUtil;

/**
 * Wrapper class to implement HardwareAbstractionLayer interface with
 * platform-specific objects
 */
public class HardwareAbstractionLayerImpl extends AbstractOshiJsonObject implements HardwareAbstractionLayer {

    private static final long serialVersionUID = 1L;

    private transient JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    private oshi.hardware.HardwareAbstractionLayer hal;

    private ComputerSystem computerSystem;
    private CentralProcessor processor;
    private GlobalMemory memory;
    private Sensors sensors;

    /**
     * Creates a new platform-specific HardwareAbstractionLayer object wrapping
     * the provided argument
     *
     * @param hardware
     *            a platform-specific HardwareAbstractionLayer object
     */
    public HardwareAbstractionLayerImpl(oshi.hardware.HardwareAbstractionLayer hardware) {
        this.hal = hardware;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ComputerSystem getComputerSystem() {
        if (this.computerSystem == null) {
            this.computerSystem = new ComputerSystemImpl(this.hal.getComputerSystem());
        }
        return this.computerSystem;
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
            HWPartition[] partitions = new HWPartition[ds[i].getPartitions().length];
            for (int j = 0; j < partitions.length; j++) {
                partitions[j] = new HWPartition(ds[i].getPartitions()[j].getIdentification(),
                        ds[i].getPartitions()[j].getName(), ds[i].getPartitions()[j].getType(),
                        ds[i].getPartitions()[j].getUuid(), ds[i].getPartitions()[j].getSize(),
                        ds[i].getPartitions()[j].getMajor(), ds[i].getPartitions()[j].getMinor(),
                        ds[i].getPartitions()[j].getMountPoint());
            }
            diskStores[i] = new HWDiskStore(ds[i].getName(), ds[i].getModel(), ds[i].getSerial(), ds[i].getSize(),
                    ds[i].getReads(), ds[i].getReadBytes(), ds[i].getWrites(), ds[i].getWriteBytes(),
                    ds[i].getTransferTime(), partitions, ds[i].getTimeStamp());
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
            networkIFs[i].setTimeStamp(ifs[i].getTimeStamp());
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
    public JsonObject toJSON(Properties properties) {
        JsonObjectBuilder json = NullAwareJsonObjectBuilder.wrap(this.jsonFactory.createObjectBuilder());
        if (PropertiesUtil.getBoolean(properties, "hardware.computerSystem")) {
            json.add("computerSystem", getComputerSystem().toJSON(properties));
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.processor")) {
            json.add("processor", getProcessor().toJSON(properties));
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.memory")) {
            json.add("memory", getMemory().toJSON(properties));
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.powerSources")) {
            JsonArrayBuilder powerSourceArrayBuilder = this.jsonFactory.createArrayBuilder();
            for (PowerSource powerSource : getPowerSources()) {
                powerSourceArrayBuilder.add(powerSource.toJSON(properties));
            }
            json.add("powerSources", powerSourceArrayBuilder.build());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.disks")) {
            JsonArrayBuilder diskStoreArrayBuilder = this.jsonFactory.createArrayBuilder();
            for (HWDiskStore diskStore : getDiskStores()) {
                diskStoreArrayBuilder.add(diskStore.toJSON(properties));
            }
            json.add("disks", diskStoreArrayBuilder.build());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.networks")) {
            JsonArrayBuilder networkIFArrayBuilder = this.jsonFactory.createArrayBuilder();
            for (NetworkIF netStore : getNetworkIFs()) {
                networkIFArrayBuilder.add(netStore.toJSON(properties));
            }
            json.add("networks", networkIFArrayBuilder.build());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.displays")) {
            JsonArrayBuilder displayArrayBuilder = this.jsonFactory.createArrayBuilder();
            for (Display display : getDisplays()) {
                displayArrayBuilder.add(display.toJSON(properties));
            }
            json.add("displays", displayArrayBuilder.build());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.sensors")) {
            json.add("sensors", getSensors().toJSON(properties));
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.usbDevices")) {
            JsonArrayBuilder usbDeviceArrayBuilder = this.jsonFactory.createArrayBuilder();
            for (UsbDevice usbDevice : getUsbDevices(
                    PropertiesUtil.getBoolean(properties, "hardware.usbDevices.tree"))) {
                usbDeviceArrayBuilder.add(usbDevice.toJSON(properties));
            }
            json.add("usbDevices", usbDeviceArrayBuilder.build());
        }
        return json.build();

    }

}
