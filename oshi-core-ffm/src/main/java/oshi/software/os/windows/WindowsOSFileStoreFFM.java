/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.windows;

import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static oshi.ffm.windows.WindowsForeignFunctions.toWideString;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.List;
import java.util.OptionalInt;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.windows.Kernel32FFM;
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
        // Fast path: call GetDiskFreeSpaceEx directly on the known volume
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment userFreeBytesBuf = arena.allocate(JAVA_LONG);
            MemorySegment totalBytesBuf = arena.allocate(JAVA_LONG);
            MemorySegment systemFreeBytesBuf = arena.allocate(JAVA_LONG);
            OptionalInt result = Kernel32FFM.GetDiskFreeSpaceEx(toWideString(arena, getVolume()), userFreeBytesBuf,
                    totalBytesBuf, systemFreeBytesBuf);
            if (result.isPresent() && result.getAsInt() != 0) {
                updateSpace(systemFreeBytesBuf.get(JAVA_LONG, 0), userFreeBytesBuf.get(JAVA_LONG, 0),
                        totalBytesBuf.get(JAVA_LONG, 0));
                return true;
            }
        }
        // Fall back to full enumeration
        List<OSFileStore> volumes;
        if (isLocal()) {
            volumes = WindowsFileSystemFFM.getLocalVolumes(getVolume());
        } else {
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
