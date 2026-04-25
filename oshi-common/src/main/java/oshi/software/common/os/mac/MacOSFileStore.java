/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.mac;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractOSFileStore;
import oshi.software.os.OSFileStore;

/**
 * Common OSFileStore fields and getters for macOS implementations.
 */
@ThreadSafe
public abstract class MacOSFileStore extends AbstractOSFileStore {

    private String logicalVolume;
    private String description;
    private String fsType;

    private long freeSpace;
    private long usableSpace;
    private long totalSpace;
    private long freeInodes;
    private long totalInodes;

    protected MacOSFileStore(String name, String volume, String label, String mount, String options, String uuid,
            boolean local, String logicalVolume, String description, String fsType, long freeSpace, long usableSpace,
            long totalSpace, long freeInodes, long totalInodes) {
        super(name, volume, label, mount, options, uuid, local);
        this.logicalVolume = logicalVolume;
        this.description = description;
        this.fsType = fsType;
        this.freeSpace = freeSpace;
        this.usableSpace = usableSpace;
        this.totalSpace = totalSpace;
        this.freeInodes = freeInodes;
        this.totalInodes = totalInodes;
    }

    @Override
    public String getLogicalVolume() {
        return this.logicalVolume;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public String getType() {
        return this.fsType;
    }

    @Override
    public long getFreeSpace() {
        return this.freeSpace;
    }

    @Override
    public long getUsableSpace() {
        return this.usableSpace;
    }

    @Override
    public long getTotalSpace() {
        return this.totalSpace;
    }

    @Override
    public long getFreeInodes() {
        return this.freeInodes;
    }

    @Override
    public long getTotalInodes() {
        return this.totalInodes;
    }

    /**
     * Copies attributes from another file store into this one.
     *
     * @param fileStore the source file store
     */
    protected void updateFrom(OSFileStore fileStore) {
        this.logicalVolume = fileStore.getLogicalVolume();
        this.description = fileStore.getDescription();
        this.fsType = fileStore.getType();
        this.freeSpace = fileStore.getFreeSpace();
        this.usableSpace = fileStore.getUsableSpace();
        this.totalSpace = fileStore.getTotalSpace();
        this.freeInodes = fileStore.getFreeInodes();
        this.totalInodes = fileStore.getTotalInodes();
    }

    /**
     * Updates only the space and inode fields, bypassing full enumeration.
     *
     * @param freeSpace   free space in bytes
     * @param usableSpace usable space in bytes
     * @param totalSpace  total space in bytes
     * @param freeInodes  free inodes
     * @param totalInodes total inodes
     */
    protected void updateSpaceAndInodes(long freeSpace, long usableSpace, long totalSpace, long freeInodes,
            long totalInodes) {
        this.freeSpace = freeSpace;
        this.usableSpace = usableSpace;
        this.totalSpace = totalSpace;
        this.freeInodes = freeInodes;
        this.totalInodes = totalInodes;
    }
}
