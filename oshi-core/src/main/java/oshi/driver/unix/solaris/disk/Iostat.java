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
package oshi.driver.unix.solaris.disk;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.tuples.Quintet;

/**
 * Utility to query iostat
 */
@ThreadSafe
public final class Iostat {

    // Note uppercase E
    private static final String IOSTAT_ER_DETAIL = "iostat -Er";

    // Note lowercase e
    private static final String IOSTAT_ER = "iostat -er";
    // Sample output:
    // errors
    // device,s/w,h/w,trn,tot
    // cmdk0,0,0,0,0
    // sd0,0,0,0

    // Note lowercase e
    private static final String IOSTAT_ERN = "iostat -ern";
    // Sample output:
    // errors
    // s/w,h/w,trn,tot,device
    // 0,0,0,0,c1d0
    // 0,0,0,0,c1t1d0

    private static final String DEVICE_HEADER = "device";

    private Iostat() {
    }

    /**
     * Query iostat to map partitions to mount points
     *
     * @return A map with partitions as the key and mount points as the value
     */
    public static Map<String, String> queryPartitionToMountMap() {
        // Create map to correlate disk name with block device mount point for
        // later use in partition info
        Map<String, String> deviceMap = new HashMap<>();

        // First, run iostat -er to enumerate disks by name.
        List<String> mountNames = ExecutingCommand.runNative(IOSTAT_ER);
        // Also run iostat -ern to get the same list by mount point.
        List<String> mountPoints = ExecutingCommand.runNative(IOSTAT_ERN);

        String disk;
        for (int i = 0; i < mountNames.size() && i < mountPoints.size(); i++) {
            // Map disk
            disk = mountNames.get(i);
            String[] diskSplit = disk.split(",");
            if (diskSplit.length >= 5 && !DEVICE_HEADER.equals(diskSplit[0])) {
                String mount = mountPoints.get(i);
                String[] mountSplit = mount.split(",");
                if (mountSplit.length >= 5 && !DEVICE_HEADER.equals(mountSplit[4])) {
                    deviceMap.put(diskSplit[0], mountSplit[4]);
                }
            }
        }
        return deviceMap;
    }

    /**
     * Query iostat to map detailed drive information
     *
     * @param diskSet
     *            A set of valid disk names; others will be ignored
     * @return A map with disk name as the key and a quintet of model, vendor,
     *         product, serial, size as the value
     */
    public static Map<String, Quintet<String, String, String, String, Long>> queryDeviceStrings(Set<String> diskSet) {
        Map<String, Quintet<String, String, String, String, Long>> deviceParamMap = new HashMap<>();
        // Run iostat -Er to get model, etc.
        List<String> iostat = ExecutingCommand.runNative(IOSTAT_ER_DETAIL);
        // We'll use Model if available, otherwise Vendor+Product
        String diskName = null;
        String model = "";
        String vendor = "";
        String product = "";
        String serial = "";
        long size = 0;
        for (String line : iostat) {
            // The -r switch enables comma delimited for easy parsing!
            // No guarantees on which line the results appear so we'll nest
            // a loop iterating on the comma splits
            String[] split = line.split(",");
            for (String keyValue : split) {
                keyValue = keyValue.trim();
                // If entry is tne name of a disk, this is beginning of new
                // output for that disk.
                if (diskSet.contains(keyValue)) {
                    // First, if we have existing output from previous,
                    // update
                    if (diskName != null) {
                        deviceParamMap.put(diskName, new Quintet<>(model, vendor, product, serial, size));
                    }
                    // Reset values for next iteration
                    diskName = keyValue;
                    model = "";
                    vendor = "";
                    product = "";
                    serial = "";
                    size = 0L;
                    continue;
                }
                // Otherwise update variables
                if (keyValue.startsWith("Model:")) {
                    model = keyValue.replace("Model:", "").trim();
                } else if (keyValue.startsWith("Serial No:")) {
                    serial = keyValue.replace("Serial No:", "").trim();
                } else if (keyValue.startsWith("Vendor:")) {
                    vendor = keyValue.replace("Vendor:", "").trim();
                } else if (keyValue.startsWith("Product:")) {
                    product = keyValue.replace("Product:", "").trim();
                } else if (keyValue.startsWith("Size:")) {
                    // Size: 1.23GB <1227563008 bytes>
                    String[] bytes = keyValue.split("<");
                    if (bytes.length > 1) {
                        bytes = ParseUtil.whitespaces.split(bytes[1]);
                        size = ParseUtil.parseLongOrDefault(bytes[0], 0L);
                    }
                }
            }
            // At end of output update last entry
            if (diskName != null) {
                deviceParamMap.put(diskName, new Quintet<>(model, vendor, product, serial, size));
            }
        }
        return deviceParamMap;
    }
}
