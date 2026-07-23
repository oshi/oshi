/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.mac;

import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static oshi.ffm.ForeignFunctions.callInArenaBooleanOrDefault;
import static oshi.ffm.platform.mac.MacSystem.F_FFREE;
import static oshi.ffm.platform.mac.MacSystem.F_FILES;
import static oshi.ffm.platform.mac.MacSystem.STATFS;
import static oshi.util.LogLevel.DEBUG;

import java.io.File;
import java.lang.foreign.MemorySegment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.platform.mac.MacSystemFunctions;
import oshi.software.common.os.mac.MacOSFileStore;
import oshi.software.os.OSFileStore;

/**
 * OSFileStore implementation using FFM (no JNA dependency).
 */
@ThreadSafe
public class MacOSFileStoreFFM extends MacOSFileStore {

    private static final Logger LOG = LoggerFactory.getLogger(MacOSFileStoreFFM.class);

    public MacOSFileStoreFFM(String name, String volume, String label, String mount, String options, String uuid,
            boolean local, String logicalVolume, String description, String fsType, long freeSpace, long usableSpace,
            long totalSpace, long freeInodes, long totalInodes) {
        super(name, volume, label, mount, options, uuid, local, logicalVolume, description, fsType, freeSpace,
                usableSpace, totalSpace, freeInodes, totalInodes);
    }

    @Override
    public boolean updateAttributes() {
        // Fast path: call statfs64 directly on the known mount point
        if (callInArenaBooleanOrDefault(arena -> {
            MemorySegment buf = arena.allocate(STATFS);
            MemorySegment pathSeg = arena.allocateFrom(getMount());
            if (MacSystemFunctions.statfs64(pathSeg, buf) == 0) {
                long ffree = buf.get(JAVA_LONG, STATFS.byteOffset(F_FFREE));
                long files = buf.get(JAVA_LONG, STATFS.byteOffset(F_FILES));
                File f = new File(getMount());
                updateSpaceAndInodes(f.getFreeSpace(), f.getUsableSpace(), f.getTotalSpace(), ffree, files);
                return true;
            }
            return false;
        }, LOG, DEBUG, "statfs64 fast path failed for " + getMount(), false)) {
            return true;
        }
        // Fall back to full enumeration
        for (OSFileStore fileStore : MacFileSystemFFM.getFileStoreMatching(getName(), isLocal())) {
            if (getVolume().equals(fileStore.getVolume()) && getMount().equals(fileStore.getMount())) {
                updateFrom(fileStore);
                return true;
            }
        }
        return false;
    }
}
