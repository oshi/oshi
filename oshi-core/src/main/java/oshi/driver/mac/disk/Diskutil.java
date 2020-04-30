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
package oshi.driver.mac.disk;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Utility to query diskutil
 */
@ThreadSafe
public final class Diskutil {

    private static final String DISKUTIL_CS_LIST = "diskutil cs list";
    private static final String LOGICAL_VOLUME_FAMILY = "Logical Volume Family";
    private static final String LOGICAL_VOLUME_GROUP = "Logical Volume Group";

    private Diskutil() {
    }

    /**
     * Query diskutil to map logical volumes
     *
     * @return A map with physical volume as the key and logical volume as the value
     */
    public static Map<String, String> queryLogicalVolumeMap() {
        Map<String, String> logicalVolumeMap = new HashMap<>();
        // Parse `diskutil cs list` to populate logical volume map
        Set<String> physicalVolumes = new HashSet<>();
        boolean logicalVolume = false;
        for (String line : ExecutingCommand.runNative(DISKUTIL_CS_LIST)) {
            if (line.contains(LOGICAL_VOLUME_GROUP)) {
                // Logical Volume Group defines beginning of grouping which will
                // list multiple physical volumes followed by the logical volume
                // they are associated with. Each physical volume will be a key
                // with the logical volume as its value, but since the value
                // doesn't appear until the end we collect the keys in a list
                physicalVolumes.clear();
                logicalVolume = false;
            } else if (line.contains(LOGICAL_VOLUME_FAMILY)) {
                // Done collecting physical volumes, prepare to store logical
                // volume
                logicalVolume = true;
            } else if (line.contains("Disk:")) {
                String volume = ParseUtil.parseLastString(line);
                if (logicalVolume) {
                    // Store this disk as the logical volume value for all the
                    // physical volume keys
                    for (String pv : physicalVolumes) {
                        logicalVolumeMap.put(pv, volume);
                    }
                    physicalVolumes.clear();
                } else {
                    physicalVolumes.add(ParseUtil.parseLastString(line));
                }
            }
        }
        return logicalVolumeMap;
    }
}
