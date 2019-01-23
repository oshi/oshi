/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.hardware.platform.unix.solaris;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.sun.jna.platform.unix.solaris.LibKstat.Kstat; //NOSONAR
import com.sun.jna.platform.unix.solaris.LibKstat.KstatIO;

import oshi.hardware.Disks;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.solaris.KstatUtil;

/**
 * Solaris hard disk implementation.
 *
 * @author widdis[at]gmail[dot]com
 */
public class SolarisDisks implements Disks {

    private static final long serialVersionUID = 1L;

    public static boolean updateDiskStats(HWDiskStore diskStore) {
        Kstat ksp = KstatUtil.kstatLookup(null, 0, diskStore.getName());
        if (ksp != null && KstatUtil.kstatRead(ksp)) {
            KstatIO data = new KstatIO(ksp.ks_data);
            diskStore.setReads(data.reads);
            diskStore.setWrites(data.writes);
            diskStore.setReadBytes(data.nread);
            diskStore.setWriteBytes(data.nwritten);
            diskStore.setCurrentQueueLength((long) data.wcnt + data.rcnt);
            // rtime and snaptime are nanoseconds, convert to millis
            diskStore.setTransferTime(data.rtime / 1_000_000L);
            diskStore.setTimeStamp(ksp.ks_snaptime / 1_000_000L);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public HWDiskStore[] getDisks() {
        // Create map indexed by device name for multiple command reference
        Map<String, HWDiskStore> diskMap = new HashMap<>();
        // First, run iostat -er to enumerate disks by name. Sample output:
        // errors
        // device,s/w,h/w,trn,tot
        // cmdk0,0,0,0,0
        // sd0,0,0,0
        List<String> disks = ExecutingCommand.runNative("iostat -er");

        // Create map to correlate disk name with block device mount point for
        // later use in partition info
        Map<String, String> deviceMap = new HashMap<>();
        // Also run iostat -ern to get the same list by mount point. Sample
        // output:
        // errors
        // s/w,h/w,trn,tot,device
        // 0,0,0,0,c1d0
        // 0,0,0,0,c1t1d0
        List<String> mountpoints = ExecutingCommand.runNative("iostat -ern");
        String disk;
        for (int i = 0; i < disks.size() && i < mountpoints.size(); i++) {
            // Map disk
            disk = disks.get(i);
            String[] diskSplit = disk.split(",");
            if (diskSplit.length < 5 || "device".equals(diskSplit[0])) {
                continue;
            }
            HWDiskStore store = new HWDiskStore();
            store.setName(diskSplit[0]);
            diskMap.put(diskSplit[0], store);
            // Map mount
            String mount = mountpoints.get(i);
            String[] mountSplit = mount.split(",");
            if (mountSplit.length < 5 || "device".equals(mountSplit[4])) {
                continue;
            }
            deviceMap.put(diskSplit[0], mountSplit[4]);
        }

        // Create map to correlate disk name with blick device mount point for
        // later use in partition info
        Map<String, Integer> majorMap = new HashMap<>();
        // Run lshal, if available, to get block device major (we'll use
        // partition # for minor)
        List<String> lshal = ExecutingCommand.runNative("lshal");
        disk = "";
        for (String line : lshal) {
            if (line.startsWith("udi ")) {
                String udi = ParseUtil.getSingleQuoteStringValue(line);
                disk = udi.substring(udi.lastIndexOf('/') + 1);
                continue;
            }
            line = line.trim();
            if (line.startsWith("block.major")) {
                majorMap.put(disk, ParseUtil.getFirstIntValue(line));
            }
        }

        // Next, run iostat -Er to get model, etc.
        disks = ExecutingCommand.runNative("iostat -Er");
        // We'll use Model if available, otherwise Vendor+Product
        disk = "";
        String model = "";
        String vendor = "";
        String product = "";
        String serial = "";
        long size = 0;
        for (String line : disks) {
            // The -r switch enables comma delimited for easy parsing!
            // No guarantees on which line the results appear so we'll nest
            // a loop iterating on the comma splits
            String[] split = line.split(",");
            for (String keyValue : split) {
                keyValue = keyValue.trim();
                // If entry is tne name of a disk, this is beginning of new
                // output for that disk.
                if (diskMap.keySet().contains(keyValue)) {
                    // First, if we have existing output from previous,
                    // update
                    if (!disk.isEmpty()) {
                        updateStore(diskMap.get(disk), model, vendor, product, serial, size, deviceMap.get(disk),
                                majorMap.getOrDefault(disk, 0));
                    }
                    // Reset values for next iteration
                    disk = keyValue;
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
            if (!disk.isEmpty()) {
                updateStore(diskMap.get(disk), model, vendor, product, serial, size, deviceMap.get(disk),
                        majorMap.getOrDefault(disk, 0));
            }
        }

        // Finally use kstat to get reads/writes
        // simultaneously populate result array
        HWDiskStore[] results = new HWDiskStore[diskMap.keySet().size()];
        int index = 0;
        for (Entry<String, HWDiskStore> entry : diskMap.entrySet()) {
            updateDiskStats(entry.getValue());
            results[index++] = entry.getValue();
        }

        return results;
    }

    /**
     * Updates the HWDiskStore. If model name is nonempty it is used, otherwise
     * vendor+product are used for model
     *
     * @param store
     *            A HWDiskStore
     * @param model
     *            model name, or empty string if none
     * @param vendor
     *            vendor name, or empty string if none
     * @param product
     *            product nmae, or empty string if none
     * @param serial
     *            serial number, or empty string if none
     * @param size
     *            size of the drive in bytes
     * @param mount
     *            The mount point of this store, used to fetch partition info
     * @param major
     *            The major device number for the partition
     */
    private void updateStore(HWDiskStore store, String model, String vendor, String product, String serial, long size,
            String mount, int major) {
        store.setModel(model.isEmpty() ? (vendor + " " + product).trim() : model);
        store.setSerial(serial);
        store.setSize(size);

        // Temporary list to hold partitions
        List<HWPartition> partList = new ArrayList<>();

        // Now grab prtvotc output for partitions
        // This requires sudo permissions; will result in "permission denied
        // otherwise" in which case we return empty partition list
        List<String> prtvotc = ExecutingCommand.runNative("prtvtoc /dev/dsk/" + mount);
        // Sample output - see man prtvtoc
        if (prtvotc.size() > 1) {
            int bytesPerSector = 0;
            String[] split;
            // We have a result, parse partition table
            for (String line : prtvotc) {
                // If line starts with asterisk we ignore except for the one
                // specifying bytes per sector
                if (line.startsWith("*")) {
                    if (line.endsWith("bytes/sector")) {
                        split = ParseUtil.whitespaces.split(line);
                        if (split.length > 0) {
                            bytesPerSector = ParseUtil.parseIntOrDefault(split[1], 0);
                        }
                    }
                    continue;
                }
                // If bytes/sector is still 0, these are not real partitions so
                // ignore.
                if (bytesPerSector == 0) {
                    continue;
                }
                // Lines without asterisk have 6 or 7 whitespaces-split values
                // representing (last field optional):
                // Partition Tag Flags Sector Count Sector Mount
                split = ParseUtil.whitespaces.split(line.trim());
                // Partition 2 is always the whole disk so we ignore it
                if (split.length < 6 || "2".equals(split[0])) {
                    continue;
                }
                HWPartition partition = new HWPartition();
                // First field is partition number
                partition.setIdentification(mount + "s" + split[0]);
                partition.setMajor(major);
                partition.setMinor(ParseUtil.parseIntOrDefault(split[0], 0));
                // Second field is tag. Parse:
                switch (ParseUtil.parseIntOrDefault(split[1], 0)) {
                case 0x01:
                case 0x18:
                    partition.setName("boot");
                    break;
                case 0x02:
                    partition.setName("root");
                    break;
                case 0x03:
                    partition.setName("swap");
                    break;
                case 0x04:
                    partition.setName("usr");
                    break;
                case 0x05:
                    partition.setName("backup");
                    break;
                case 0x06:
                    partition.setName("stand");
                    break;
                case 0x07:
                    partition.setName("var");
                    break;
                case 0x08:
                    partition.setName("home");
                    break;
                case 0x09:
                    partition.setName("altsctr");
                    break;
                case 0x0a:
                    partition.setName("cache");
                    break;
                case 0x0b:
                    partition.setName("reserved");
                    break;
                case 0x0c:
                    partition.setName("system");
                    break;
                case 0x0e:
                    partition.setName("public region");
                    break;
                case 0x0f:
                    partition.setName("private region");
                    break;
                default:
                    partition.setName("unknown");
                    break;
                }
                // Third field is flags.
                // First character writable, second is mountable
                switch (split[2]) {
                case "00":
                    partition.setType("wm");
                    break;
                case "10":
                    partition.setType("rm");
                    break;
                case "01":
                    partition.setType("wu");
                    break;
                default:
                    partition.setType("ru");
                    break;
                }
                // Fifth field is sector count
                partition.setSize(bytesPerSector * ParseUtil.parseLongOrDefault(split[4], 0L));
                // Seventh field (if present) is mount point
                if (split.length > 6) {
                    partition.setMountPoint(split[6]);
                }
                partList.add(partition);
            }
            store.setPartitions(partList.toArray(new HWPartition[0]));
        }
    }
}
