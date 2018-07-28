/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
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
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.jna.platform.linux;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;

/**
 * Interface for talking with Udev.
 *
 * @author enrico[dot]bianchi[at]gmail[dot]com
 */
public interface Udev extends Library {

    Udev INSTANCE = Native.loadLibrary("udev", Udev.class);

    final class UdevHandle extends PointerType {

        public UdevHandle(Pointer address) {
            super(address);
        }

        public UdevHandle() {
            super();
        }
    }

    final class UdevDevice extends PointerType {

        public UdevDevice(Pointer address) {
            super(address);
        }

        public UdevDevice() {
            super();
        }
    }

    final class UdevEnumerate extends PointerType {

        public UdevEnumerate(Pointer address) {
            super(address);
        }

        public UdevEnumerate() {
            super();
        }
    }

    final class UdevListEntry extends PointerType {

        public UdevListEntry(Pointer address) {
            super(address);
        }

        public UdevListEntry() {
            super();
        }
    }

    Udev.UdevHandle udev_new();

    void udev_unref(Udev.UdevHandle udev);

    void udev_device_unref(Udev.UdevDevice udev_device);

    void udev_enumerate_unref(Udev.UdevEnumerate udev_enumerate);

    Udev.UdevEnumerate udev_enumerate_new(Udev.UdevHandle udev);

    Udev.UdevDevice udev_device_get_parent_with_subsystem_devtype(Udev.UdevDevice udev_device, String subsystem,
            String devtype);

    Udev.UdevDevice udev_device_new_from_syspath(Udev.UdevHandle udev, String syspath);

    Udev.UdevListEntry udev_list_entry_get_next(Udev.UdevListEntry list_entry);

    String udev_device_get_sysattr_value(Udev.UdevDevice udev_device, String sysattr);

    int udev_enumerate_add_match_subsystem(Udev.UdevEnumerate udev_enumerate, String subsystem);

    int udev_enumerate_scan_devices(Udev.UdevEnumerate udev_enumerate);

    Udev.UdevListEntry udev_enumerate_get_list_entry(Udev.UdevEnumerate udev_enumerate);

    String udev_list_entry_get_name(Udev.UdevListEntry list_entry);

    String udev_device_get_devtype(Udev.UdevDevice udev_device);

    String udev_device_get_devnode(Udev.UdevDevice udev_device);

    String udev_device_get_syspath(Udev.UdevDevice udev_device);

    String udev_device_get_property_value(Udev.UdevDevice udev_device, String key);

    String udev_device_get_sysname(UdevDevice udev_device);
}
