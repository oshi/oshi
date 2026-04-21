/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.windows;

import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.os.windows.WindowsOSFileStore;
import oshi.software.os.OSFileStore;

/**
 * FFM-based Windows OSFileStore implementation.
 */
@ThreadSafe
public class WindowsOSFileStoreFFM extends WindowsOSFileStore {

    public WindowsOSFileStoreFFM(String name, String volume, String label, String mount, String options, String uuid,
            boolean local, String logicalVolume, String description, String fsType, long freeSpace, long usableSpace,
            long totalSpace, long freeInodes, long totalInodes) {
        super(name, volume, label, mount, options, uuid, local, logicalVolume, description, fsType, freeSpace,
                usableSpace, totalSpace, freeInodes, totalInodes);
    }

    @Override
    public boolean updateAttributes() {
        // Check if we have the volume locally
        List<OSFileStore> volumes;
        if (isLocal()) {
            volumes = WindowsFileSystemFFM.getLocalVolumes(getVolume());
        } else {
            // Not locally, search WMI
            String nameToMatch = getMount().length() < 2 ? null : getMount().substring(0, 2);
            volumes = WindowsFileSystemFFM.getWmiVolumes(nameToMatch, false);
        }
        for (OSFileStore fileStore : volumes) {
            if (getVolume().equals(fileStore.getVolume()) && getMount().equals(fileStore.getMount())) {
                updateFrom(fileStore);
                return true;
            }
        }
        return false;
    }
}
