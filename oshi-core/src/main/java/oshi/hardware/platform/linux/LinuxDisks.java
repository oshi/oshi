/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2016 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
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

    @Override
    public HWDiskStore[] getDisks() {
        HWDiskStore store = null;
        List<HWDiskStore> result;

        updateMountsMap();

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
                if (Udev.INSTANCE.udev_device_get_devnode(device).startsWith("/dev/loop")
                        || Udev.INSTANCE.udev_device_get_devnode(device).startsWith("/dev/ram")) {
                    // Ignore loopback and ram disks; do nothing
                } else if ("disk".equals(Udev.INSTANCE.udev_device_get_devtype(device))) {
                    store = new HWDiskStore();
                    store.setName(Udev.INSTANCE.udev_device_get_devnode(device));

                    // Avoid model and serial in virtual environments
                    store.setModel(Udev.INSTANCE.udev_device_get_property_value(device, "ID_MODEL") == null ? "Unknown"
                            : Udev.INSTANCE.udev_device_get_property_value(device, "ID_MODEL"));
                    store.setSerial(Udev.INSTANCE.udev_device_get_property_value(device, "ID_SERIAL_SHORT") == null
                            ? "Unknown" : Udev.INSTANCE.udev_device_get_property_value(device, "ID_SERIAL_SHORT"));

                    store.setSize(ParseUtil.parseLongOrDefault(
                            Udev.INSTANCE.udev_device_get_sysattr_value(device, "size"), 0L) * SECTORSIZE);
                    store.setPartitions(new HWPartition[0]);
                    computeDiskStats(store, device);
                    result.add(store);
                } else if ("partition".equals(Udev.INSTANCE.udev_device_get_devtype(device)) && store != null) {
                    // `store` should still point to the HWDiskStore this
                    // partition is attached to. If not, it's an error, so skip.
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
                            this.mountsMap.getOrDefault(name, ""));
                    store.setPartitions(partArray);
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

    private void updateMountsMap() {
        this.mountsMap.clear();
        List<String> mounts = FileUtil.readFile("/proc/self/mounts");
        for (String mount : mounts) {
            String[] split = mount.split("\\s+");
            if (split.length < 2 || !split[0].startsWith("/dev/")) {
                continue;
            }
            this.mountsMap.put(split[0], split[1]);
        }
    }

    private void computeDiskStats(HWDiskStore store, Udev.UdevDevice disk) {
        LinuxBlockDevStats stats;
        stats = new LinuxBlockDevStats(store.getName(), disk);
        store.setTimeStamp(System.currentTimeMillis());

        // Reads and writes are converted in bytes
        store.setReads(stats.read_ops);
        store.setReadBytes(stats.read_512bytes * SECTORSIZE);
        store.setWrites(stats.write_ops);
        store.setWriteBytes(stats.write_512bytes * SECTORSIZE);
        store.setTransferTime(stats.active_ms);
    }
}
