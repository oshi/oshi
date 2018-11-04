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
package oshi.json.hardware;

import java.util.Properties;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.json.SystemInfo;
import oshi.json.json.AbstractOshiJsonObject;
import oshi.json.json.NullAwareJsonObjectBuilder;
import oshi.json.util.PropertiesUtil;

/**
 * A storage mechanism where data are recorded by various electronic, magnetic,
 * optical, or mechanical changes to a surface layer of one or more rotating
 * disks or flash storage such as a removable or solid state drive. In contrast
 * to a File System, defining the way an Operating system uses the storage, the
 * Disk Store represents the hardware which a FileSystem uses for its File
 * Stores.
 *
 * @author enrico[dot]bianchi[at]gmail[dot]com
 */
public class HWDiskStore extends AbstractOshiJsonObject implements Comparable<HWDiskStore> {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(HWDiskStore.class);

    private transient JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    private oshi.hardware.HWDiskStore diskStore;

    /**
     * Create a new HWDiskStore with default/empty values
     */
    public HWDiskStore() {
        this.diskStore = new oshi.hardware.HWDiskStore();
    }

    /**
     * Create json diskStore from hardware diskStore
     * 
     * @param diskStore
     *            The object to copy
     */
    public HWDiskStore(oshi.hardware.HWDiskStore diskStore) {
        this.diskStore = new oshi.hardware.HWDiskStore(diskStore);
    }

    /**
     * Update all the statistics about the drive without needing to recreate the
     * drive list
     * 
     * @return True if the update was successful, false if the disk was not
     *         found
     */
    public boolean updateDiskStats() {
        boolean diskFound = false;
        switch (SystemInfo.getCurrentPlatformEnum()) {
        case WINDOWS:
            diskFound = oshi.hardware.platform.windows.WindowsDisks.updateDiskStats(this.diskStore);
            break;
        case LINUX:
            diskFound = oshi.hardware.platform.linux.LinuxDisks.updateDiskStats(this.diskStore);
            break;
        case MACOSX:
            diskFound = oshi.hardware.platform.mac.MacDisks.updateDiskStats(this.diskStore);
            break;
        case SOLARIS:
            diskFound = oshi.hardware.platform.unix.solaris.SolarisDisks.updateDiskStats(this.diskStore);
            break;
        case FREEBSD:
            diskFound = oshi.hardware.platform.unix.freebsd.FreeBsdDisks.updateDiskStats(this.diskStore);
            break;
        default:
            LOG.error("Unsupported platform. No update performed.");
            break;
        }
        return diskFound;
    }

    /**
     * @return the name
     */
    public String getName() {
        return this.diskStore.getName();
    }

    /**
     * @return the model
     */
    public String getModel() {
        return this.diskStore.getModel();
    }

    /**
     * @return the serial
     */
    public String getSerial() {
        return this.diskStore.getSerial();
    }

    /**
     * @return Get size of disk (in bytes)
     */
    public long getSize() {
        return this.diskStore.getSize();
    }

    /**
     * @return the reads
     */
    public long getReads() {
        return this.diskStore.getReads();
    }

    /**
     * @return the bytes read
     */
    public long getReadBytes() {
        return this.diskStore.getReadBytes();
    }

    /**
     * @return the writes
     */
    public long getWrites() {
        return this.diskStore.getWrites();
    }

    /**
     * @return the bytes written
     */
    public long getWriteBytes() {
        return this.diskStore.getWriteBytes();
    }

    /**
     * @return the length of the disk queue (#I/O's in progress). Includes I/O
     *         requests that have been issued to the device driver but have not
     *         yet completed. Not supported on macOS.
     */
    public long getCurrentQueueLength() {
        return this.diskStore.getCurrentQueueLength();
    }

    /**
     * @return the milliseconds spent reading or writing
     */
    public long getTransferTime() {
        return this.diskStore.getTransferTime();
    }

    /**
     * @return the partitions of this disk
     */
    public HWPartition[] getPartitions() {
        HWPartition[] partitions = new HWPartition[this.diskStore.getPartitions().length];
        for (int i = 0; i < partitions.length; i++) {
            partitions[i] = new HWPartition(this.diskStore.getPartitions()[i].getIdentification(),
                    this.diskStore.getPartitions()[i].getName(), this.diskStore.getPartitions()[i].getType(),
                    this.diskStore.getPartitions()[i].getUuid(), this.diskStore.getPartitions()[i].getSize(),
                    this.diskStore.getPartitions()[i].getMajor(), this.diskStore.getPartitions()[i].getMinor(),
                    this.diskStore.getPartitions()[i].getMountPoint());
        }
        return partitions;
    }

    /**
     * @return Returns the timeStamp.
     */
    public long getTimeStamp() {
        return this.diskStore.getTimeStamp();
    }

    /**
     * @param name
     *            the name to set
     */
    public void setName(String name) {
        this.diskStore.setName(name);
    }

    /**
     * @param model
     *            the model to set
     */
    public void setModel(String model) {
        this.diskStore.setModel(model);
    }

    /**
     * @param serial
     *            the serial to set
     */
    public void setSerial(String serial) {
        this.diskStore.setSerial(serial);
    }

    /**
     * @param size
     *            Set size of disk (in bytes)
     */
    public void setSize(long size) {
        this.diskStore.setSize(size);
    }

    /**
     * @param reads
     *            the reads to set
     */
    public void setReads(long reads) {
        this.diskStore.setReads(reads);
    }

    /**
     * @param readBytes
     *            the bytes read to set
     */
    public void setReadBytes(long readBytes) {
        this.diskStore.setReadBytes(readBytes);
    }

    /**
     * @param writes
     *            the writes to set
     */
    public void setWrites(long writes) {
        this.diskStore.setWrites(writes);
    }

    /**
     * @param writeBytes
     *            the bytes written to set
     */
    public void setWriteBytes(long writeBytes) {
        this.diskStore.setWriteBytes(writeBytes);
    }

    /**
     * @param currentQueueLength
     *            the length of the disk queue (#I/O's in progress) to set
     */
    public void setCurrentQueueLength(long currentQueueLength) {
        this.diskStore.setCurrentQueueLength(currentQueueLength);
    }

    /**
     * @param transferTime
     *            milliseconds spent reading or writing to set
     */
    public void setTransferTime(long transferTime) {
        this.diskStore.setTransferTime(transferTime);
    }

    /**
     * @param partitions
     *            Partitions of this disk
     */
    public void setPartitions(HWPartition[] partitions) {
        oshi.hardware.HWPartition[] parts = new oshi.hardware.HWPartition[partitions.length];
        for (int i = 0; i < partitions.length; i++) {
            parts[i] = new oshi.hardware.HWPartition(partitions[i].getIdentification(), partitions[i].getName(),
                    partitions[i].getType(), partitions[i].getUuid(), partitions[i].getSize(), partitions[i].getMajor(),
                    partitions[i].getMinor(), partitions[i].getMountPoint());
        }
        this.diskStore.setPartitions(parts);
    }

    /**
     * @param timeStamp
     *            The timeStamp to set.
     */
    public void setTimeStamp(long timeStamp) {
        this.diskStore.setTimeStamp(timeStamp);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject toJSON(Properties properties) {
        JsonObjectBuilder json = NullAwareJsonObjectBuilder.wrap(this.jsonFactory.createObjectBuilder());
        if (PropertiesUtil.getBoolean(properties, "hardware.disks.name")) {
            json.add("name", this.diskStore.getName());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.disks.model")) {
            json.add("model", this.diskStore.getModel());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.disks.serial")) {
            json.add("serial", this.diskStore.getSerial());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.disks.size")) {
            json.add("size", this.diskStore.getSize());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.disks.reads")) {
            json.add("reads", this.diskStore.getReads());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.disks.readBytes")) {
            json.add("readBytes", this.diskStore.getReadBytes());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.disks.writes")) {
            json.add("writes", this.diskStore.getWrites());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.disks.writeBytes")) {
            json.add("writeBytes", this.diskStore.getWriteBytes());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.disks.queueLength")) {
            json.add("currentQueueLength", this.diskStore.getCurrentQueueLength());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.disks.transferTime")) {
            json.add("transferTime", this.diskStore.getTransferTime());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.disks.partitions")) {
            JsonArrayBuilder partitionArrayBuilder = this.jsonFactory.createArrayBuilder();
            for (HWPartition partition : getPartitions()) {
                partitionArrayBuilder.add(partition.toJSON(properties));
            }
            json.add("partitions", partitionArrayBuilder.build());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.disks.timeStamp")) {
            json.add("timeStamp", this.diskStore.getTimeStamp());
        }
        return json.build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(HWDiskStore store) {
        return this.diskStore.compareTo(store.diskStore);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.diskStore == null ? 0 : this.diskStore.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof HWDiskStore)) {
            return false;
        }
        HWDiskStore other = (HWDiskStore) obj;
        if (this.diskStore == null) {
            if (other.diskStore != null) {
                return false;
            }
        } else if (!this.diskStore.equals(other.diskStore)) {
            return false;
        }
        return true;
    }

}
