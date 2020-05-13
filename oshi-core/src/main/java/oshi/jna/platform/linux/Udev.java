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
package oshi.jna.platform.linux;

import com.sun.jna.Library; // NOSONAR squid:S1191
import com.sun.jna.Native;
import com.sun.jna.PointerType;

/**
 * Interface for libudev
 */
public interface Udev extends Library {

    Udev INSTANCE = Native.load("udev", Udev.class);

    /*
     * Decorated pointers to match documentation
     */
    /**
     * Base udev type for library context
     */
    class UdevHandle extends PointerType {
    }

    /**
     * Access to libudev generated lists
     */
    class UdevListEntry extends UdevHandle {
    }

    /**
     * Access to sysfs/kernel devices
     */
    class UdevDevice extends UdevHandle {
    }

    /**
     * Search sysfs for specific devices and provide a sorted list
     */
    class UdevEnumerate extends UdevHandle {
    }

    UdevHandle udev_new();

    void udev_unref(UdevHandle udev);

    void udev_device_unref(UdevDevice udev_device);

    void udev_enumerate_unref(UdevEnumerate udev_enumerate);

    UdevEnumerate udev_enumerate_new(UdevHandle udev);

    UdevDevice udev_device_get_parent_with_subsystem_devtype(UdevDevice udev_device, String subsystem, String devtype);

    UdevDevice udev_device_new_from_syspath(UdevHandle udev, String syspath);

    UdevListEntry udev_list_entry_get_next(UdevListEntry list_entry);

    String udev_device_get_sysattr_value(UdevDevice udev_device, String sysattr);

    int udev_enumerate_add_match_subsystem(UdevEnumerate udev_enumerate, String subsystem);

    int udev_enumerate_scan_devices(UdevEnumerate udev_enumerate);

    UdevListEntry udev_enumerate_get_list_entry(UdevEnumerate udev_enumerate);

    String udev_list_entry_get_name(UdevListEntry list_entry);

    String udev_device_get_devtype(UdevDevice udev_device);

    String udev_device_get_devnode(UdevDevice udev_device);

    String udev_device_get_syspath(UdevDevice udev_device);

    String udev_device_get_property_value(UdevDevice udev_device, String key);

    String udev_device_get_sysname(UdevDevice udev_device);
}
