/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.solaris;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractOSFileStore;
import oshi.software.os.OSFileStore;

/**
 * OSFileStore implementation for Solaris. Contains no native code, so a single concrete class serves both the JNA and
 * FFM implementations.
 */
@ThreadSafe
public class SolarisOSFileStore extends AbstractOSFileStore {

    /**
     * Constructs a new {@code SolarisOSFileStore}.
     *
     * @param name          the device name
     * @param volume        the volume name
     * @param label         the volume label
     * @param mount         the mount point
     * @param options       the mount options
     * @param uuid          the volume UUID
     * @param local         whether the store is local
     * @param logicalVolume the logical volume
     * @param description   the description
     * @param fsType        the fs type
     * @param freeSpace     the free space in bytes
     * @param usableSpace   the usable space in bytes
     * @param totalSpace    the total space in bytes
     * @param freeInodes    the free inode count
     * @param totalInodes   the total inode count
     */
    public SolarisOSFileStore(String name, String volume, String label, String mount, String options, String uuid,
            boolean local, String logicalVolume, String description, String fsType, long freeSpace, long usableSpace,
            long totalSpace, long freeInodes, long totalInodes) {
        super(name, volume, label, mount, options, uuid, local, logicalVolume, description, fsType, freeSpace,
                usableSpace, totalSpace, freeInodes, totalInodes);
    }

    @Override
    public boolean updateAttributes() {
        for (OSFileStore fileStore : SolarisFileSystem.getFileStoreMatching(getName(), isLocal())) {
            if (getVolume().equals(fileStore.getVolume()) && getMount().equals(fileStore.getMount())) {
                updateFrom(fileStore);
                return true;
            }
        }
        return false;
    }
}
