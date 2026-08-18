/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.os.OSFileStore;

/**
 * Common implementations for OSFileStore
 */
@ThreadSafe
public abstract class AbstractOSFileStore implements OSFileStore {

    private String name;
    private String volume;
    private String label;
    private String mount;
    private String options;
    private String uuid;
    private boolean local;

    // Mutable state refreshed by updateFrom()/updateSpace*(); protected (volatile retained) to match the
    // AbstractOSProcess model. See the VisibilityModifier suppression for this file. The bulk update helpers are
    // retained; the immutable identity fields above stay private.
    protected volatile String logicalVolume;
    protected volatile String description;
    protected volatile String fsType;

    protected volatile long freeSpace;
    protected volatile long usableSpace;
    protected volatile long totalSpace;
    protected volatile long freeInodes;
    protected volatile long totalInodes;

    /**
     * Creates an AbstractOSFileStore with all parameters.
     *
     * @param name          the file store name
     * @param volume        the volume name
     * @param label         the volume label
     * @param mount         the mount point
     * @param options       the mount options
     * @param uuid          the UUID
     * @param local         whether this is a local file store
     * @param logicalVolume the logical volume
     * @param description   the description
     * @param fsType        the filesystem type
     * @param freeSpace     free space in bytes
     * @param usableSpace   usable space in bytes
     * @param totalSpace    total space in bytes
     * @param freeInodes    free inodes
     * @param totalInodes   total inodes
     */
    protected AbstractOSFileStore(String name, String volume, String label, String mount, String options, String uuid,
            boolean local, String logicalVolume, String description, String fsType, long freeSpace, long usableSpace,
            long totalSpace, long freeInodes, long totalInodes) {
        this.name = name;
        this.volume = volume;
        this.label = label;
        this.mount = mount;
        this.options = options;
        this.uuid = uuid;
        this.local = local;
        this.logicalVolume = logicalVolume;
        this.description = description;
        this.fsType = fsType;
        setSpace(freeSpace, usableSpace, totalSpace);
        this.freeInodes = freeInodes;
        this.totalInodes = totalInodes;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getVolume() {
        return this.volume;
    }

    @Override
    public String getLabel() {
        return this.label;
    }

    @Override
    public String getMount() {
        return this.mount;
    }

    @Override
    public String getOptions() {
        return options;
    }

    @Override
    public String getUUID() {
        return this.uuid;
    }

    @Override
    public boolean isLocal() {
        return this.local;
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
     * Copies mutable attributes from another file store into this one.
     *
     * @param fileStore the source file store
     */
    protected synchronized void updateFrom(OSFileStore fileStore) {
        this.logicalVolume = fileStore.getLogicalVolume();
        this.description = fileStore.getDescription();
        this.fsType = fileStore.getType();
        setSpace(fileStore.getFreeSpace(), fileStore.getUsableSpace(), fileStore.getTotalSpace());
        this.freeInodes = fileStore.getFreeInodes();
        this.totalInodes = fileStore.getTotalInodes();
    }

    /**
     * Updates only the space and inode fields.
     *
     * @param freeSpace   free space in bytes
     * @param usableSpace usable space in bytes
     * @param totalSpace  total space in bytes
     * @param freeInodes  free inodes
     * @param totalInodes total inodes
     */
    protected synchronized void updateSpaceAndInodes(long freeSpace, long usableSpace, long totalSpace, long freeInodes,
            long totalInodes) {
        setSpace(freeSpace, usableSpace, totalSpace);
        this.freeInodes = freeInodes;
        this.totalInodes = totalInodes;
    }

    /**
     * Updates only the space fields (no inodes).
     *
     * @param freeSpace   free space in bytes
     * @param usableSpace usable space in bytes
     * @param totalSpace  total space in bytes
     */
    protected synchronized void updateSpace(long freeSpace, long usableSpace, long totalSpace) {
        setSpace(freeSpace, usableSpace, totalSpace);
    }

    /*
     * Single write path for the three space values, so the invariant cannot depend on which platform, which native
     * implementation, or which of the four update entry points supplied them.
     *
     * Most implementations obtain the three values from three separate queries -- three statvfs calls, by way of the
     * File methods -- so on a filesystem whose capacity is computed rather than fixed they describe three adjacent
     * instants rather than one, and can contradict each other: ZFS derives a dataset's total from its pool, and a
     * swap-backed tmpfs derives it from free memory. Reading them atomically would take a native statvfs per platform
     * (which LinuxFileSystem.queryStatvfs does, and the other platforms deliberately do not), so instead the ordering
     * is restored here by clamping downward only. No value is ever reported higher than the OS reported it, which keeps
     * the error in the direction that overstates fullness rather than free capacity.
     */
    private void setSpace(long freeSpace, long usableSpace, long totalSpace) {
        // Clamp against locals rather than the fields being written. Reading back a volatile just assigned would make
        // this a read-modify-write, and two concurrent refreshes could then interleave to produce a triple that
        // violates the very ordering this method exists to guarantee.
        long total = Math.max(0L, totalSpace);
        long free = Math.min(Math.max(0L, freeSpace), total);
        long usable = Math.min(Math.max(0L, usableSpace), free);
        this.totalSpace = total;
        this.freeSpace = free;
        this.usableSpace = usable;
    }

    @Override
    public String toString() {
        return "OSFileStore [name=" + getName() + ", volume=" + getVolume() + ", label=" + getLabel()
                + ", logicalVolume=" + getLogicalVolume() + ", mount=" + getMount() + ", description="
                + getDescription() + ", fsType=" + getType() + ", options=\"" + getOptions() + "\", uuid=" + getUUID()
                + ", isLocal=" + isLocal() + ", freeSpace=" + getFreeSpace() + ", usableSpace=" + getUsableSpace()
                + ", totalSpace=" + getTotalSpace() + ", freeInodes=" + getFreeInodes() + ", totalInodes="
                + getTotalInodes() + "]";
    }
}
