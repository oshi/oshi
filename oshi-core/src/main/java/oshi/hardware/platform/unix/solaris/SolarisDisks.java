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
package oshi.hardware.platform.unix.solaris;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.hardware.HWDiskStore;
import oshi.hardware.common.AbstractDisks;
import oshi.jna.platform.linux.Udev;
import oshi.util.ParseUtil;

/**
 * Linux hard disk implementation.
 *
 * @author enrico[dot]bianchi[at]gmail[dot]com
 */
public class SolarisDisks extends AbstractDisks {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(SolarisDisks.class);

    private final int SECTORSIZE = 512;

    private void computeDiskStats(HWDiskStore store, Udev.UdevDevice disk) {
        SolarisBlockDevStats stats;
        stats = new SolarisBlockDevStats(store.getName(), disk);

        // Reads and writes are converted in bytes
        store.setReads(stats.read_512bytes * this.SECTORSIZE);
        store.setWrites(stats.write_512bytes * this.SECTORSIZE);
    }

    @Override
    public HWDiskStore[] getDisks() {
        HWDiskStore store;
        List<HWDiskStore> result;

        Udev.UdevHandle handle = null;
        Udev.UdevDevice device = null;
        Udev.UdevEnumerate enumerate = null;
        Udev.UdevListEntry entry, oldEntry;

        result = new ArrayList<>();

        handle = Udev.INSTANCE.udev_new();
        enumerate = Udev.INSTANCE.udev_enumerate_new(handle);
        Udev.INSTANCE.udev_enumerate_add_match_subsystem(enumerate, "block");
        Udev.INSTANCE.udev_enumerate_scan_devices(enumerate);

        entry = Udev.INSTANCE.udev_enumerate_get_list_entry(enumerate);
        while (true) {
            store = new HWDiskStore();
            try {
                oldEntry = entry;
                device = Udev.INSTANCE.udev_device_new_from_syspath(handle,
                        Udev.INSTANCE.udev_list_entry_get_name(entry));
                if (Udev.INSTANCE.udev_device_get_devtype(device).equals("disk")
                        && !Udev.INSTANCE.udev_device_get_devnode(device).startsWith("/dev/loop")
                        && !Udev.INSTANCE.udev_device_get_devnode(device).startsWith("/dev/ram")) {
                    store.setName(Udev.INSTANCE.udev_device_get_devnode(device));

                    // Avoid model and serial in virtual environments
                    store.setModel((Udev.INSTANCE.udev_device_get_property_value(device, "ID_MODEL") == null)
                            ? "Unknown" : Udev.INSTANCE.udev_device_get_property_value(device, "ID_MODEL"));
                    store.setSerial((Udev.INSTANCE.udev_device_get_property_value(device, "ID_SERIAL_SHORT") == null)
                            ? "Unknown"
                            : Udev.INSTANCE.udev_device_get_property_value(device, "ID_SERIAL_SHORT"));

                    store.setSize(ParseUtil.parseLongOrDefault(
                            Udev.INSTANCE.udev_device_get_sysattr_value(device, "size"), 0L) * SECTORSIZE);

                    this.computeDiskStats(store, device);
                    result.add(store);
                }
                entry = Udev.INSTANCE.udev_list_entry_get_next(oldEntry);
            } catch (Exception ex) {
                LOG.debug("Reached all disks. Exiting ");
                break;
            } finally {
                if (device instanceof Udev.UdevDevice) {
                    Udev.INSTANCE.udev_device_unref(device);
                }
            }
        }

        Udev.INSTANCE.udev_enumerate_unref(enumerate);
        Udev.INSTANCE.udev_unref(handle);

        return result.toArray(new HWDiskStore[result.size()]);
    }
}
