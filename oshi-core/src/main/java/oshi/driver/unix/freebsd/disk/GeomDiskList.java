/*
 * MIT License
 *
 * Copyright (c) 2020-2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.driver.unix.freebsd.disk;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.tuples.Triplet;

/**
 * Utility to query geom part list
 */
@ThreadSafe
public final class GeomDiskList {

    private static final String GEOM_DISK_LIST = "geom disk list";

    private GeomDiskList() {
    }

    /**
     * Queries disk data using geom
     *
     * @return A map with disk name as the key and a Triplet of model, serial, and
     *         size as the value
     */
    public static Map<String, Triplet<String, String, Long>> queryDisks() {
        // Map of device name to disk, to be returned
        Map<String, Triplet<String, String, Long>> diskMap = new HashMap<>();
        // Parameters needed.
        String diskName = null; // Non-null identifies a valid partition
        String descr = Constants.UNKNOWN;
        String ident = Constants.UNKNOWN;
        long mediaSize = 0L;

        List<String> geom = ExecutingCommand.runNative(GEOM_DISK_LIST);
        for (String line : geom) {
            line = line.trim();
            // Marks the DiskStore device
            if (line.startsWith("Geom name:")) {
                // Save any previous disk in the map
                if (diskName != null) {
                    diskMap.put(diskName, new Triplet<>(descr, ident, mediaSize));
                    descr = Constants.UNKNOWN;
                    ident = Constants.UNKNOWN;
                    mediaSize = 0L;
                }
                // Now use new diskName
                diskName = line.substring(line.lastIndexOf(' ') + 1);
            }
            // If we don't have a valid store, don't bother parsing anything
            if (diskName != null) {
                line = line.trim();
                if (line.startsWith("Mediasize:")) {
                    String[] split = ParseUtil.whitespaces.split(line);
                    if (split.length > 1) {
                        mediaSize = ParseUtil.parseLongOrDefault(split[1], 0L);
                    }
                }
                if (line.startsWith("descr:")) {
                    descr = line.replace("descr:", "").trim();
                }
                if (line.startsWith("ident:")) {
                    ident = line.replace("ident:", "").replace("(null)", "").trim();
                }
            }
        }
        if (diskName != null) {
            diskMap.put(diskName, new Triplet<>(descr, ident, mediaSize));
        }
        return diskMap;
    }
}
