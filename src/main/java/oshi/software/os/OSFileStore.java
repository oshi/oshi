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
 * Contributors:
 * dblock[at]dblock[dot]org
 * alessandro[at]perucchi[dot]org
 * widdis[at]gmail[dot]com
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.software.os;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The File System is a storage pool, device, partition, volume, concrete file
 * system or other implementation specific means of file storage. See subclasses
 * for definitions as they apply to specific platforms.
 * 
 * @author widdis[at]gmail[dot]com
 */
public class OSFileStore {
    private static final Logger LOG = LoggerFactory.getLogger(OSFileStore.class);

    private String name;

    private String description;

    private long usableSpace;

    private long totalSpace;

    /**
     * Creates a {@link OSFileStore} with the specified parameters.
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
        this.setName(newName);
        this.setDescription(newDescription);
        this.setUsableSpace(newUsableSpace);
        this.setTotalSpace(newTotalSpace);
        LOG.debug("Initialized OSFileStore: {}", newName);
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
