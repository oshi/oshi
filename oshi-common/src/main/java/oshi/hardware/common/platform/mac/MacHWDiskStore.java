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

    protected MacHWDiskStore(String name, String model, String serial, long size) {
        super(name, model, serial, size);
    }

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

    protected void setReads(long reads) {
        this.reads = reads;
    }

    protected void setReadBytes(long readBytes) {
        this.readBytes = readBytes;
    }

    protected void setWrites(long writes) {
        this.writes = writes;
    }

    protected void setWriteBytes(long writeBytes) {
        this.writeBytes = writeBytes;
    }

    protected void setCurrentQueueLength(long currentQueueLength) {
        this.currentQueueLength = currentQueueLength;
    }

    protected void setTransferTime(long transferTime) {
        this.transferTime = transferTime;
    }

    protected void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    protected void setPartitionList(List<HWPartition> partitionList) {
        this.partitionList = partitionList;
    }

    /**
     * Strings to convert to CFStringRef for pointer lookups.
     */
    protected enum CFKey {
        IO_PROPERTY_MATCH("IOPropertyMatch"), //

        STATISTICS("Statistics"), //
        READ_OPS("Operations (Read)"), READ_BYTES("Bytes (Read)"), READ_TIME("Total Time (Read)"), //
        WRITE_OPS("Operations (Write)"), WRITE_BYTES("Bytes (Write)"), WRITE_TIME("Total Time (Write)"), //

        BSD_UNIT("BSD Unit"), LEAF("Leaf"), WHOLE("Whole"), //

        DA_MEDIA_NAME("DAMediaName"), DA_VOLUME_NAME("DAVolumeName"), DA_MEDIA_SIZE("DAMediaSize"), //
        DA_DEVICE_MODEL("DADeviceModel"), MODEL("Model");

        private final String key;

        CFKey(String key) {
            this.key = key;
        }

        public String getKey() {
            return this.key;
        }
    }
}
