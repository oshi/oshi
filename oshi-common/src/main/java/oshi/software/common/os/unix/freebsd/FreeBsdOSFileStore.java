/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.freebsd;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractOSFileStore;
import oshi.software.os.OSFileStore;

/**
 * OSFileStore implementation. The owning {@link FreeBsdFileSystem} is captured at construction so
 * {@link #updateAttributes()} can re-query against whichever JNA/FFM concrete produced this instance, rather than
 * baking a specific subclass into oshi-common.
 */
@ThreadSafe
public class FreeBsdOSFileStore extends AbstractOSFileStore {

    private final FreeBsdFileSystem fileSystem;

    /**
     * Constructs a new {@code FreeBsdOSFileStore}.
     *
     * @param fileSystem    the owning file system
     * @param name          the file store name
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
    public FreeBsdOSFileStore(FreeBsdFileSystem fileSystem, String name, String volume, String label, String mount,
            String options, String uuid, boolean local, String logicalVolume, String description, String fsType,
            long freeSpace, long usableSpace, long totalSpace, long freeInodes, long totalInodes) {
        super(name, volume, label, mount, options, uuid, local, logicalVolume, description, fsType, freeSpace,
                usableSpace, totalSpace, freeInodes, totalInodes);
        this.fileSystem = fileSystem;
    }

    @Override
    public boolean updateAttributes() {
        for (OSFileStore fileStore : fileSystem.getFileStores(isLocal())) {
            if (getName().equals(fileStore.getName()) && getVolume().equals(fileStore.getVolume())
                    && getMount().equals(fileStore.getMount())) {
                updateFrom(fileStore);
                return true;
            }
        }
        return false;
    }
}
