/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.windows;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractOSFileStore;
import oshi.software.os.OSFileStore;

/**
 * Common base class for Windows OSFileStore implementations, containing shared fields and getters.
 */
@ThreadSafe
public abstract class WindowsOSFileStore extends AbstractOSFileStore {

    private String logicalVolume;
    private String description;
    private String fsType;

    private long freeSpace;
    private long usableSpace;
    private long totalSpace;
    private long freeInodes;
    private long totalInodes;

    protected WindowsOSFileStore(String name, String volume, String label, String mount, String options, String uuid,
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
     * Sets fields from a matching file store during attribute updates.
     *
     * @param fileStore the file store with updated values
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
     * Updates only the space fields, bypassing full volume enumeration.
     *
     * @param freeSpace   free space in bytes
     * @param usableSpace usable space in bytes
     * @param totalSpace  total space in bytes
     */
    protected void updateSpace(long freeSpace, long usableSpace, long totalSpace) {
        this.freeSpace = freeSpace;
        this.usableSpace = usableSpace;
        this.totalSpace = totalSpace;
    }
}
