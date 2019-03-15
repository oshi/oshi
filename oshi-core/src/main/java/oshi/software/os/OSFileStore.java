/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.software.os;

import java.io.Serializable;

/**
 * A File Store is a storage pool, device, partition, volume, concrete file
 * system or other implementation specific means of file storage. See subclasses
 * for definitions as they apply to specific platforms.
 *
 * @author widdis[at]gmail[dot]com
 */
public class OSFileStore implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String volume;
    private String logicalVolume = "";
    private String mount;
    private String description;
    private String fsType;
    private String uuid;
    private long freeSpace;
    private long usableSpace;
    private long totalSpace;
    private long freeInodes = -1;
    private long totalInodes = -1;

    public OSFileStore() {
    }

    /**
     * Creates a copy of an OSFileStore.
     *
     * @param fileStore
     *            OSFileStore which is copied
     */
    public OSFileStore(OSFileStore fileStore) {
        setName(fileStore.getName());
        setVolume(fileStore.getVolume());
        setLogicalVolume(fileStore.getLogicalVolume());
        setMount(fileStore.getMount());
        setDescription(fileStore.getDescription());
        setType(fileStore.getType());
        setUUID(fileStore.getUUID());
        setFreeSpace(fileStore.getFreeSpace());
        setUsableSpace(fileStore.getUsableSpace());
        setTotalSpace(fileStore.getTotalSpace());
        setFreeInodes(fileStore.getFreeInodes());
        setTotalInodes(fileStore.getTotalInodes());
    }

    /**
     * Name of the File System
     *
     * @return The file system name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sets the File System name
     *
     * @param value
     *            The name
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Volume of the File System
     *
     * @return The volume of the file system
     */
    public String getVolume() {
        return this.volume;
    }

    /**
     * Logical volume of the File System
     *
     * Provides an optional alternative volume identifier for the file system.
     * Only supported on Linux, provides symlink value via '/dev/mapper/' (used
     * with LVM file systems).
     *
     * @return The logical volume of the file system
     */
    public String getLogicalVolume() {
        return this.logicalVolume;
    }

    /**
     * Sets the volume of the File System
     *
     * @param value
     *            The volume
     */
    public void setVolume(String value) {
        this.volume = value;
    }

    /**
     * Sets the logical volume of the File System
     *
     * @param value
     *            The logical volume
     */
    public void setLogicalVolume(String value) {
        this.logicalVolume = value;
    }

    /**
     * Mountpoint of the File System
     *
     * @return The mountpoint of the file system
     */
    public String getMount() {
        return this.mount;
    }

    /**
     * Sets the mountpoint of the File System
     *
     * @param value
     *            The mountpoint
     */
    public void setMount(String value) {
        this.mount = value;
    }

    /**
     * Description of the File System
     *
     * @return The file system description
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Sets the File System description
     *
     * @param value
     *            The description
     */
    public void setDescription(String value) {
        this.description = value;
    }

    /**
     * Type of the File System (FAT, NTFS, etx2, ext4, etc)
     *
     * @return The file system type
     */
    public String getType() {
        return this.fsType;
    }

    /**
     * Sets the File System type
     *
     * @param value
     *            The type
     */
    public void setType(String value) {
        this.fsType = value;
    }

    /**
     * UUID/GUID of the File System
     *
     * @return The file system UUID/GUID
     */
    public String getUUID() {
        return this.uuid;
    }

    /**
     * Sets the File System UUID/GUID
     *
     * @param value
     *            The UUID/GUID
     */
    public void setUUID(String value) {
        this.uuid = value;
    }

    /**
     * Free space on the drive. This space is unallocated but may require
     * elevated permissions to write.
     *
     * @return Free space on the drive (in bytes)
     */
    public long getFreeSpace() {
        return this.freeSpace;
    }

    /**
     * Sets free space on the drive.
     *
     * @param value
     *            Bytes of free space.
     */
    public void setFreeSpace(long value) {
        this.freeSpace = value;
    }

    /**
     * Usable space on the drive. This is space available to unprivileged users.
     *
     * @return Usable space on the drive (in bytes)
     */
    public long getUsableSpace() {
        return this.usableSpace;
    }

    /**
     * Sets usable space on the drive.
     *
     * @param value
     *            Bytes of writable space.
     */
    public void setUsableSpace(long value) {
        this.usableSpace = value;
    }

    /**
     * Total space/capacity of the drive.
     *
     * @return Total capacity of the drive (in bytes)
     */
    public long getTotalSpace() {
        return this.totalSpace;
    }

    /**
     * Sets the total space on the drive.
     *
     * @param value
     *            Bytes of total space.
     */
    public void setTotalSpace(long value) {
        this.totalSpace = value;
    }

    /**
     * Usable / free inodes on the drive. Not applicable on Windows.
     *
     * @return Usable / free inodes on the drive (count), or -1 if unimplemented
     */
    public long getFreeInodes() {
        return this.freeInodes;
    }

    /**
     * Sets usable inodes on the drive.
     *
     * @param value
     *            Number of free inodes.
     */
    public void setFreeInodes(long value) {
        this.freeInodes = value;
    }

    /**
     * Total / maximum number of inodes of the filesystem. Not applicable on
     * Windows.
     *
     * @return Total / maximum number of inodes of the filesystem (count), or -1
     *         if unimplemented
     */
    public long getTotalInodes() {
        return this.totalInodes;
    }

    /**
     * Sets the total / maximum number of inodes on the filesystem.
     *
     * @param value
     *            Total / maximum count of inodes
     */
    public void setTotalInodes(long value) {
        this.totalInodes = value;
    }
}
