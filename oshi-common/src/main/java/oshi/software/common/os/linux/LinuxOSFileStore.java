/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.linux;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractOSFileStore;
import oshi.software.os.OSFileStore;

/**
 * OSFileStore implementation
 */
@ThreadSafe
public class LinuxOSFileStore extends AbstractOSFileStore {

    private final LinuxFileSystem fs;

    /**
     * Creates a LinuxOSFileStore.
     *
     * @param name          the name
     * @param volume        the volume
     * @param label         the label
     * @param mount         the mount point
     * @param options       the mount options
     * @param uuid          the UUID
     * @param local         whether local
     * @param logicalVolume the logical volume
     * @param description   the description
     * @param fsType        the filesystem type
     * @param freeSpace     free space in bytes
     * @param usableSpace   usable space in bytes
     * @param totalSpace    total space in bytes
     * @param freeInodes    free inodes
     * @param totalInodes   total inodes
     * @param fs            the parent filesystem
     */
    public LinuxOSFileStore(String name, String volume, String label, String mount, String options, String uuid,
            boolean local, String logicalVolume, String description, String fsType, long freeSpace, long usableSpace,
            long totalSpace, long freeInodes, long totalInodes, LinuxFileSystem fs) {
        super(name, volume, label, mount, options, uuid, local, logicalVolume, description, fsType, freeSpace,
                usableSpace, totalSpace, freeInodes, totalInodes);
        this.fs = fs;
    }

    @Override
    public boolean updateAttributes() {
        // Fast path: query space/inode stats directly on the known mount point
        long[] vfs = fs.queryStatvfs(getMount());
        if (vfs != null) {
            long ts = vfs[2];
            long us = vfs[3];
            long frs = vfs[4];
            // If native methods failed use JVM methods
            if (ts == 0L) {
                java.io.File f = new java.io.File(getMount());
                ts = f.getTotalSpace();
                us = f.getUsableSpace();
                frs = f.getFreeSpace();
            }
            updateSpaceAndInodes(frs, us, ts, vfs[1], vfs[0]);
            return true;
        }
        // Fall back to full enumeration if the direct call failed
        for (OSFileStore fileStore : fs.getFileStoreMatching(getName(), LinuxFileSystem.buildUuidMap(), isLocal())) {
            if (getVolume().equals(fileStore.getVolume()) && getMount().equals(fileStore.getMount())) {
                updateFrom(fileStore);
                return true;
            }
        }
        return false;
    }
}
