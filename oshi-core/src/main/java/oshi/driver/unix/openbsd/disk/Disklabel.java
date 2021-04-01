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
package oshi.driver.unix.openbsd.disk;

import java.util.ArrayList;
import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.HWPartition;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Quartet;

/**
 * Utility class parsing partition information from disklabel command
 */
@ThreadSafe
public final class Disklabel {

    private Disklabel() {
    }

    /**
     * Gets disk and partition information
     *
     * @param diskName
     *            The disk to fetch partition information from
     * @return A quartet containing the disk's name/label, DUID, size, and a list of
     *         partitions
     */
    public static Quartet<String, String, Long, List<HWPartition>> getDiskParams(String diskName) {
        // disklabel (requires root) supports 15 configurable partitions, `a' through
        // `p', excluding `c'.
        // The `c' partition describes the entire physical disk.
        // By convention, the `a' partition of the boot disk is the root
        // partition, and the `b' partition of the boot disk is the swap partition,
        // and the 'i' partition is usually the boot record

        // Create a list for all the other partitions
        List<HWPartition> partitions = new ArrayList<>();
        // Save some values to return to the caller to populate HWDiskStore values
        String totalMarker = "total sectors:";
        long totalSectors = 1L;
        String bpsMarker = "bytes/sector:";
        int bytesPerSector = 1;
        String labelMarker = "label:";
        String label = "";
        String duidMarker = "duid:";
        String duid = "";
        for (String line : ExecutingCommand.runNative("disklabel -n " + diskName)) {
            // Check for values in the header we need for the HWDiskstore
            // # /dev/rsd1c:
            // type: SCSI
            // disk: SCSI disk
            // label: Storage Device
            // duid: 0000000000000000
            // flags:
            // bytes/sector: 512
            // sectors/track: 63
            // tracks/cylinder: 255
            // sectors/cylinder: 16065
            // cylinders: 976
            // total sectors: 15693824
            // boundstart: 0
            // boundend: 15693824
            // drivedata: 0
            if (line.contains(totalMarker)) {
                totalSectors = ParseUtil.getFirstIntValue(line);
            } else if (line.contains(bpsMarker)) {
                bytesPerSector = ParseUtil.getFirstIntValue(line);
            } else if (line.contains(labelMarker)) {
                label = line.split(labelMarker)[1].trim();
            } else if (line.contains(duidMarker)) {
                duid = line.split(duidMarker)[1].trim();
            }
            /*-
            16 partitions:
            #                size           offset  fstype [fsize bsize   cpg]
              a:          2097152             1024  4.2BSD   2048 16384 12958 # /
              b:         17023368          2098176    swap                    # none
              c:        500118192                0  unused
              d:          8388576         19121568  4.2BSD   2048 16384 12958 # /tmp
              e:         41386752         27510144  4.2BSD   2048 16384 12958 # /var
              f:          4194304         68896896  4.2BSD   2048 16384 12958 # /usr
              g:          2097152         73091200  4.2BSD   2048 16384 12958 # /usr/X11R6
              h:         20971520         75188352  4.2BSD   2048 16384 12958 # /usr/local
              i:              960               64   MSDOS
              j:          4194304         96159872  4.2BSD   2048 16384 12958 # /usr/src
              k:         12582912        100354176  4.2BSD   2048 16384 12958 # /usr/obj
              l:        387166336        112937088  4.2BSD   4096 32768 26062 # /home
             Note size is in sectors
             */
            if (line.trim().indexOf(':') == 1) {
                // partition table values have a single letter followed by a colon
                String[] split = ParseUtil.whitespaces.split(line.trim(), 9);
                String name = split[0].substring(0, 1);
                // get major and minor from stat
                Pair<Integer, Integer> majorMinor = getMajorMinor(diskName, name);
                // add partitions
                if (split.length > 4) {
                    partitions.add(new HWPartition(diskName + name, name, split[3], duid + "." + name,
                            ParseUtil.parseLongOrDefault(split[1], 0L) * bytesPerSector, majorMinor.getA(),
                            majorMinor.getB(), split.length > 5 ? split[split.length - 1] : ""));
                }
            }
        }
        if (partitions.isEmpty()) {
            return getDiskParamsNoRoot(diskName);
        }
        return new Quartet<>(label, duid, totalSectors * bytesPerSector, partitions);
    }

    private static Quartet<String, String, Long, List<HWPartition>> getDiskParamsNoRoot(String diskName) {
        List<HWPartition> partitions = new ArrayList<>();
        for (String line : ExecutingCommand.runNative("df")) {
            if (line.startsWith("/dev/" + diskName)) {
                String[] split = ParseUtil.whitespaces.split(line);
                String name = split[0].substring(5 + diskName.length());
                Pair<Integer, Integer> majorMinor = getMajorMinor(diskName, name);
                if (split.length > 5) {
                    long partSize = ParseUtil.parseLongOrDefault(split[1], 1L) * 512L;
                    partitions.add(new HWPartition(split[0], split[0].substring(5), Constants.UNKNOWN,
                            Constants.UNKNOWN, partSize, majorMinor.getA(), majorMinor.getB(), split[5]));
                }
            }
        }
        return new Quartet<>(Constants.UNKNOWN, Constants.UNKNOWN, 0L, partitions);
    }

    private static Pair<Integer, Integer> getMajorMinor(String diskName, String name) {
        int major = 0;
        int minor = 0;
        String majorMinor = ExecutingCommand.getFirstAnswer("stat -f %Hr,%Lr /dev/" + diskName + name);
        int comma = majorMinor.indexOf(',');
        if (comma > 0 && comma < majorMinor.length()) {
            major = ParseUtil.parseIntOrDefault(majorMinor.substring(0, comma), 0);
            minor = ParseUtil.parseIntOrDefault(majorMinor.substring(comma + 1), 0);
        }
        return new Pair<>(major, minor);
    }
}
