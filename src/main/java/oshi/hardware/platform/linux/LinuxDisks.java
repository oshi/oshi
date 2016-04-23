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
 * Contributors:
 * enrico[dot]bianchi[at]gmail[dot]com
 *    com.github.dblock - initial API and implementation and/or initial documentation
 */
package oshi.hardware.platform.linux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.hardware.Disks;
import oshi.hardware.HWDiskStore;
import oshi.jna.platform.linux.Udev;
import oshi.json.NullAwareJsonObjectBuilder;

/**
 * Linux hard disk implementation.
 *
 * @author enrico[dot]bianchi[at]gmail[dot]com
 */
public class LinuxDisks implements Disks {

    private final int SECTORSIZE = 512;

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);
    private static final Logger LOG = LoggerFactory.getLogger(LinuxDisks.class);

    private void computeDiskStats(HWDiskStore store, Udev.UdevDevice disk) {
        Udev.BlockDevStats stats;
        String devstat;
        String[] splitstats;

        devstat = Udev.INSTANCE.udev_device_get_sysattr_value(disk, "stat");
        splitstats = devstat.split("\\s+");

        stats = new Udev.BlockDevStats(Long.parseLong(splitstats[1]),
                Long.parseLong(splitstats[2]),
                Long.parseLong(splitstats[3]),
                Long.parseLong(splitstats[4]),
                Long.parseLong(splitstats[5]),
                Long.parseLong(splitstats[6]),
                Long.parseLong(splitstats[7]),
                Long.parseLong(splitstats[8]),
                Long.parseLong(splitstats[9]),
                Long.parseLong(splitstats[10]),
                Long.parseLong(splitstats[11]));

        // Reads and writes are converted in bytes
        store.setReads((stats.read_512bytes * this.SECTORSIZE));
        store.setWrites(stats.write_512bytes * this.SECTORSIZE);
    }

    @Override
    public HWDiskStore[] getDisks() {
        HWDiskStore store;
        List<HWDiskStore> result;
        HashMap<String, Long> stats;

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
            try {
                oldEntry = entry;
                device = Udev.INSTANCE.udev_device_new_from_syspath(handle, Udev.INSTANCE.udev_list_entry_get_name(entry));
                if (Udev.INSTANCE.udev_device_get_devtype(device).equals("disk")) {
                    store = new HWDiskStore();
                    store.setName(Udev.INSTANCE.udev_device_get_devnode(device));
                    store.setModel(Udev.INSTANCE.udev_device_get_property_value(device, "ID_MODEL"));
                    store.setSerial(Udev.INSTANCE.udev_device_get_property_value(device, "ID_SERIAL_SHORT"));
                    store.setSize(Long.parseLong(Udev.INSTANCE.udev_device_get_sysattr_value(device, "size")) * SECTORSIZE);

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

    @Override
    public JsonObject toJSON() {

        JsonArrayBuilder array = jsonFactory.createArrayBuilder();

        for (HWDiskStore store : getDisks()) {
            array.add(store.toJSON());
        }
        return NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder()).add("disks", array).build();
    }
}
