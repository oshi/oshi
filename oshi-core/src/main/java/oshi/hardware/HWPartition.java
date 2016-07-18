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
package oshi.hardware;

import java.io.Serializable;

/**
 * A region on a hard disk or other secondary storage, so that an operating
 * system can manage information in each region separately. A partition appears
 * in the operating system as a distinct "logical" disk that uses part of the
 * actual disk.
 *
 * @author widdis[at]gmail[dot]com
 */
public class HWPartition implements Serializable, Comparable<HWPartition> {

    private static final long serialVersionUID = 1L;

    private String identification;
    private String name;
    private String type;
    private String uuid;
    private long size;
    private int major;
    private int minor;
    private String mountPoint;

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
        this.identification = identification;
        this.name = name;
        this.type = type;
        this.uuid = uuid;
        this.size = size;
        this.major = major;
        this.minor = minor;
        this.mountPoint = mountPoint;
    }

    /**
     * @return Returns the identification.
     */
    public String getIdentification() {
        return identification;
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return Returns the type.
     */
    public String getType() {
        return type;
    }

    /**
     * @return Returns the uuid.
     */
    public String getUuid() {
        return uuid;
    }

    /**
     * @return Returns the size in bytes.
     */
    public long getSize() {
        return size;
    }

    /**
     * @return Returns the major device ID.
     */
    public int getMajor() {
        return major;
    }

    /**
     * @return Returns the minor device ID.
     */
    public int getMinor() {
        return minor;
    }

    /**
     * @return Returns the mount point.
     */
    public String getMountPoint() {
        return mountPoint;
    }

    /**
     * @param identification
     *            The identification to set.
     */
    public void setIdentification(String identification) {
        this.identification = identification;
    }

    /**
     * @param name
     *            The name to set.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param type
     *            The type to set.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @param uuid
     *            The uuid to set.
     */
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    /**
     * @param size
     *            The size (in bytes) to set.
     */
    public void setSize(long size) {
        this.size = size;
    }

    /**
     * @param major
     *            The major device ID to set.
     */
    public void setMajor(int major) {
        this.major = major;
    }

    /**
     * @param minor
     *            The minor device ID to set.
     */
    public void setMinor(int minor) {
        this.minor = minor;
    }

    /**
     * @param active
     *            Set whether the partition is active.
     */
    public void setMountPoint(String mountPoint) {
        this.mountPoint = mountPoint;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(HWPartition part) {
        // Naturally sort by device ID
        return this.getIdentification().compareTo(part.getIdentification());
    }
}
