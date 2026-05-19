/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.windows;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractOSFileStore;

/**
 * Common base class for Windows OSFileStore implementations.
 */
@ThreadSafe
public abstract class WindowsOSFileStore extends AbstractOSFileStore {

    /**
     * Constructor.
     *
     * @param name          the name
     * @param volume        the volume
     * @param label         the label
     * @param mount         the mount
     * @param options       the options
     * @param uuid          the uuid
     * @param local         the local
     * @param logicalVolume the logicalVolume
     * @param description   the description
     * @param fsType        the fsType
     * @param freeSpace     the freeSpace
     * @param usableSpace   the usableSpace
     * @param totalSpace    the totalSpace
     * @param freeInodes    the freeInodes
     * @param totalInodes   the totalInodes
     */
    protected WindowsOSFileStore(String name, String volume, String label, String mount, String options, String uuid,
            boolean local, String logicalVolume, String description, String fsType, long freeSpace, long usableSpace,
            long totalSpace, long freeInodes, long totalInodes) {
        super(name, volume, label, mount, options, uuid, local, logicalVolume, description, fsType, freeSpace,
                usableSpace, totalSpace, freeInodes, totalInodes);
    }
}
