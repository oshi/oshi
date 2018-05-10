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

    private String logicalVolume;

    private String mount;

    private String description;

    private String fsType;

    private String uuid;

    private long usableSpace;

    private long totalSpace;

    public OSFileStore() {
    }

    /**
     * Creates an OSFileStore with the specified parameters.
     *
     * @param newName
     *            Name of the filestore
     * @param newVolume
     *            Volume of the filestore
     * @param newMount
     *            Mountpoint of the filestore
     * @param newDescription
     *            Description of the file store
     * @param newType
     *            Type of the filestore, e.g. FAT, NTFS, etx2, ext4, etc.
     * @param newUuid
     *            UUID/GUID of the filestore
     * @param newUsableSpace
     *            Available/usable bytes
     * @param newTotalSpace
     *            Total bytes
     */
    public OSFileStore(String newName, String newVolume, String newMount, String newDescription, String newType,
            String newUuid, long newUsableSpace, long newTotalSpace) {
        setName(newName);
        setVolume(newVolume);
        setLogicalVolume("");
        setMount(newMount);
        setDescription(newDescription);
        setType(newType);
        setUUID(newUuid);
        setUsableSpace(newUsableSpace);
        setTotalSpace(newTotalSpace);
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
     * Usable space on the drive.
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
}
