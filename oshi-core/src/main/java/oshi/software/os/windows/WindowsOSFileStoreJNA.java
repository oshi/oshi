/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.windows;

import java.util.List;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.os.windows.WindowsOSFileStore;
import oshi.software.os.OSFileStore;

/**
 * JNA-based Windows OSFileStore implementation.
 */
@ThreadSafe
public class WindowsOSFileStoreJNA extends WindowsOSFileStore {

    public WindowsOSFileStoreJNA(String name, String volume, String label, String mount, String options, String uuid,
            boolean local, String logicalVolume, String description, String fsType, long freeSpace, long usableSpace,
            long totalSpace, long freeInodes, long totalInodes) {
        super(name, volume, label, mount, options, uuid, local, logicalVolume, description, fsType, freeSpace,
                usableSpace, totalSpace, freeInodes, totalInodes);
    }

    @Override
    public boolean updateAttributes() {
        // Fast path: call GetDiskFreeSpaceEx directly on the known volume
        WinNT.LARGE_INTEGER userFreeBytes = new WinNT.LARGE_INTEGER(0L);
        WinNT.LARGE_INTEGER totalBytes = new WinNT.LARGE_INTEGER(0L);
        WinNT.LARGE_INTEGER systemFreeBytes = new WinNT.LARGE_INTEGER(0L);
        if (Kernel32.INSTANCE.GetDiskFreeSpaceEx(getVolume(), userFreeBytes, totalBytes, systemFreeBytes)) {
            updateSpace(systemFreeBytes.getValue(), userFreeBytes.getValue(), totalBytes.getValue());
            return true;
        }
        // Fall back to full enumeration
        List<OSFileStore> volumes;
        if (isLocal()) {
            volumes = WindowsFileSystemJNA.getLocalVolumes(getVolume());
        } else {
            String nameToMatch = getMount().length() < 2 ? null : getMount().substring(0, 2);
            volumes = WindowsFileSystemJNA.getWmiVolumes(nameToMatch, false);
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
