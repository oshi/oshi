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
package oshi.software.os;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import oshi.json.NullAwareJsonObjectBuilder;
import oshi.json.OshiJsonObject;

/**
 * The File System is a storage pool, device, partition, volume, concrete file
 * system or other implementation specific means of file storage. See subclasses
 * for definitions as they apply to specific platforms.
 * 
 * @author widdis[at]gmail[dot]com
 */
public class OSFileStore implements OshiJsonObject {

    private static final long serialVersionUID = 1L;

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    private String name;

    private String mount;

    private String description;

    private String fsType;

    private long usableSpace;

    private long totalSpace;

    /**
     * Creates an OSFileStore with the specified parameters.
     * 
     * @param newName
     *            Name of the filestore
     * @param newDescription
     *            Description of the file store
     * @param newUsableSpace
     *            Available/usable bytes
     * @param newTotalSpace
     *            Total bytes
     */
    public OSFileStore(String newName, String newDescription, long newUsableSpace, long newTotalSpace) {
        this(newName, "unknown", newDescription, "unknown", newUsableSpace, newTotalSpace);
    }

    /**
     * Creates an OSFileStore with the specified parameters.
     * 
     * @param newName
     *            Name of the filestore
     * @param newMount
     *            Mountpoint of the filestore
     * @param newDescription
     *            Description of the file store
     * @param newUsableSpace
     *            Available/usable bytes
     * @param newTotalSpace
     *            Total bytes
     */
    public OSFileStore(String newName, String newMount, String newDescription, long newUsableSpace,
            long newTotalSpace) {
        this(newName, newMount, newDescription, "unknown", newUsableSpace, newTotalSpace);
    }

    /**
     * Creates an OSFileStore with the specified parameters.
     * 
     * @param newName
     *            Name of the filestore
     * @param newMount
     *            Mountpoint of the filestore
     * @param newDescription
     *            Description of the file store
     * @param newType
     *            Type of the filestore, e.g. FAT, NTFS, etx2, ext4, etc.
     * @param newUsableSpace
     *            Available/usable bytes
     * @param newTotalSpace
     *            Total bytes
     */
    public OSFileStore(String newName, String newMount, String newDescription, String newType, long newUsableSpace,
            long newTotalSpace) {
        this.setName(newName);
        this.setMount(newMount);
        this.setDescription(newDescription);
        this.setType(newType);
        this.setUsableSpace(newUsableSpace);
        this.setTotalSpace(newTotalSpace);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject toJSON() {
        return NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder()).add("name", getName())
                .add("mountPoint", getMount()).add("description", getDescription()).add("fsType", getType())
                .add("usableSpace", getUsableSpace()).add("totalSpace", getTotalSpace()).build();
    }
}
