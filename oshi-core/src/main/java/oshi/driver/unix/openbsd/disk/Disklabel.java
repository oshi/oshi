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
package oshi.driver.unix.openbsd.disk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import oshi.hardware.HWPartition;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;

public final class Disklabel {

    private Disklabel() {
    }

    /*
    * └─ $ ▶ doas disklabel sd1
doas (mprins@thinkpad.local) password:
# /dev/rsd1c:
type: SCSI
disk: SCSI disk
label: Storage Device
duid: 0000000000000000
flags:
bytes/sector: 512
sectors/track: 63
tracks/cylinder: 255
sectors/cylinder: 16065
cylinders: 976
total sectors: 15693824
boundstart: 0
boundend: 15693824
drivedata: 0

*/
    public static Pair<Map<String, String>, List<HWPartition>> getDiskParams(String diskName) {
        List<HWPartition> partitions = new ArrayList<>();
        HashMap<String, String> params = new HashMap<>();
        Pair<Map<String, String>, List<HWPartition>> pair = new Pair<>(params, partitions);
        // disklabel (requires root) supports 15 configurable partitions, `a' through `p', excluding `c'.
        // The `c' partition describes the entire physical disk.
        // By convention, the `a' partition of the boot disk is the root
        // partition, and the `b' partition of the boot disk is the swap partition
        // List<String> cmd = ExecutingCommand.runNative("doas disklabel -n " + diskName);
        List<String> cmd = ExecutingCommand.runNative("disklabel -n " + diskName);
        if (cmd.size() > 0) {
            String[] split;
            String line;
            ListIterator<String> lines = cmd.listIterator();
            while (lines.hasNext()) {
                line = lines.next();
                if (line.startsWith("16 partitions:")) {
                    break;
                }
                split = line.split(":");
                if (split.length == 2) {
                    params.put(split[0], split[1].trim());
                }
            }
            // should be just the partition table left
            HWPartition p;
            String name;
            while (lines.hasNext()) {
                line = lines.next();
                //16 partitions:
                //#                size           offset  fstype [fsize bsize   cpg]
                //  a:          2097152             1024  4.2BSD   2048 16384 12958 # /
                //  b:         17023368          2098176    swap                    # none
                //  c:        500118192                0  unused
                //  d:          8388576         19121568  4.2BSD   2048 16384 12958 # /tmp
                //  e:         41386752         27510144  4.2BSD   2048 16384 12958 # /var
                //  f:          4194304         68896896  4.2BSD   2048 16384 12958 # /usr
                //  g:          2097152         73091200  4.2BSD   2048 16384 12958 # /usr/X11R6
                //  h:         20971520         75188352  4.2BSD   2048 16384 12958 # /usr/local
                //  i:              960               64   MSDOS
                //  j:          4194304         96159872  4.2BSD   2048 16384 12958 # /usr/src
                //  k:         12582912        100354176  4.2BSD   2048 16384 12958 # /usr/obj
                //  l:        387166336        112937088  4.2BSD   4096 32768 26062 # /home
                // Note size is in sectors
                if (line.startsWith("#") || line.isEmpty()) {
                    continue;
                }
                split = ParseUtil.whitespaces.split(line.trim(), 9);
                name = split[0].replaceAll(":", "");
                if (split.length > 5) {
                    p = new HWPartition(diskName + name, name, split[3], Constants.UNKNOWN,
                        ParseUtil.parseLongOrDefault(split[1], -1) * ParseUtil
                            .parseLongOrDefault(params.get("bytes/sector"), 1), -1, -1,
                        split[split.length - 1]);
                    partitions.add(p);
                } else if (split.length == 4) {
                    p = new HWPartition(diskName + name, name, split[3], Constants.UNKNOWN,
                        ParseUtil.parseLongOrDefault(split[1], -1) * ParseUtil
                            .parseLongOrDefault(params.get("bytes/sector"), -1), -1, -1, "");
                    partitions.add(p);
                }
            }
        }
        return pair;
    }
}
