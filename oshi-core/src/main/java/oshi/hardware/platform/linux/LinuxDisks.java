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
package oshi.hardware.platform.linux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.jna.platform.linux.Udev;
import oshi.jna.platform.linux.Udev.UdevContext;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.platform.linux.ProcPath;

/**
 * Linux hard disk implementation.
 */
@ThreadSafe
public final class LinuxDisks {

    private static final int SECTORSIZE = 512;

    private LinuxDisks() {
    }

    // Get a list of orders to pass to ParseUtil
    private static final int[] UDEV_STAT_ORDERS = new int[UdevStat.values().length];
    static {
        for (UdevStat stat : UdevStat.values()) {
            UDEV_STAT_ORDERS[stat.ordinal()] = stat.getOrder();
        }
    }

    // There are at least 11 elements in udev stat output or sometimes 15. We want
    // the rightmost 11 or 15 if there is leading text.
    private static final int UDEV_STAT_LENGTH;
    static {
        String stat = FileUtil.getStringFromFile(ProcPath.DISKSTATS);
        int statLength = 11;
        if (!stat.isEmpty()) {
            statLength = ParseUtil.countStringToLongArray(stat, ' ');
        }
        UDEV_STAT_LENGTH = statLength;
    }

    /**
     * Gets the disks on this machine
     *
     * @return an array of {@link HWDiskStore} objects representing the disks
     */
    public static HWDiskStore[] getDisks() {
        return getDisks(null);
    }

    private static HWDiskStore[] getDisks(HWDiskStore storeToUpdate) {
        HWDiskStore store = null;
        List<HWDiskStore> result = new ArrayList<>();

        Map<String, String> mountsMap = readMountsMap();

        UdevContext handle = Udev.INSTANCE.udev_new();
        Udev.UdevEnumerate enumerate = Udev.INSTANCE.udev_enumerate_new(handle);
        Udev.INSTANCE.udev_enumerate_add_match_subsystem(enumerate, "block");
        Udev.INSTANCE.udev_enumerate_scan_devices(enumerate);

        Udev.UdevListEntry entry = Udev.INSTANCE.udev_enumerate_get_list_entry(enumerate);
        Udev.UdevDevice device;
        while ((device = Udev.INSTANCE.udev_device_new_from_syspath(handle,
                Udev.INSTANCE.udev_list_entry_get_name(entry))) != null) {
            String devnode = Udev.INSTANCE.udev_device_get_devnode(device);
            // Ignore loopback and ram disks; do nothing
            if (devnode != null && !devnode.startsWith("/dev/loop") && !devnode.startsWith("/dev/ram")) {
                if ("disk".equals(Udev.INSTANCE.udev_device_get_devtype(device))) {
                    store = new HWDiskStore();
                    store.setName(devnode);

                    // Avoid model and serial in virtual environments
                    store.setModel(Udev.INSTANCE.udev_device_get_property_value(device, "ID_MODEL") == null ? "Unknown"
                            : Udev.INSTANCE.udev_device_get_property_value(device, "ID_MODEL"));
                    store.setSerial(
                            Udev.INSTANCE.udev_device_get_property_value(device, "ID_SERIAL_SHORT") == null ? "Unknown"
                                    : Udev.INSTANCE.udev_device_get_property_value(device, "ID_SERIAL_SHORT"));

                    store.setSize(ParseUtil.parseLongOrDefault(
                            Udev.INSTANCE.udev_device_get_sysattr_value(device, "size"), 0L) * SECTORSIZE);

                    if (storeToUpdate == null) {
                        // If getting all stores, add to the list with stats
                        store.setPartitions(new HWPartition[0]);
                        computeDiskStats(store, device);
                        result.add(store);
                    } else if (store.equals(storeToUpdate)) {
                        // Note this equality test is not object equality. If we are only updating a
                        // single disk, the name, model, serial, and size are sufficient to test if this
                        // is a match. Add the (old) object, release handle and return.
                        computeDiskStats(storeToUpdate, device);
                        result.add(storeToUpdate);
                        Udev.INSTANCE.udev_device_unref(device);
                        break;
                    }
                } else if ("partition".equals(Udev.INSTANCE.udev_device_get_devtype(device)) && store != null) {
                    // `store` should still point to the HWDiskStore this
                    // partition is attached to. If not, it's an error, so
                    // skip.
                    HWPartition[] partArray = new HWPartition[store.getPartitions().length + 1];
                    System.arraycopy(store.getPartitions(), 0, partArray, 0, store.getPartitions().length);
                    String name = Udev.INSTANCE.udev_device_get_devnode(device);
                    partArray[partArray.length - 1] = new HWPartition(name,
                            Udev.INSTANCE.udev_device_get_sysname(device),
                            Udev.INSTANCE.udev_device_get_property_value(device, "ID_FS_TYPE") == null ? "partition"
                                    : Udev.INSTANCE.udev_device_get_property_value(device, "ID_FS_TYPE"),
                            Udev.INSTANCE.udev_device_get_property_value(device, "ID_FS_UUID") == null ? ""
                                    : Udev.INSTANCE.udev_device_get_property_value(device, "ID_FS_UUID"),
                            ParseUtil.parseLongOrDefault(Udev.INSTANCE.udev_device_get_sysattr_value(device, "size"),
                                    0L) * SECTORSIZE,
                            ParseUtil.parseIntOrDefault(Udev.INSTANCE.udev_device_get_property_value(device, "MAJOR"),
                                    0),
                            ParseUtil.parseIntOrDefault(Udev.INSTANCE.udev_device_get_property_value(device, "MINOR"),
                                    0),
                            mountsMap.getOrDefault(name, ""));
                    store.setPartitions(partArray);
                }
            }
            Udev.INSTANCE.udev_device_unref(device);
            entry = Udev.INSTANCE.udev_list_entry_get_next(entry);
        }
        Udev.INSTANCE.udev_enumerate_unref(enumerate);
        Udev.INSTANCE.udev_unref(handle);

        return result.toArray(new HWDiskStore[0]);
    }

    /**
     * Updates the statistics on a disk store.
     *
     * @param diskStore
     *            the {@link oshi.hardware.HWDiskStore} to update.
     * @return {@code true} if the update was (probably) successful.
     */
    public static boolean updateDiskStats(HWDiskStore diskStore) {
        // If this returns non-empty (the same store, but updated) then we were
        // successful in the update
        return 0 < getDisks(diskStore).length;
    }

    private static Map<String, String> readMountsMap() {
        Map<String, String> mountsMap = new HashMap<>();
        List<String> mounts = FileUtil.readFile(ProcPath.MOUNTS);
        for (String mount : mounts) {
            String[] split = ParseUtil.whitespaces.split(mount);
            if (split.length < 2 || !split[0].startsWith("/dev/")) {
                continue;
            }
            mountsMap.put(split[0], split[1]);
        }
        return mountsMap;
    }

    private static void computeDiskStats(HWDiskStore store, Udev.UdevDevice disk) {
        String devstat = Udev.INSTANCE.udev_device_get_sysattr_value(disk, "stat");
        long[] devstatArray = ParseUtil.parseStringToLongArray(devstat, UDEV_STAT_ORDERS, UDEV_STAT_LENGTH, ' ');
        store.setTimeStamp(System.currentTimeMillis());

        // Reads and writes are converted in bytes
        store.setReads(devstatArray[UdevStat.READS.ordinal()]);
        store.setReadBytes(devstatArray[UdevStat.READ_BYTES.ordinal()] * SECTORSIZE);
        store.setWrites(devstatArray[UdevStat.WRITES.ordinal()]);
        store.setWriteBytes(devstatArray[UdevStat.WRITE_BYTES.ordinal()] * SECTORSIZE);
        store.setCurrentQueueLength(devstatArray[UdevStat.QUEUE_LENGTH.ordinal()]);
        store.setTransferTime(devstatArray[UdevStat.ACTIVE_MS.ordinal()]);
    }

    // Order the field is in udev stats
    enum UdevStat {
        // The parsing implementation in ParseUtil requires these to be declared
        // in increasing order. Use 0-ordered index here
        READS(0), READ_BYTES(2), WRITES(4), WRITE_BYTES(6), QUEUE_LENGTH(8), ACTIVE_MS(9);

        private int order;

        public int getOrder() {
            return this.order;
        }

        UdevStat(int order) {
            this.order = order;
        }
    }
}
