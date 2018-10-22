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
package oshi.json.software.os;

import java.util.Properties;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import oshi.json.json.AbstractOshiJsonObject;
import oshi.json.json.NullAwareJsonObjectBuilder;
import oshi.json.util.PropertiesUtil;

/**
 * A File Store is a storage pool, device, partition, volume, concrete file
 * system or other implementation specific means of file storage. See subclasses
 * for definitions as they apply to specific platforms.
 *
 * @author widdis[at]gmail[dot]com
 */
public class OSFileStore extends AbstractOshiJsonObject {

    private static final long serialVersionUID = 1L;

    private transient JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    private oshi.software.os.OSFileStore fileStore;

    /**
     * Create an json OSFileStore from OSFileStore.
     *
     * @param origFileStore
     *            Original filestore
     */
    public OSFileStore(oshi.software.os.OSFileStore origFileStore) {
        this.fileStore = new oshi.software.os.OSFileStore(origFileStore);
    }

    /**
     * Name of the File System
     *
     * @return The file system name
     */
    public String getName() {
        return this.fileStore.getName();
    }

    /**
     * Sets the File System name
     *
     * @param value
     *            The name
     */
    public void setName(String value) {
        this.fileStore.setName(value);
    }

    /**
     * Volume of the File System
     *
     * @return The volume of the file system
     */
    public String getVolume() {
        return this.fileStore.getVolume();
    }

    /**
     * Sets the volume of the File System
     *
     * @param value
     *            The volume
     */
    public void setVolume(String value) {
        this.fileStore.setVolume(value);
    }

    /**
     * Logical volume of the File System
     *
     * @return The logical volume of the file system
     */
    public String getLogicalvolume() {
        return this.fileStore.getLogicalVolume();
    }

    /**
     * Sets the logical volume of the File System
     *
     * @param value
     *            The logical volume
     */
    public void setLogicalvolume(String value) {
        this.fileStore.setLogicalVolume(value);
    }

    /**
     * Mountpoint of the File System
     *
     * @return The mountpoint of the file system
     */
    public String getMount() {
        return this.fileStore.getMount();
    }

    /**
     * Sets the mountpoint of the File System
     *
     * @param value
     *            The mountpoint
     */
    public void setMount(String value) {
        this.fileStore.setMount(value);
    }

    /**
     * Description of the File System
     *
     * @return The file system description
     */
    public String getDescription() {
        return this.fileStore.getDescription();
    }

    /**
     * Sets the File System description
     *
     * @param value
     *            The description
     */
    public void setDescription(String value) {
        this.fileStore.setDescription(value);
    }

    /**
     * Type of the File System (FAT, NTFS, etx2, ext4, etc)
     *
     * @return The file system type
     */
    public String getType() {
        return this.fileStore.getType();
    }

    /**
     * Sets the File System type
     *
     * @param value
     *            The type
     */
    public void setType(String value) {
        this.fileStore.setType(value);
    }

    /**
     * UUID/GUID of the File System
     *
     * @return The file system UUID/GUID
     */
    public String getUUID() {
        return this.fileStore.getUUID();
    }

    /**
     * Sets the File System UUID/GUID
     *
     * @param value
     *            The UUID/GUID
     */
    public void setUUID(String value) {
        this.fileStore.setUUID(value);
    }

    /**
     * Usable space on the drive.
     *
     * @return Usable space on the drive (in bytes)
     */
    public long getUsableSpace() {
        return this.fileStore.getUsableSpace();
    }

    /**
     * Sets usable space on the drive.
     *
     * @param value
     *            Bytes of writable space.
     */
    public void setUsableSpace(long value) {
        this.fileStore.setUsableSpace(value);
    }

    /**
     * Total space/capacity of the drive.
     *
     * @return Total capacity of the drive (in bytes)
     */
    public long getTotalSpace() {
        return this.fileStore.getTotalSpace();
    }

    /**
     * Sets the total space on the drive.
     *
     * @param value
     *            Bytes of total space.
     */
    public void setTotalSpace(long value) {
        this.fileStore.setTotalSpace(value);
    }

    /**
     * Usable / free inodes on the drive. Not applicable on Windows.
     *
     * @return Usable / free inodes on the drive (count), or -1 if unimplemented
     */
    public long getFreeInodes() {
        return this.fileStore.getFreeInodes();
    }

    /**
     * Sets usable inodes on the drive.
     *
     * @param value
     *            Number of free inodes.
     */
    public void setFreeInodes(long value) {
        this.fileStore.setFreeInodes(value);
    }

    /**
     * Total / maximum number of inodes of the filesystem. Not applicable on
     * Windows.
     *
     * @return Total / maximum number of inodes of the filesystem (count), or -1
     *         if unimplemented
     */
    public long getTotalInodes() {
        return this.fileStore.getTotalInodes();
    }

    /**
     * Sets the maximum number of inodes on the filesystem.
     *
     * @param value
     *            Count of maximum number of inodes
     */
    public void setTotalInodes(long value) {
        this.fileStore.setTotalInodes(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject toJSON(Properties properties) {
        JsonObjectBuilder json = NullAwareJsonObjectBuilder.wrap(this.jsonFactory.createObjectBuilder());
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.fileSystem.fileStores.name")) {
            json.add("name", getName());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.fileSystem.fileStores.volume")) {
            json.add("volume", getVolume());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.fileSystem.fileStores.mountPoint")) {
            json.add("mountPoint", getMount());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.fileSystem.fileStores.description")) {
            json.add("description", getDescription());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.fileSystem.fileStores.fsType")) {
            json.add("fsType", getType());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.fileSystem.fileStores.uuid")) {
            json.add("uuid", getUUID());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.fileSystem.fileStores.usableSpace")) {
            json.add("usableSpace", getUsableSpace());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.fileSystem.fileStores.totalSpace")) {
            json.add("totalSpace", getTotalSpace());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.fileSystem.fileStores.freeInodes")) {
            json.add("freeInodes", getFreeInodes());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.fileSystem.fileStores.totalInodes")) {
            json.add("totalInodes", getTotalInodes());
        }
        return json.build();
    }
}
