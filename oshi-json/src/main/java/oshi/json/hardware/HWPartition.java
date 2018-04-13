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
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import oshi.json.json.AbstractOshiJsonObject;
import oshi.json.json.NullAwareJsonObjectBuilder;
import oshi.json.util.PropertiesUtil;

/**
 * A region on a hard disk or other secondary storage, so that an operating
 * system can manage information in each region separately. A partition appears
 * in the operating system as a distinct "logical" disk that uses part of the
 * actual disk.
 *
 * @author widdis[at]gmail[dot]com
 */
public class HWPartition extends AbstractOshiJsonObject implements Comparable<HWPartition> {

    private static final long serialVersionUID = 1L;

    private transient JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    private oshi.hardware.HWPartition hwPartition;

    /**
     * Creates a new HWPartition
     *
     * @param identification
     *            The unique partition id
     * @param name
     *            Friendly name of the partition
     * @param type
     *            Type or description of the partition
     * @param uuid
     *            UUID
     * @param size
     *            Size in bytes
     * @param major
     *            Device ID (Major)
     * @param minor
     *            Device ID (Minor)
     * @param mountPoint
     *            Where the partition is mounted
     */
    public HWPartition(String identification, String name, String type, String uuid, long size, int major, int minor,
            String mountPoint) {
        this.hwPartition = new oshi.hardware.HWPartition(identification, name, type, uuid, size, major, minor,
                mountPoint);
    }

    /**
     * Creates a new HWPartition
     */
    public HWPartition() {
        this.hwPartition = new oshi.hardware.HWPartition();
    }

    /**
     * @return Returns the identification.
     */
    public String getIdentification() {
        return this.hwPartition.getIdentification();
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return this.hwPartition.getName();
    }

    /**
     * @return Returns the type.
     */
    public String getType() {
        return this.hwPartition.getType();
    }

    /**
     * @return Returns the uuid.
     */
    public String getUuid() {
        return this.hwPartition.getUuid();
    }

    /**
     * @return Returns the size in bytes.
     */
    public long getSize() {
        return this.hwPartition.getSize();
    }

    /**
     * @return Returns the major device ID.
     */
    public int getMajor() {
        return this.hwPartition.getMajor();
    }

    /**
     * @return Returns the minor device ID.
     */
    public int getMinor() {
        return this.hwPartition.getMinor();
    }

    /**
     * @return Returns the mount point.
     */
    public String getMountPoint() {
        return this.hwPartition.getMountPoint();
    }

    /**
     * @param identification
     *            The identification to set.
     */
    public void setIdentification(String identification) {
        this.hwPartition.setIdentification(identification);
    }

    /**
     * @param name
     *            The name to set.
     */
    public void setName(String name) {
        this.hwPartition.setName(name);
    }

    /**
     * @param type
     *            The type to set.
     */
    public void setType(String type) {
        this.hwPartition.setType(type);
    }

    /**
     * @param uuid
     *            The uuid to set.
     */
    public void setUuid(String uuid) {
        this.hwPartition.setUuid(uuid);
    }

    /**
     * @param size
     *            The size (in bytes) to set.
     */
    public void setSize(long size) {
        this.hwPartition.setSize(size);
    }

    /**
     * @param major
     *            The major device ID to set.
     */
    public void setMajor(int major) {
        this.hwPartition.setMajor(major);
    }

    /**
     * @param minor
     *            The minor device ID to set.
     */
    public void setMinor(int minor) {
        this.hwPartition.setMinor(minor);
    }

    /**
     * @param mountPoint
     *            Mount point of the partition
     */
    public void setMountPoint(String mountPoint) {
        this.hwPartition.setMountPoint(mountPoint);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject toJSON(Properties properties) {
        JsonObjectBuilder json = NullAwareJsonObjectBuilder.wrap(this.jsonFactory.createObjectBuilder());
        if (PropertiesUtil.getBoolean(properties, "hardware.disks.partitions.identification")) {
            json.add("identification", this.hwPartition.getIdentification());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.disks.partitions.name")) {
            json.add("name", this.hwPartition.getName());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.disks.partitions.type")) {
            json.add("type", this.hwPartition.getType());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.disks.partitions.uuid")) {
            json.add("uuid", this.hwPartition.getUuid());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.disks.partitions.size")) {
            json.add("size", this.hwPartition.getSize());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.disks.partitions.major")) {
            json.add("major", this.hwPartition.getMajor());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.disks.partitions.minor")) {
            json.add("minor", this.hwPartition.getMinor());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.disks.partitions.mountPoint")) {
            json.add("mountPoint", this.hwPartition.getMountPoint());
        }
        return json.build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(HWPartition part) {
        return this.hwPartition.compareTo(part.hwPartition);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.hwPartition == null ? 0 : this.hwPartition.hashCode());
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
        if (!(obj instanceof HWPartition)) {
            return false;
        }
        HWPartition other = (HWPartition) obj;
        if (this.hwPartition == null) {
            if (other.hwPartition != null) {
                return false;
            }
        } else if (!this.hwPartition.equals(other.hwPartition)) {
            return false;
        }
        return true;
    }

}
