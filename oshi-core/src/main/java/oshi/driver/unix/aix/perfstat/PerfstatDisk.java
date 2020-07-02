/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.driver.unix.aix.perfstat;

import com.sun.jna.Native;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.platform.unix.aix.Perfstat;
import oshi.jna.platform.unix.aix.Perfstat.perfstat_disk_t;
import oshi.jna.platform.unix.aix.Perfstat.perfstat_id_t;

/**
 * Utility to query performance stats for bio_stats
 */
@ThreadSafe
public final class PerfstatDisk {

    private static final Perfstat PERF = Perfstat.INSTANCE;

    private PerfstatDisk() {
    }

    /**
     * Queries perfstat_disk for per-disk usage statistics
     *
     * @return an array of usage statistics
     */
    public static perfstat_disk_t[] queryDiskStats() {
        perfstat_disk_t disk_stats = new perfstat_disk_t();
        // With null, null, ..., 0, returns total # of elements
        int total = PERF.perfstat_disk(null, null, disk_stats.size(), 0);
        if (total > 0) {
            perfstat_disk_t[] statp = (perfstat_disk_t[]) disk_stats.toArray(total);
            perfstat_id_t firstdisk_stats = new perfstat_id_t(); // name is ""
            int ret = PERF.perfstat_disk(firstdisk_stats, statp, disk_stats.size(), total);
            if (ret > 0) {
                return statp;
            }
        }
        return new perfstat_disk_t[0];
    }

    public static void main(String[] args) {
        perfstat_disk_t[] disks = queryDiskStats();
        System.out.println("Found " + disks.length + " disk(s)");
        for (int i = 0; i < disks.length; i++) {
            System.out.format("%s: (%s) [%s] size=%d, free=%d, byteR=%d, byteW=%d%n", Native.toString(disks[i].name),
                    Native.toString(disks[i].description), Native.toString(disks[i].vgname), disks[i].size,
                    disks[i].free, disks[i].rblks * disks[i].bsize, disks[i].wblks * disks[i].bsize);
        }
    }
}
