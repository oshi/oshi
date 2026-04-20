/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.mac.disk;

import static oshi.ffm.mac.MacSystem.F_MNTFROMNAME;
import static oshi.ffm.mac.MacSystem.F_MNTONNAME;
import static oshi.ffm.mac.MacSystem.MNT_NOWAIT;
import static oshi.ffm.mac.MacSystem.STATFS;
import static oshi.ffm.mac.MacSystemFunctions.getfsstat64;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.HashMap;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Utility to query fsstat using FFM.
 */
@ThreadSafe
public final class FsstatFFM {

    private FsstatFFM() {
    }

    /**
     * Query fsstat to map partitions to mount points.
     *
     * @return A map with partitions as the key and mount points as the value
     */
    public static Map<String, String> queryPartitionToMountMap() {
        Map<String, String> mountPointMap = new HashMap<>();
        try (Arena arena = Arena.ofConfined()) {
            int numfs = getfsstat64(MemorySegment.NULL, 0, 0);
            if (numfs <= 0) {
                return mountPointMap;
            }
            long statfsSize = STATFS.byteSize();
            MemorySegment statfsBuffer = arena.allocate(statfsSize * numfs);
            numfs = getfsstat64(statfsBuffer, (int) statfsBuffer.byteSize(), MNT_NOWAIT);
            if (numfs <= 0) {
                return mountPointMap;
            }
            for (int f = 0; f < numfs; f++) {
                MemorySegment statfs = statfsBuffer.asSlice(f * statfsSize, statfsSize);
                String mntFrom = statfs.getString(STATFS.byteOffset(F_MNTFROMNAME));
                String mntOn = statfs.getString(STATFS.byteOffset(F_MNTONNAME));
                mountPointMap.put(mntFrom.replace("/dev/", ""), mntOn);
            }
        } catch (Throwable t) {
            // Fall through with empty map
        }
        return mountPointMap;
    }
}
