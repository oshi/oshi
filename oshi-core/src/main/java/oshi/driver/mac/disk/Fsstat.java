/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.mac.disk;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.sun.jna.Native;
import com.sun.jna.platform.mac.SystemB;
import com.sun.jna.platform.mac.SystemB.Statfs;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Utility to query fsstat
 */
@ThreadSafe
public final class Fsstat {

    private Fsstat() {
    }

    /**
     * Query fsstat to map partitions to mount points
     *
     * @return A map with partitions as the key and mount points as the value
     */
    public static Map<String, String> queryPartitionToMountMap() {
        Map<String, String> mountPointMap = new HashMap<>();

        // Use statfs to get size of mounted file systems
        int numfs = queryFsstat(null, 0, 0);
        // Get data on file system
        Statfs s = new Statfs();
        // Create array to hold results
        Statfs[] fs = (Statfs[]) s.toArray(numfs);
        // Write file system data to array
        queryFsstat(fs, numfs * fs[0].size(), SystemB.MNT_NOWAIT);

        // Iterate all mounted file systems
        for (Statfs f : fs) {
            String mntFrom = Native.toString(f.f_mntfromname, StandardCharsets.UTF_8);
            mountPointMap.put(mntFrom.replace("/dev/", ""), Native.toString(f.f_mntonname, StandardCharsets.UTF_8));
        }
        return mountPointMap;
    }

    private static int queryFsstat(Statfs[] buf, int bufsize, int flags) {
        return SystemB.INSTANCE.getfsstat64(buf, bufsize, flags);
    }

}
