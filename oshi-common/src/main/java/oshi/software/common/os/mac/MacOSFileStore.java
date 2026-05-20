/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.mac;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractOSFileStore;

/**
 * Common base class for macOS OSFileStore implementations.
 */
@ThreadSafe
public abstract class MacOSFileStore extends AbstractOSFileStore {

    /**
     * Creates a MacOSFileStore.
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
     */
    protected MacOSFileStore(String name, String volume, String label, String mount, String options, String uuid,
            boolean local, String logicalVolume, String description, String fsType, long freeSpace, long usableSpace,
            long totalSpace, long freeInodes, long totalInodes) {
        super(name, volume, label, mount, options, uuid, local, logicalVolume, description, fsType, freeSpace,
                usableSpace, totalSpace, freeInodes, totalInodes);
    }
}
