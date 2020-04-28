/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.hardware;

import oshi.annotation.concurrent.Immutable;
import oshi.util.FormatUtil;

/**
 * A region on a hard disk or other secondary storage, so that an operating
 * system can manage information in each region separately. A partition appears
 * in the operating system as a distinct "logical" disk that uses part of the
 * actual disk.
 */
@Immutable
public class HWPartition implements Comparable<HWPartition> {

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
     * <p>
     * Getter for the field <code>identification</code>.
     * </p>
     *
     * @return Returns the identification.
     */
    public String getIdentification() {
        return this.identification;
    }

    /**
     * <p>
     * Getter for the field <code>name</code>.
     * </p>
     *
     * @return Returns the name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * <p>
     * Getter for the field <code>type</code>.
     * </p>
     *
     * @return Returns the type.
     */
    public String getType() {
        return this.type;
    }

    /**
     * <p>
     * Getter for the field <code>uuid</code>.
     * </p>
     *
     * @return Returns the uuid.
     */
    public String getUuid() {
        return this.uuid;
    }

    /**
     * <p>
     * Getter for the field <code>size</code>.
     * </p>
     *
     * @return Returns the size in bytes.
     */
    public long getSize() {
        return this.size;
    }

    /**
     * <p>
     * Getter for the field <code>major</code>.
     * </p>
     *
     * @return Returns the major device ID.
     */
    public int getMajor() {
        return this.major;
    }

    /**
     * <p>
     * Getter for the field <code>minor</code>.
     * </p>
     *
     * @return Returns the minor device ID.
     */
    public int getMinor() {
        return this.minor;
    }

    /**
     * <p>
     * Getter for the field <code>mountPoint</code>.
     * </p>
     *
     * @return Returns the mount point.
     */
    public String getMountPoint() {
        return this.mountPoint;
    }

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
        if (!(obj.getClass().equals(this.getClass()))) {
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getIdentification()).append(": ");
        sb.append(getName()).append(" ");
        sb.append("(").append(getType()).append(") ");
        sb.append("Maj:Min=").append(getMajor()).append(":").append(getMinor()).append(", ");
        sb.append("size: ").append(FormatUtil.formatBytesDecimal(getSize()));
        sb.append(getMountPoint().isEmpty() ? "" : " @ " + getMountPoint());
        return sb.toString();
    }
}
