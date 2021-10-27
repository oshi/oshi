/*
 * MIT License
 *
 * Copyright (c) 2010 - 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.driver.mac.disk;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.sun.jna.Native; // NOSONAR squid:S1191
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

    public static int queryFsstat(Statfs[] buf, int bufsize, int flags) {
        return SystemB.INSTANCE.getfsstat64(buf, bufsize, flags);
    }

    public static Statfs[] getFileSystems(int numfs) {
        // Create array to hold results
        Statfs[] fs = new Statfs[numfs];
        // Write file system data to array
        queryFsstat(fs, numfs * new Statfs().size(), SystemB.MNT_NOWAIT);
        return fs;
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
        Statfs[] fs = getFileSystems(numfs);

        // Iterate all mounted file systems
        for (Statfs f : fs) {
            String mntFrom = Native.toString(f.f_mntfromname, StandardCharsets.UTF_8);
            mountPointMap.put(mntFrom.replace("/dev/", ""), Native.toString(f.f_mntonname, StandardCharsets.UTF_8));
        }
        return mountPointMap;
    }
}
