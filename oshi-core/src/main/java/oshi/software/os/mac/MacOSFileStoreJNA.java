/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.mac;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.os.mac.MacOSFileStore;
import oshi.software.os.OSFileStore;

/**
 * OSFileStore implementation
 */
@ThreadSafe
public class MacOSFileStoreJNA extends MacOSFileStore {

    public MacOSFileStoreJNA(String name, String volume, String label, String mount, String options, String uuid,
            boolean local, String logicalVolume, String description, String fsType, long freeSpace, long usableSpace,
            long totalSpace, long freeInodes, long totalInodes) {
        super(name, volume, label, mount, options, uuid, local, logicalVolume, description, fsType, freeSpace,
                usableSpace, totalSpace, freeInodes, totalInodes);
    }

    @Override
    public boolean updateAttributes() {
        for (OSFileStore fileStore : MacFileSystemJNA.getFileStoreMatching(getName(), isLocal())) {
            if (getVolume().equals(fileStore.getVolume()) && getMount().equals(fileStore.getMount())) {
                updateFrom(fileStore);
                return true;
            }
        }
        return false;
    }
}
