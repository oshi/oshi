/*
 * Copyright (c) 2016 com.github.dblock.
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
import java.util.List;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.hardware.HWDiskStore;
import oshi.jna.platform.linux.Udev;
import oshi.json.NullAwareJsonObjectBuilder;
import oshi.json.OshiJsonObject;

/**
 * Linux hard disk implementation.
 *
 * @author enrico[dot]bianchi[at]gmail[dot]com
 */
public class LinuxDisks implements OshiJsonObject {

    private final int SECTORSIZE = 512;

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);
    private static final Logger LOG = LoggerFactory.getLogger(LinuxDisks.class);

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

        oldEntry = Udev.INSTANCE.udev_enumerate_get_list_entry(enumerate);
        while (true) {
            try {
                entry = Udev.INSTANCE.udev_list_entry_get_next(oldEntry);
                device = Udev.INSTANCE.udev_device_new_from_syspath(handle, Udev.INSTANCE.udev_list_entry_get_name(entry));
                String devtype = Udev.INSTANCE.udev_device_get_devtype(device);
                if (devtype.equals("disk")) {
                    store = new HWDiskStore();
                    store.setName(Udev.INSTANCE.udev_device_get_devnode(device));
                    store.setSerial(Udev.INSTANCE.udev_device_get_property_value(device, "ID_SERIAL_SHORT"));
                    
                    result.add(store);
                }
                oldEntry = entry;
            } catch (Exception ex) {
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
