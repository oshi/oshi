/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import java.util.Collections;
import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.HWPartition;
import oshi.hardware.common.AbstractHWDiskStore;

/**
 * Base class for macOS HWDiskStore implementations. Subclasses provide platform-specific disk enumeration and
 * statistics updates.
 */
@ThreadSafe
public abstract class MacHWDiskStore extends AbstractHWDiskStore {

    private long reads = 0L;
    private long readBytes = 0L;
    private long writes = 0L;
    private long writeBytes = 0L;
    private long currentQueueLength = 0L;
    private long transferTime = 0L;
    private long timeStamp = 0L;
    private List<HWPartition> partitionList = Collections.emptyList();

    /**
     * Creates a MacHWDiskStore with unknown disk type.
     *
     * @param name   the disk name
     * @param model  the disk model
     * @param serial the serial number
     * @param size   the disk size in bytes
     */
    protected MacHWDiskStore(String name, String model, String serial, long size) {
        super(name, model, serial, size);
    }

    /**
     * Creates a MacHWDiskStore.
     *
     * @param name     the disk name
     * @param model    the disk model
     * @param serial   the serial number
     * @param size     the disk size in bytes
     * @param diskType the disk type
     */
    protected MacHWDiskStore(String name, String model, String serial, long size, String diskType) {
        super(name, model, serial, size, diskType);
    }

    @Override
    public long getReads() {
        return reads;
    }

    @Override
    public long getReadBytes() {
        return readBytes;
    }

    @Override
    public long getWrites() {
        return writes;
    }

    @Override
    public long getWriteBytes() {
        return writeBytes;
    }

    @Override
    public long getCurrentQueueLength() {
        return currentQueueLength;
    }

    @Override
    public long getTransferTime() {
        return transferTime;
    }

    @Override
    public long getTimeStamp() {
        return timeStamp;
    }

    @Override
    public List<HWPartition> getPartitions() {
        return this.partitionList;
    }

    /** @param reads the reads to set */
    protected void setReads(long reads) {
        this.reads = reads;
    }

    /** @param readBytes the read bytes to set */
    protected void setReadBytes(long readBytes) {
        this.readBytes = readBytes;
    }

    /** @param writes the writes to set */
    protected void setWrites(long writes) {
        this.writes = writes;
    }

    /** @param writeBytes the write bytes to set */
    protected void setWriteBytes(long writeBytes) {
        this.writeBytes = writeBytes;
    }

    /** @param currentQueueLength the queue length to set */
    protected void setCurrentQueueLength(long currentQueueLength) {
        this.currentQueueLength = currentQueueLength;
    }

    /** @param transferTime the transfer time to set */
    protected void setTransferTime(long transferTime) {
        this.transferTime = transferTime;
    }

    /** @param timeStamp the timestamp to set */
    protected void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    /** @param partitionList the partition list to set */
    protected void setPartitionList(List<HWPartition> partitionList) {
        this.partitionList = partitionList;
    }

    /**
     * Strings to convert to CFStringRef for pointer lookups.
     */
    protected enum CFKey {
        /** IOPropertyMatch key. */
        IO_PROPERTY_MATCH("IOPropertyMatch"),
        /** Statistics dictionary key. */
        STATISTICS("Statistics"),
        /** Read operations count. */
        READ_OPS("Operations (Read)"),
        /** Read bytes count. */
        READ_BYTES("Bytes (Read)"),
        /** Read time. */
        READ_TIME("Total Time (Read)"),
        /** Write operations count. */
        WRITE_OPS("Operations (Write)"),
        /** Write bytes count. */
        WRITE_BYTES("Bytes (Write)"),
        /** Write time. */
        WRITE_TIME("Total Time (Write)"),
        /** BSD unit number. */
        BSD_UNIT("BSD Unit"),
        /** Leaf node indicator. */
        LEAF("Leaf"),
        /** Whole disk indicator. */
        WHOLE("Whole"),
        /** DiskArbitration media name. */
        DA_MEDIA_NAME("DAMediaName"),
        /** DiskArbitration volume name. */
        DA_VOLUME_NAME("DAVolumeName"),
        /** DiskArbitration media size. */
        DA_MEDIA_SIZE("DAMediaSize"),
        /** DiskArbitration device model. */
        DA_DEVICE_MODEL("DADeviceModel"),
        /** IOKit model property. */
        MODEL("Model");

        private final String key;

        CFKey(String key) {
            this.key = key;
        }

        /**
         * Gets the CoreFoundation key string.
         *
         * @return the key string
         */
        public String getKey() {
            return this.key;
        }
    }
}
