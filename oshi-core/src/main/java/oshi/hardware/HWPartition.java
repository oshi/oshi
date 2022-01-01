/*
 * MIT License
 *
 * Copyright (c) 2019-2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
public class HWPartition {

    private final String identification;
    private final String name;
    private final String type;
    private final String uuid;
    private final long size;
    private final int major;
    private final int minor;
    private final String mountPoint;

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
