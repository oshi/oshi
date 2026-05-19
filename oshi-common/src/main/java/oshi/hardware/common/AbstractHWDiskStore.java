/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import java.util.Collections;
import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.util.FormatUtil;

/**
 * Common methods for platform HWDiskStore classes
 */
@ThreadSafe
public abstract class AbstractHWDiskStore implements HWDiskStore {

    private final String name;
    private final String model;
    private final String serial;
    private final long size;
    private final String diskType;

    private long reads;
    private long readBytes;
    private long writes;
    private long writeBytes;
    private long currentQueueLength;
    private long transferTime;
    private long timeStamp;
    private List<HWPartition> partitionList = Collections.emptyList();

    /**
     * Creates an AbstractHWDiskStore with unknown disk type.
     *
     * @param name   the disk name
     * @param model  the disk model
     * @param serial the disk serial number
     * @param size   the disk size in bytes
     */
    protected AbstractHWDiskStore(String name, String model, String serial, long size) {
        this(name, model, serial, size, "Unknown");
    }

    /**
     * Creates an AbstractHWDiskStore.
     *
     * @param name     the disk name
     * @param model    the disk model
     * @param serial   the disk serial number
     * @param size     the disk size in bytes
     * @param diskType the disk type (e.g., SSD, HDD)
     */
    protected AbstractHWDiskStore(String name, String model, String serial, long size, String diskType) {
        this.name = name;
        this.model = model;
        this.serial = serial;
        this.size = size;
        this.diskType = diskType;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getModel() {
        return this.model;
    }

    @Override
    public String getSerial() {
        return this.serial;
    }

    @Override
    public long getSize() {
        return this.size;
    }

    @Override
    public String getDiskType() {
        return this.diskType;
    }

    @Override
    public long getReads() {
        return this.reads;
    }

    @Override
    public long getReadBytes() {
        return this.readBytes;
    }

    @Override
    public long getWrites() {
        return this.writes;
    }

    @Override
    public long getWriteBytes() {
        return this.writeBytes;
    }

    @Override
    public long getCurrentQueueLength() {
        return this.currentQueueLength;
    }

    @Override
    public long getTransferTime() {
        return this.transferTime;
    }

    @Override
    public long getTimeStamp() {
        return this.timeStamp;
    }

    @Override
    public List<HWPartition> getPartitions() {
        return this.partitionList;
    }

    /**
     * Sets the disk statistics.
     *
     * @param reads              number of reads
     * @param readBytes          bytes read
     * @param writes             number of writes
     * @param writeBytes         bytes written
     * @param currentQueueLength current I/O queue length
     * @param transferTime       time spent on transfers in ms
     * @param timeStamp          timestamp of the measurement
     */
    protected void setDiskStats(long reads, long readBytes, long writes, long writeBytes, long currentQueueLength,
            long transferTime, long timeStamp) {
        this.reads = reads;
        this.readBytes = readBytes;
        this.writes = writes;
        this.writeBytes = writeBytes;
        this.currentQueueLength = currentQueueLength;
        this.transferTime = transferTime;
        this.timeStamp = timeStamp;
    }

    /**
     * Sets the reads.
     *
     * @param reads the reads
     */
    protected void setReads(long reads) {
        this.reads = reads;
    }

    /**
     * Sets the read bytes.
     *
     * @param readBytes the read bytes
     */
    protected void setReadBytes(long readBytes) {
        this.readBytes = readBytes;
    }

    /**
     * Sets the writes.
     *
     * @param writes the writes
     */
    protected void setWrites(long writes) {
        this.writes = writes;
    }

    /**
     * Sets the write bytes.
     *
     * @param writeBytes the write bytes
     */
    protected void setWriteBytes(long writeBytes) {
        this.writeBytes = writeBytes;
    }

    /**
     * Sets the current queue length.
     *
     * @param currentQueueLength the queue length
     */
    protected void setCurrentQueueLength(long currentQueueLength) {
        this.currentQueueLength = currentQueueLength;
    }

    /**
     * Sets the transfer time.
     *
     * @param transferTime the transfer time in ms
     */
    protected void setTransferTime(long transferTime) {
        this.transferTime = transferTime;
    }

    /**
     * Sets the timestamp.
     *
     * @param timeStamp the timestamp
     */
    protected void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    /**
     * Sets the partition list.
     *
     * @param partitionList the partition list
     */
    protected void setPartitionList(List<HWPartition> partitionList) {
        this.partitionList = partitionList;
    }

    @Override
    public String toString() {
        boolean readwrite = getReads() > 0 || getWrites() > 0;
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append(": ");
        sb.append("(model: ").append(getModel());
        sb.append(" - S/N: ").append(getSerial());
        sb.append(" - type: ").append(getDiskType()).append(") ");
        sb.append("size: ").append(getSize() > 0 ? FormatUtil.formatBytesDecimal(getSize()) : "?").append(", ");
        sb.append("reads: ").append(readwrite ? getReads() : "?");
        sb.append(" (").append(readwrite ? FormatUtil.formatBytes(getReadBytes()) : "?").append("), ");
        sb.append("writes: ").append(readwrite ? getWrites() : "?");
        sb.append(" (").append(readwrite ? FormatUtil.formatBytes(getWriteBytes()) : "?").append("), ");
        sb.append("xfer: ").append(readwrite ? getTransferTime() : "?");
        return sb.toString();
    }
}
