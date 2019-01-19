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
package oshi.hardware.platform.linux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.hardware.Disks;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.jna.platform.linux.Udev;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;

/**
 * Linux hard disk implementation.
 *
 * @author enrico[dot]bianchi[at]gmail[dot]com
 */
public class LinuxDisks implements Disks {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(LinuxDisks.class);

    private static final int SECTORSIZE = 512;

    private final Map<String, String> mountsMap = new HashMap<>();

    private static final Map<Integer, String> hashCodeToPathMap = new HashMap<>();

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

    // Get a list of orders to pass to ParseUtil
    private static final int[] UDEV_STAT_ORDERS = new int[UdevStat.values().length];
    static {
        for (UdevStat stat : UdevStat.values()) {
            UDEV_STAT_ORDERS[stat.ordinal()] = stat.getOrder();
        }
    }

    // There are at least 11 elements in udev stat output. Some platforms have
    // 12 but we want the last 11. ParseUtil works from the right
    private static final int UDEV_STAT_LENGTH = 11;

    /**
     * {@inheritDoc}
     */
    @Override
    public HWDiskStore[] getDisks() {
        HWDiskStore store = null;
        List<HWDiskStore> result;

        updateMountsMap();
        hashCodeToPathMap.clear();

        Udev.UdevHandle handle = null;
        Udev.UdevDevice device = null;
        Udev.UdevEnumerate enumerate = null;
        Udev.UdevListEntry entry;
        Udev.UdevListEntry oldEntry;

        result = new ArrayList<>();

        handle = Udev.INSTANCE.udev_new();
        enumerate = Udev.INSTANCE.udev_enumerate_new(handle);
        Udev.INSTANCE.udev_enumerate_add_match_subsystem(enumerate, "block");
        Udev.INSTANCE.udev_enumerate_scan_devices(enumerate);

        entry = Udev.INSTANCE.udev_enumerate_get_list_entry(enumerate);
        while (true) {
            try {
                oldEntry = entry;
                device = Udev.INSTANCE.udev_device_new_from_syspath(handle,
                        Udev.INSTANCE.udev_list_entry_get_name(entry));
                // Ignore loopback and ram disks; do nothing
                if (!Udev.INSTANCE.udev_device_get_devnode(device).startsWith("/dev/loop")
                        && !Udev.INSTANCE.udev_device_get_devnode(device).startsWith("/dev/ram")) {
                    if ("disk".equals(Udev.INSTANCE.udev_device_get_devtype(device))) {
                        store = new HWDiskStore();
                        store.setName(Udev.INSTANCE.udev_device_get_devnode(device));

                        // Avoid model and serial in virtual environments
                        store.setModel(
                                Udev.INSTANCE.udev_device_get_property_value(device, "ID_MODEL") == null ? "Unknown"
                                        : Udev.INSTANCE.udev_device_get_property_value(device, "ID_MODEL"));
                        store.setSerial(Udev.INSTANCE.udev_device_get_property_value(device, "ID_SERIAL_SHORT") == null
                                ? "Unknown"
                                : Udev.INSTANCE.udev_device_get_property_value(device, "ID_SERIAL_SHORT"));

                        store.setSize(ParseUtil.parseLongOrDefault(
                                Udev.INSTANCE.udev_device_get_sysattr_value(device, "size"), 0L) * SECTORSIZE);
                        store.setPartitions(new HWPartition[0]);
                        computeDiskStats(store, device);

                        hashCodeToPathMap.put(store.hashCode(), Udev.INSTANCE.udev_list_entry_get_name(entry));
                        result.add(store);
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
                                ParseUtil.parseLongOrDefault(
                                        Udev.INSTANCE.udev_device_get_sysattr_value(device, "size"), 0L) * SECTORSIZE,
                                ParseUtil.parseIntOrDefault(
                                        Udev.INSTANCE.udev_device_get_property_value(device, "MAJOR"), 0),
                                ParseUtil.parseIntOrDefault(
                                        Udev.INSTANCE.udev_device_get_property_value(device, "MINOR"), 0),
                                mountsMap.getOrDefault(name, ""));
                        store.setPartitions(partArray);
                    }
                }
                entry = Udev.INSTANCE.udev_list_entry_get_next(oldEntry);
            } catch (NullPointerException ex) { // NOSONAR squid:S1166
                LOG.debug("Reached all disks. Exiting ");
                break;
            } finally {
                Udev.INSTANCE.udev_device_unref(device);
            }
        }

        Udev.INSTANCE.udev_enumerate_unref(enumerate);
        Udev.INSTANCE.udev_unref(handle);

        return result.toArray(new HWDiskStore[result.size()]);
    }

    /**
     * {@inheritDoc}
     */
    public static boolean updateDiskStats(HWDiskStore diskStore) {
        String path = hashCodeToPathMap.get(diskStore.hashCode());

        Udev.UdevHandle handle = Udev.INSTANCE.udev_new();
        Udev.UdevDevice device = Udev.INSTANCE.udev_device_new_from_syspath(handle, path);

        boolean update = false;
        if (device != null) {
            computeDiskStats(diskStore, device);
            update = true;
            Udev.INSTANCE.udev_device_unref(device);
        }
        Udev.INSTANCE.udev_unref(handle);
        return update;
    }

    private void updateMountsMap() {
        this.mountsMap.clear();
        List<String> mounts = FileUtil.readFile("/proc/self/mounts");
        for (String mount : mounts) {
            String[] split = ParseUtil.whitespaces.split(mount);
            if (split.length < 2 || !split[0].startsWith("/dev/")) {
                continue;
            }
            this.mountsMap.put(split[0], split[1]);
        }
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
}
