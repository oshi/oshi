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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.SystemInfo;
import oshi.hardware.platform.linux.LinuxDisks;
import oshi.hardware.platform.mac.MacDisks;
import oshi.hardware.platform.unix.freebsd.FreeBsdDisks;
import oshi.hardware.platform.unix.solaris.SolarisDisks;
import oshi.hardware.platform.windows.WindowsDisks;

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
public class HWDiskStore implements Serializable, Comparable<HWDiskStore> {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(HWDiskStore.class);

    private String model;
    private String name;
    private String serial;
    private long size;
    private long reads;
    private long readBytes;
    private long writes;
    private long writeBytes;
    private long transferTime;
    private HWPartition[] partitions;
    private long timeStamp;

    /**
     * Create an object with empty/default values
     */
    public HWDiskStore() {
        this("", "", "", 0L, 0L, 0L, 0L, 0L, 0L, new HWPartition[0], 0L);
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
     * @param readBytes
     *            Number of bytes read from the disk
     * @param writes
     *            Number of writes to the disk
     * @param writeBytes
     *            Number of bytes written to the disk
     * @param transferTime
     *            milliseconds spent reading or writing to the disk
     * @param partitions
     *            Partitions on this disk
     * @param timeStamp
     *            milliseconds since the epoch
     */
    public HWDiskStore(String name, String model, String serial, long size, long reads, long readBytes, long writes,
            long writeBytes, long transferTime, HWPartition[] partitions, long timeStamp) {
        setName(name);
        setModel(model);
        setSerial(serial);
        setSize(size);
        setReads(reads);
        setReadBytes(readBytes);
        setWrites(writes);
        setWriteBytes(writeBytes);
        setTransferTime(transferTime);
        setPartitions(partitions);
        setTimeStamp(timeStamp);
    }

    /**
     * Make a best effort to update all the statistics about the drive without
     * needing to recreate the drive list. This method provides for more
     * frequent periodic updates of drive statistics. It will not detect if a
     * removable drive has been removed and replaced by a different drive in
     * between method calls.
     * 
     * @return True if the update was (probably) successful, false if the disk
     *         was not found
     */
    public boolean updateDiskStats() {
        boolean diskFound = false;
        switch (SystemInfo.getCurrentPlatformEnum()) {
        case WINDOWS:
            diskFound = WindowsDisks.updateDiskStats(this);
            break;
        case LINUX:
            diskFound = LinuxDisks.updateDiskStats(this);
            break;
        case MACOSX:
            diskFound = MacDisks.updateDiskStats(this);
            break;
        case SOLARIS:
            diskFound = SolarisDisks.updateDiskStats(this);
            break;
        case FREEBSD:
            diskFound = FreeBsdDisks.updateDiskStats(this);
            break;
        default:
            LOG.error("Unsupported platform. No update performed.");
            break;
        }
        return diskFound;
    }

    /**
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return the model
     */
    public String getModel() {
        return this.model;
    }

    /**
     * @return the serial
     */
    public String getSerial() {
        return this.serial;
    }

    /**
     * @return Get size of disk (in bytes)
     */
    public long getSize() {
        return this.size;
    }

    /**
     * @return the reads
     */
    public long getReads() {
        return this.reads;
    }

    /**
     * @return the bytes read
     */
    public long getReadBytes() {
        return this.readBytes;
    }

    /**
     * @return the writes
     */
    public long getWrites() {
        return this.writes;
    }

    /**
     * @return the bytes written
     */
    public long getWriteBytes() {
        return this.writeBytes;
    }

    /**
     * @return the milliseconds spent reading or writing
     */
    public long getTransferTime() {
        return this.transferTime;
    }

    /**
     * @return Returns the partitions on this drive.
     */
    public HWPartition[] getPartitions() {
        return this.partitions;
    }

    /**
     * @return Returns the timeStamp.
     */
    public long getTimeStamp() {
        return this.timeStamp;
    }

    /**
     * @param name
     *            the name to set
     */
    public void setName(String name) {
        this.name = name == null ? "" : name;
    }

    /**
     * @param model
     *            the model to set
     */
    public void setModel(String model) {
        this.model = model == null ? "" : model;
    }

    /**
     * @param serial
     *            the serial to set
     */
    public void setSerial(String serial) {
        this.serial = serial == null ? "" : serial;
    }

    /**
     * @param size
     *            Set size of disk (in bytes)
     */
    public void setSize(long size) {
        this.size = size;
    }

    /**
     * @param reads
     *            the reads to set
     */
    public void setReads(long reads) {
        this.reads = reads;
    }

    /**
     * @param readBytes
     *            the bytes read to set
     */
    public void setReadBytes(long readBytes) {
        this.readBytes = readBytes;
    }

    /**
     * @param writes
     *            the writes to set
     */
    public void setWrites(long writes) {
        this.writes = writes;
    }

    /**
     * @param writeBytes
     *            the bytes written to set
     */
    public void setWriteBytes(long writeBytes) {
        this.writeBytes = writeBytes;
    }

    /**
     * @param transferTime
     *            milliseconds spent reading or writing to set
     */
    public void setTransferTime(long transferTime) {
        this.transferTime = transferTime;
    }

    /**
     * @param partitions
     *            The partitions to set.
     */
    public void setPartitions(HWPartition[] partitions) {
        this.partitions = partitions;
    }

    /**
     * @param timeStamp
     *            The timeStamp to set.
     */
    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(HWDiskStore store) {
        // Naturally sort by device name
        return getName().compareTo(store.getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.model == null ? 0 : this.model.hashCode());
        result = prime * result + (this.name == null ? 0 : this.name.hashCode());
        result = prime * result + (this.serial == null ? 0 : this.serial.hashCode());
        result = prime * result + (int) (this.size ^ this.size >>> 32);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof HWDiskStore)) {
            return false;
        }
        HWDiskStore other = (HWDiskStore) obj;
        if (this.model == null) {
            if (other.model != null) {
                return false;
            }
        } else if (!this.model.equals(other.model)) {
            return false;
        }
        if (this.name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!this.name.equals(other.name)) {
            return false;
        }
        if (this.serial == null) {
            if (other.serial != null) {
                return false;
            }
        } else if (!this.serial.equals(other.serial)) {
            return false;
        }
        return this.size == other.size;
    }

}
