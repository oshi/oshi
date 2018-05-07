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
        setIdentification(identification);
        setName(name);
        setType(type);
        setUuid(uuid);
        setSize(size);
        setMajor(major);
        setMinor(minor);
        setMountPoint(mountPoint);
    }

    /**
     * Creates a new HWPartition
     */
    public HWPartition() {
        this("", "", "", "", 0L, 0, 0, "");
    }

    /**
     * @return Returns the identification.
     */
    public String getIdentification() {
        return this.identification;
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return Returns the type.
     */
    public String getType() {
        return this.type;
    }

    /**
     * @return Returns the uuid.
     */
    public String getUuid() {
        return this.uuid;
    }

    /**
     * @return Returns the size in bytes.
     */
    public long getSize() {
        return this.size;
    }

    /**
     * @return Returns the major device ID.
     */
    public int getMajor() {
        return this.major;
    }

    /**
     * @return Returns the minor device ID.
     */
    public int getMinor() {
        return this.minor;
    }

    /**
     * @return Returns the mount point.
     */
    public String getMountPoint() {
        return this.mountPoint;
    }

    /**
     * @param identification
     *            The identification to set.
     */
    public void setIdentification(String identification) {
        this.identification = identification == null ? "" : identification;
    }

    /**
     * @param name
     *            The name to set.
     */
    public void setName(String name) {
        this.name = name == null ? "" : name;
    }

    /**
     * @param type
     *            The type to set.
     */
    public void setType(String type) {
        this.type = type == null ? "" : type;
    }

    /**
     * @param uuid
     *            The uuid to set.
     */
    public void setUuid(String uuid) {
        this.uuid = uuid == null ? "" : uuid;
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
     * @param mountPoint
     *            Mount point of the partition
     */
    public void setMountPoint(String mountPoint) {
        this.mountPoint = mountPoint == null ? "" : mountPoint;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(HWPartition part) {
        // Naturally sort by device ID
        return getIdentification().compareTo(part.getIdentification());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.identification == null ? 0 : this.identification.hashCode());
        result = prime * result + this.major;
        result = prime * result + this.minor;
        result = prime * result + (this.mountPoint == null ? 0 : this.mountPoint.hashCode());
        result = prime * result + (this.name == null ? 0 : this.name.hashCode());
        result = prime * result + (int) (this.size ^ this.size >>> 32);
        result = prime * result + (this.type == null ? 0 : this.type.hashCode());
        result = prime * result + (this.uuid == null ? 0 : this.uuid.hashCode());
        return result;
    }

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
        if (this.identification == null) {
            if (other.identification != null) {
                return false;
            }
        } else if (!this.identification.equals(other.identification)) {
            return false;
        }
        if (this.major != other.major) {
            return false;
        }
        if (this.minor != other.minor) {
            return false;
        }
        if (this.mountPoint == null) {
            if (other.mountPoint != null) {
                return false;
            }
        } else if (!this.mountPoint.equals(other.mountPoint)) {
            return false;
        }
        if (this.name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!this.name.equals(other.name)) {
            return false;
        }
        if (this.size != other.size) {
            return false;
        }
        if (this.type == null) {
            if (other.type != null) {
                return false;
            }
        } else if (!this.type.equals(other.type)) {
            return false;
        }
        if (this.uuid == null) {
            if (other.uuid != null) {
                return false;
            }
        } else if (!this.uuid.equals(other.uuid)) {
            return false;
        }
        return true;
    }

}
