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
package oshi.json.hardware;

import java.util.Properties;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import oshi.json.json.AbstractOshiJsonObject;
import oshi.json.json.NullAwareJsonObjectBuilder;
import oshi.json.util.PropertiesUtil;

/**
 * A storage mechanism where data are recorded by various electronic, magnetic,
 * optical, or mechanical changes to a surface layer of one or more rotating
 * disks or flash storage such as a removable or solid state drive. In constrast
 * to a File System, defining the way an Operating system uses the storage, the
 * Disk Store represents the hardware which a FileSystem uses for its File
 * Stores.
 *
 * @author enrico[dot]bianchi[at]gmail[dot]com
 */
public class HWDiskStore extends AbstractOshiJsonObject {

    private static final long serialVersionUID = 1L;

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    private oshi.hardware.HWDiskStore hwDiskStore;

    /**
     * Create an object with all values
     * 
     * @param name
     *            Name of the disk (e.g., /dev/disk1)
     * @param model
     *            Model of the disk
     * @param serial
     *            Disk serial number, if available
     * @param size
     *            Disk capacity in bytes
     * @param reads
     *            Number of reads from the disk
     * @param writes
     *            Number of writes to the disk
     */
    public HWDiskStore(String name, String model, String serial, long size, long reads, long writes) {
        this.hwDiskStore = new oshi.hardware.HWDiskStore(name, model, serial, size, reads, writes);
    }

    /**
     * @return the name
     */
    public String getName() {
        return this.hwDiskStore.getName();
    }

    /**
     * @param name
     *            the name to set
     */
    public void setName(String name) {
        this.hwDiskStore.setName(name);
    }

    /**
     * @return the model
     */
    public String getModel() {
        return this.hwDiskStore.getModel();
    }

    /**
     * @param model
     *            the model to set
     */
    public void setModel(String model) {
        this.hwDiskStore.setModel(model);
    }

    /**
     * @return the serial
     */
    public String getSerial() {
        return this.hwDiskStore.getSerial();
    }

    /**
     * @param serial
     *            the serial to set
     */
    public void setSerial(String serial) {
        this.hwDiskStore.setSerial(serial);
    }

    /**
     * @return Get size of disk (in bytes)
     */
    public long getSize() {
        return this.hwDiskStore.getSize();
    }

    /**
     * @param size
     *            Set size of disk (in bytes)
     */
    public void setSize(long size) {
        this.hwDiskStore.setSize(size);
    }

    /**
     * @return the reads
     */
    public long getReads() {
        return this.hwDiskStore.getReads();
    }

    /**
     * @param reads
     *            the reads to set
     */
    public void setReads(long reads) {
        this.hwDiskStore.setReads(reads);
    }

    /**
     * @return the writes
     */
    public long getWrites() {
        return this.hwDiskStore.getWrites();
    }

    /**
     * @param writes
     *            the writes to set
     */
    public void setWrites(long writes) {
        this.hwDiskStore.setWrites(writes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject toJSON(Properties properties) {
        JsonObjectBuilder json = NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder());
        if (PropertiesUtil.getBoolean(properties, "hardware.disks.name")) {
            json.add("name", this.hwDiskStore.getName());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.disks.model")) {
            json.add("model", this.hwDiskStore.getModel());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.disks.serial")) {
            json.add("serial", this.hwDiskStore.getSerial());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.disks.size")) {
            json.add("size", this.hwDiskStore.getSize());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.disks.reads")) {
            json.add("reads", this.hwDiskStore.getReads());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.disks.writes")) {
            json.add("writes", this.hwDiskStore.getWrites());
        }
        return json.build();
    }
}
