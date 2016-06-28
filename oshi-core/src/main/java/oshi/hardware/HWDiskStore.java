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
 * A storage mechanism where data are recorded by various electronic, magnetic,
 * optical, or mechanical changes to a surface layer of one or more rotating
 * disks or or flash storage such as a removable or solid state drive. In
 * constrast to a File System, defining the way an Operating system uses the
 * storage, the Disk Store represents the hardware which a FileSystem uses for
 * its File Stores.
 *
 * @author enrico[dot]bianchi[at]gmail[dot]com
 */
public class HWDiskStore implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String model;
    private String serial;
    private long size;
    private long reads;
    private long writes;

    /**
     * Create an object with empty/default values
     */
    public HWDiskStore() {
        this("", "", "", 0L, 0L, 0L);
    }

    /**
     * Create an object with all values
     * 
     * @param name
     *            Name of the disk (e.g., /dev/disk1)
     * @param model
     *            Model of the disk
     * @param serial
     *            Disk serial number, if available
     * @param size
     *            Disk capacity in bytes
     * @param reads
     *            Number of reads from the disk
     * @param writes
     *            Number of writes to the disk
     */
    public HWDiskStore(String name, String model, String serial, long size, long reads, long writes) {
        setName(name);
        setModel(model);
        setSerial(serial);
        setSize(size);
        setReads(reads);
        setWrites(writes);
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     *            the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the model
     */
    public String getModel() {
        return model;
    }

    /**
     * @param model
     *            the model to set
     */
    public void setModel(String model) {
        this.model = model;
    }

    /**
     * @return the serial
     */
    public String getSerial() {
        return serial;
    }

    /**
     * @param serial
     *            the serial to set
     */
    public void setSerial(String serial) {
        this.serial = serial;
    }

    /**
     * @return Get size of disk (in bytes)
     */
    public long getSize() {
        return size;
    }

    /**
     * @param size
     *            Set size of disk (in bytes)
     */
    public void setSize(long size) {
        this.size = size;
    }

    /**
     * @return the reads
     */
    public long getReads() {
        return reads;
    }

    /**
     * @param reads
     *            the reads to set
     */
    public void setReads(long reads) {
        this.reads = reads;
    }

    /**
     * @return the writes
     */
    public long getWrites() {
        return writes;
    }

    /**
     * @param writes
     *            the writes to set
     */
    public void setWrites(long writes) {
        this.writes = writes;
    }
}
